package pt.ulisboa.tecnico.cnv;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthChecker {
    private final ScheduledExecutorService scheduler;

    public HealthChecker(RequestManager requestManager) {
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void scheduleHealthChecks() {
        final Runnable healthChecker = this::performHealthChecks;
        scheduler.scheduleAtFixedRate(healthChecker, 0, 30, TimeUnit.SECONDS);
    }

    private void performHealthChecks() {
        Collection<ServerInstance> instances = SharedInstanceRegistry.getInstances();
        if (instances.isEmpty()) {
            System.out.println("No instances available for health check.");
            return;
        }
        
        instances.forEach(this::checkInstanceHealth);
    }

    private void checkInstanceHealth(ServerInstance instance) {
        HttpClient client = HttpClient.newHttpClient();
        String instanceUrl = "http://" + instance.getAddress() + ":8000/";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(instanceUrl))
            .GET()
            .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Health check for instance " + instance.getInstanceId() + " returned " + response.statusCode() + ": " + response.body());
        } catch (IOException | InterruptedException e) {
            System.out.println("Failed to perform health check for instance " + instance.getInstanceId() + ": " + e.getMessage());
        }
    }
}
