package pt.ulisboa.tecnico.cnv.loadbalancerautoscaler;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
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

import org.json.JSONObject;

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

    private final AWSLambda awsLambda;
    private AtomicInteger amountOfLambdasInstances = new AtomicInteger(0);
    private static final int LAMBDA_THRESHOLD = 5;
    private static final String ENHANCE_LAMBDA_FUNC_NAME = "EnhanceImageHandler";
    private static final String BLUR_LAMBDA_FUNC_NAME = "BlurImageHandler";
    private static final String RAYTRACER_LAMBDA_FUNC_NAME = "RaytracerHandler";

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
        this.awsLambda = AWSLambdaClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain()).build();
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

    private void forwardRequestToLambda(HttpExchange exchange, String requestType, String payload) throws IOException {
        // Update the amount of lambda instances
        int currentAmmountOfLambdasInstances = amountOfLambdasInstances.incrementAndGet();
        // System.out.println("Current amount of lambda instances:" + currentAmmountOfLambdasInstances);
        // Create the payload for the lambda
        // Need to have a Map<String, Object> with the following
        // - fileFormat: String (jpeg)
        // - body: String (base64 encoded image)
        Map<String, Object> lambdaPayload = new HashMap<>();
        lambdaPayload.put("fileFormat", "jpeg");
        // Need to remove "data:image/jpeg;base64," from the payload
        String base64Image = payload.split(",")[1];
        lambdaPayload.put("body", base64Image);

        // Create the request to the lambda
        JSONObject jsonPayload = new JSONObject(lambdaPayload);
        String jsonPayloadString = jsonPayload.toString();
        String functionName = requestType.equalsIgnoreCase("blur") ? BLUR_LAMBDA_FUNC_NAME
                : requestType.equalsIgnoreCase("enhance") ? ENHANCE_LAMBDA_FUNC_NAME : RAYTRACER_LAMBDA_FUNC_NAME;

        InvokeRequest invokeRequest = new InvokeRequest()
                .withFunctionName(functionName)
                .withPayload(jsonPayloadString);

        // Invoke the lambda
        try {
            InvokeResult invokeResult = awsLambda.invoke(invokeRequest);
            // The string comes wrapped in quotes, so we need to remove them
            String lambdaResponse = new String(invokeResult.getPayload().array());
            lambdaResponse = lambdaResponse.substring(1, lambdaResponse.length() - 1);

            sendResponse(exchange, lambdaResponse, 200);
        } catch (Exception e) {
            logger.severe("Error invoking lambda: " + e.getMessage());
            sendResponse(exchange, "Internal server error", 500);
        } finally {
            // Update the amount of lambda instances
            amountOfLambdasInstances.decrementAndGet();
            exchange.close();
        }

    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // System.out.println("Received request: " + exchange.getRequestURI());

        // Check if request path is root
        if (exchange.getRequestURI().getPath().equals("/")) {
            sendResponse(exchange, "Hello, World!", 200);
            return;
        }

        // If it's been more than 1 minute since the last update, update the estimators
        if (System.currentTimeMillis() - this.lastTimeEstimatorUpdated > 60000) {
            updateEstimators();
        }

        // Read the payload once and pass it to the handler
        String payload = new String(exchange.getRequestBody().readAllBytes());
        RequestParamsHandler paramsHandler = new RequestParamsHandler(exchange, payload);
        Map<String, Object> requestDetails = paramsHandler.getRequestDetails();

        // Log the request details obtained from the handler
        // System.out.println("Request Details: " + requestDetails.toString());

        // Calculate the complexity of the request
        String requestType = (String) requestDetails.get("requestType");
        if (requestType == "unknown") {
            sendResponse(exchange, "Invalid request type", 400);
            return;
        }

        int inputSize = (int) requestDetails.get("imageSize");

        // Check if requestType is blur or enhance
        if (checkIfLambdaInstancesAvailable()
                && (requestType.equalsIgnoreCase("blur")
                        || requestType.equalsIgnoreCase("enhance"))) {
            // System.out.println("Forwarding Request to lambda:\n" + requestDetails.toString());
            System.out.println("Forwarding Request to lambda:\n");

            forwardRequestToLambda(exchange, requestType, payload);
            return;
        }

        Function<Double, Double> estimator = estimators.get(requestType.toLowerCase());
        double complexity = estimator.apply((double) inputSize);

        ServerInstance instance = instanceSelector.getInstanceWithLessComplexity();

        if (instance == null) {
            exchange.sendResponseHeaders(503, -1); // Service Unavailable
            exchange.close();
            return;
        }

        // System.out.println("Forwarding Request:\n"+ requestDetails.toString() + "\nTo instance:\n" + instance.toString());
        System.out.println("Forwarding Request to instance:" + instance.getInstanceId());
        // Add the complexity to the instance
        SharedInstanceRegistry.updateInstanceComplexity(instance.getInstanceId(), complexity,
                SharedInstanceRegistry.UpdateComplexityType.ADD);
        
        instance.increaseNumRunningRequests();

        // Forward the request
        forwardRequest(instance, exchange, payload, complexity);

        // Remove the complexity from the instance
        SharedInstanceRegistry.updateInstanceComplexity(instance.getInstanceId(), complexity,
                SharedInstanceRegistry.UpdateComplexityType.REMOVE);

        instance.decreaseNumRunningRequests();
    
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
