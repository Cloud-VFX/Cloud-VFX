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

public class RequestManager implements HttpHandler {
    private final InstanceSelector instanceSelector = new InstanceSelector();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ServerInstance instance = instanceSelector.getNextInstance();
        

        if (instance != null) {
            System.out.println("Forwarding request to instance: " + instance.toString());
            forwardRequest(instance, exchange);
        } else {
            exchange.sendResponseHeaders(503, -1); // Service Unavailable
            exchange.close();
        }
    }

    private void forwardRequest(ServerInstance instance, HttpExchange exchange) throws IOException {
        HttpClient client = HttpClient.newHttpClient();
        String instanceUrl = "http://" + instance.getAddress() + ":8000" + exchange.getRequestURI();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(instanceUrl));

        try {
            String method = exchange.getRequestMethod();
            HttpRequest request = null;
            switch (method) {
                case "POST":
                    String requestBody = new String(exchange.getRequestBody().readAllBytes());
                    request = requestBuilder.POST(BodyPublishers.ofString(requestBody)).build();
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
            e.printStackTrace();
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
