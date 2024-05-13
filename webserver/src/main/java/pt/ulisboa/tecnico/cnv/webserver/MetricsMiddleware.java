package pt.ulisboa.tecnico.cnv.webserver;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

public class MetricsMiddleware implements HttpHandler {
    private HttpHandler handler;

    public MetricsMiddleware(HttpHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            MetricsContext.start();
            handler.handle(he);
        } finally {
            MetricsContext.end();
            RequestMetrics metrics = MetricsContext.getMetrics();
            System.out.println("Metrics: " + metrics.toJSON());
        }
    }
}
