package pt.ulisboa.tecnico.cnv.loadbalancerautoscaler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.function.Function;

import pt.ulisboa.tecnico.cnv.metrics.AggregatedDAO;
import pt.ulisboa.tecnico.cnv.metrics.AggregatedMetrics;

public class RequestManager implements HttpHandler {
    private final InstanceSelector instanceSelector = new InstanceSelector();
    private static final Logger logger = Logger.getLogger("RequestManagerLogger");
    private final AggregatedDAO aggregatedDAO = new AggregatedDAO();
    private long lastTimeEstimatorUpdated;
    private Map<String, AggregatedMetrics> latestMetrics;
    private Map<String, Function<Double, Double>> estimators = new HashMap<>();

    private AtomicInteger amountOfLambdasInstances = new AtomicInteger(0);
    private static final int LAMBDA_THRESHOLD = 5;

    static {
        try {
            FileHandler fh = new FileHandler("RequestManager.log", true);
            logger.addHandler(fh);
            logger.setUseParentHandlers(false);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public RequestManager() {
        updateEstimators();
    }

    private void updateEstimators() {
        // Get the latest aggregated metrics
        this.latestMetrics = aggregatedDAO.readLatestAggregatedMetricsForAllMetricsTypes();

        if (this.latestMetrics.isEmpty()) {
            logger.warning("No metrics available to update estimators");
            this.lastTimeEstimatorUpdated = System.currentTimeMillis();
            return;
        }

        // Update the estimators functions with their latest values
        for (Map.Entry<String, AggregatedMetrics> entry : latestMetrics.entrySet()) {
            AggregatedMetrics metrics = entry.getValue();
            estimators.put(entry.getKey().toLowerCase(), metrics::estimateComplexity);
        }

        this.lastTimeEstimatorUpdated = System.currentTimeMillis();
    }

    private boolean checkIfLambdaInstancesAvailable() {
        return amountOfLambdasInstances.get() < LAMBDA_THRESHOLD;
    }

    private void forwardRequestToLambda(HttpExchange exchange, String payload) {
        // Update the amount of lambda instances
        int currentAmmountOfLambdasInstances = amountOfLambdasInstances.incrementAndGet();
        System.out.println("Forwarding request to lambda instance. Current amount of lambda instances: "
                + currentAmmountOfLambdasInstances);
        // TODO: Forward the request to a lambda instance
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("Received request: " + exchange.getRequestURI());

        // If it's been more than 1 minute since the last update, update the estimators
        if (System.currentTimeMillis() - this.lastTimeEstimatorUpdated > 60000) {
            updateEstimators();
        }

        // Read the payload once and pass it to the handler
        String payload = new String(exchange.getRequestBody().readAllBytes());
        RequestParamsHandler paramsHandler = new RequestParamsHandler(exchange, payload);
        Map<String, Object> requestDetails = paramsHandler.getRequestDetails();

        // Log the request details obtained from the handler
        logger.info("Request Details: " + requestDetails.toString());

        // Calculate the complexity of the request
        String requestType = (String) requestDetails.get("requestType");
        int inputSize = (int) requestDetails.get("imageSize");

        // Check if requestType is blur or enhance
        if (requestType.equalsIgnoreCase("blur") || requestType.equalsIgnoreCase("enhance")) {
            if (checkIfLambdaInstancesAvailable()) {
                forwardRequestToLambda(exchange, payload);
                // TODO: How can we forward the response from the lambda to the client?
                return;
            }
        }

        Function<Double, Double> estimator = estimators.get(requestType.toLowerCase());
        double complexity = estimator.apply((double) inputSize);

        // TODO: Key Points
        // -> Fetch the current usage of lambda instances: DONE
        // If requestType is BLUR, or ENHANCE, forward the request with these
        // conditions:
        // -> If the current usage is below a certain threshold, forward the request to
        // a lambda instance: Almost done
        // -> If the current usage is above a certain threshold, forward the request to
        // an EC2 instance
        // If requestType is RAYTRACER, forward the request to an EC2 instance
        // Apply algorithm to select the best instance to forward the request
        // Forward the request to the selected instance

        ServerInstance instance = instanceSelector.getInstanceWithLessComplexity();

        if (instance == null) {
            exchange.sendResponseHeaders(503, -1); // Service Unavailable
            exchange.close();
            return;
        }

        // Add the complexity to the instance
        SharedInstanceRegistry.updateInstanceComplexity(instance.getInstanceId(), complexity,
                SharedInstanceRegistry.UpdateComplexityType.ADD);

        // Forward the request
        forwardRequest(instance, exchange, payload, complexity);

        // Remove the complexity from the instance
        SharedInstanceRegistry.updateInstanceComplexity(instance.getInstanceId(), complexity,
                SharedInstanceRegistry.UpdateComplexityType.REMOVE);
    }

    private void forwardRequest(ServerInstance instance, HttpExchange exchange, String payload, double complexity)
            throws IOException {
        HttpClient client = HttpClient.newHttpClient();
        String instanceUrl = "http://" + instance.getAddress() + ":8000" + exchange.getRequestURI();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(instanceUrl));

        try {
            String method = exchange.getRequestMethod();
            HttpRequest request = null;
            switch (method) {
                case "POST":
                    request = requestBuilder.POST(BodyPublishers.ofString(payload)).build();
                    break;
                case "GET":
                    request = requestBuilder.GET().build();
                    break;
                default:
                    throw new UnsupportedOperationException("HTTP method not supported");
            }
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            sendResponse(exchange, response.body(), 200);
        } catch (Exception e) {
            logger.severe("Error processing request: " + e.getMessage());
            sendResponse(exchange, "Internal server error", 500);
        } finally {
            exchange.close();
        }
    }

    private void sendResponse(HttpExchange exchange, String body, int statusCode) throws IOException {
        byte[] responseBytes = body.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.getResponseBody().close();
    }
}
