package pt.ulisboa.tecnico.cnv;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class LoadBalancer {
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public LoadBalancer() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RequestHandler());
        server.setExecutor(executorService);
        server.start();
        System.out.println("LoadBalancer started on port " + port);
        scheduleHealthChecks();
    }

    private class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            executorService.submit(() -> {
                try {
                    ServerInstance instance = getNextInstance();
                    if (instance != null) {
                        String responseBody = forwardRequest(instance, exchange);
                        byte[] responseBytes = responseBody.getBytes();
                        exchange.sendResponseHeaders(200, responseBytes.length);
                        exchange.getResponseBody().write(responseBytes);
                    } else {
                        exchange.sendResponseHeaders(503, -1); // Service Unavailable
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        exchange.sendResponseHeaders(500, -1); // Internal Server Error
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } finally {
                    exchange.close();
                }
            });
        }
    }

    private synchronized ServerInstance getNextInstance() {
        Collection<ServerInstance> instances = SharedInstanceRegistry.getInstances();
        if (instances.isEmpty()) {
            return null;
        }
        int index = currentIndex.getAndIncrement() % instances.size();
        return (ServerInstance) instances.toArray()[index];
    }

    private String forwardRequest(ServerInstance instance, HttpExchange exchange) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String instanceUrl = "http://" + instance.getAddress() + ":8000" + exchange.getRequestURI();  // Include the port number here
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(instanceUrl));

        switch (exchange.getRequestMethod()) {
            case "POST":
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                requestBuilder.POST(BodyPublishers.ofString(requestBody));
                break;
            case "GET":
                requestBuilder.GET();
                break;
            default:
                throw new UnsupportedOperationException("Method not supported");
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return response.body();
    }
    
    private void scheduleHealthChecks() {
        final Runnable healthChecker = this::performHealthChecks;
        scheduler.scheduleAtFixedRate(healthChecker, 0, 30, TimeUnit.SECONDS);
    }

    private void performHealthChecks() {
        Collection<ServerInstance> instances = SharedInstanceRegistry.getInstances();
        instances.forEach(this::checkInstanceHealth);
    }

    private void checkInstanceHealth(ServerInstance instance) {
        HttpClient client = HttpClient.newHttpClient();
        String instanceUrl = "http://" + instance.getAddress() + ":8000/";  // Include the port number here
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(instanceUrl))
            .GET()
            .build();

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            System.out.println("Health check for instance " + instance.getInstanceId() + " returned " + response.statusCode() + ": " + response.body());
        } catch (Exception e) {
            System.out.println("Failed to perform health check for instance " + instance.getInstanceId() + ": " + e.getMessage());
        }
    }
}
