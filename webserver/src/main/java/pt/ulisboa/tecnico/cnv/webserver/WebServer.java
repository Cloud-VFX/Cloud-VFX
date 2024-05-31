package pt.ulisboa.tecnico.cnv.webserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler;
import pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler;
import pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler;
import pt.ulisboa.tecnico.cnv.metrics.AggregatedMetricsClient;
import pt.ulisboa.tecnico.cnv.metrics.DynamoDBClient;

public class WebServer {

    public static final String METRICS_FILE = "metrics.log";

    private static void createMetricsFile() throws IOException {
        java.io.File file = new java.io.File(METRICS_FILE);
        // Check if the file already exists
        if (file.exists()) {
            System.out.println("Metrics file already exists.");
            return;
        }
        file.createNewFile();
    }

    private static void startCpuUsagePublisher() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        CpuUsagePublisher cpuUsagePublisher = new CpuUsagePublisher();
        scheduler.scheduleAtFixedRate(() -> cpuUsagePublisher.publishCpuUsage(), 0, 10, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Starting WebServer...");
        createMetricsFile();
        startCpuUsagePublisher();

        // Setup DynamoDB table
        DynamoDBClient.setupTable();
        AggregatedMetricsClient.setupTable();

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        server.createContext("/", new MetricsMiddleware(new RootHandler()));
        server.createContext("/raytracer", new MetricsMiddleware(new RaytracerHandler()));
        server.createContext("/blurimage", new MetricsMiddleware(new BlurImageHandler()));
        server.createContext("/enhanceimage", new MetricsMiddleware(new EnhanceImageHandler()));

        server.start();

        System.out.println("WebServer started on port 8000");

    }

}
