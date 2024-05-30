package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class RequestManager implements HttpHandler {
    private final InstanceSelector instanceSelector = new InstanceSelector();
    private static final Logger logger = Logger.getLogger("RequestManagerLogger");
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

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ServerInstance instance = instanceSelector.getNextInstance();
        if (instance == null) {
            exchange.sendResponseHeaders(503, -1); // Service Unavailable
            exchange.close();
            return;
        }

        // Read the payload once and pass it to the handler
        String payload = new String(exchange.getRequestBody().readAllBytes());
        RequestParamsHandler paramsHandler = new RequestParamsHandler(exchange, payload);
        Map<String, Object> requestDetails = paramsHandler.getRequestDetails();

        // Log the request details obtained from the handler
        logger.info("Request Details: " + requestDetails.toString());

        // Forward the request
        forwardRequest(instance, exchange, payload);
    }

    private void forwardRequest(ServerInstance instance, HttpExchange exchange, String payload) throws IOException {
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
