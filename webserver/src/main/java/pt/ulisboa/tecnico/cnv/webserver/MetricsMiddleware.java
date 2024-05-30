package pt.ulisboa.tecnico.cnv.webserver;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

import pt.ulisboa.tecnico.cnv.metrics.MetricsDAO;
import pt.ulisboa.tecnico.cnv.metrics.RequestMetrics;

public class MetricsMiddleware implements HttpHandler {
    private HttpHandler handler;
    private MetricsDAO metricsDAO;

    public static final String METRICS_FILE = "metrics.log";

    public MetricsMiddleware(HttpHandler handler) {
        this.handler = handler;
        this.metricsDAO = new MetricsDAO();
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
            MetricsContext.end();
            RequestMetrics metrics = MetricsContext.getMetrics();
            // Append the metrics to the metrics file
            appendMetricsToFile(metrics);
            // Save the metrics to the database
            metricsDAO.createMetrics(metrics);
            System.out.println("Metrics: " + metrics.toJSON());
            System.out.println("Metrics saved to file and database.");
        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
            // Don't save metrics if an error occurred
            return;
        }
    }
}
