package pt.ulisboa.tecnico.cnv.webserver;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

public class MetricsMiddleware implements HttpHandler {
    private HttpHandler handler;

    public static final String METRICS_FILE = "metrics.log";

    public MetricsMiddleware(HttpHandler handler) {
        this.handler = handler;
    }

    private void appendMetricsToFile(RequestMetrics metrics) throws IOException {
        // Open the metrics file
        java.io.FileWriter fileWriter = new java.io.FileWriter(METRICS_FILE, true);
        java.io.BufferedWriter bufferedWriter = new java.io.BufferedWriter(fileWriter);

        // Write the metrics to the file
        bufferedWriter.write(metrics.toString());
        bufferedWriter.newLine();

        // Close the file
        bufferedWriter.close();
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            MetricsContext.start();
            handler.handle(he);
        } finally {
            MetricsContext.end();
            RequestMetrics metrics = MetricsContext.getMetrics();

            // Append the metrics to the metrics file
            appendMetricsToFile(metrics);

            System.out.println("Metrics: " + metrics.toJSON());
        }
    }
}
