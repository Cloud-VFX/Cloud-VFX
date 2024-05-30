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
import pt.ulisboa.tecnico.cnv.metrics.AggregatedDAO;
import pt.ulisboa.tecnico.cnv.metrics.AggregatedMetricsClient;
import pt.ulisboa.tecnico.cnv.metrics.DynamoDBClient;
import pt.ulisboa.tecnico.cnv.metrics.MetricsAggregator;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

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

    public static void main(String[] args) throws Exception {
        System.out.println("Starting WebServer...");
        createMetricsFile();

        // Setup DynamoDB table
        DynamoDBClient.setupTable();
        AggregatedMetricsClient.setupTable();

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        server.createContext("/", new RootHandler());
        server.createContext("/raytracer", new MetricsMiddleware(new RaytracerHandler()));
        server.createContext("/blurimage", new MetricsMiddleware(new BlurImageHandler()));
        server.createContext("/enhanceimage", new MetricsMiddleware(new EnhanceImageHandler()));

        // TODO: temporary endpoint to trigger aggregation
        server.createContext("/aggregate", (exchange -> {
            handleAggregation(new MetricsAggregator());
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        }));

        /**
         * Endpoint to query the latest aggregated metrics for a specific metric type
         * Usage: /query_aggregated?metricType=METRIC_TYPE
         */
        server.createContext("/query_aggregated", (exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String[] params = query.split("=");
            if (params.length != 2) {
                exchange.sendResponseHeaders(400, 0);
                exchange.close();
                return;
            }
            String metricType = params[1];
            AggregatedDAO dao = new AggregatedDAO();
            String response = dao.readLatestAggregatedMetrics(metricType).toString();
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();

        }));

        server.start();

        System.out.println("WebServer started on port 8000");

        // Scheduled periodic aggregation of metrics
        // TODO: This need to be moved to the LoadBalancer
        // ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        // MetricsAggregator aggregator = new MetricsAggregator();
        // scheduler.scheduleAtFixedRate(() -> handleAggregation(aggregator), 10, 30,
        // TimeUnit.SECONDS);

    }

    private static void handleAggregation(MetricsAggregator aggregator) {
        try {
            aggregator.handleRequest(null, new Context() {
                @Override
                public String getAwsRequestId() {
                    return "localRequestId";
                }

                @Override
                public String getLogGroupName() {
                    return "localLogGroup";
                }

                @Override
                public String getLogStreamName() {
                    return "localLogStream";
                }

                @Override
                public String getFunctionName() {
                    return "localFunction";
                }

                @Override
                public String getFunctionVersion() {
                    return "1.0";
                }

                @Override
                public String getInvokedFunctionArn() {
                    return "localArn";
                }

                @Override
                public CognitoIdentity getIdentity() {
                    return null;
                }

                @Override
                public ClientContext getClientContext() {
                    return null;
                }

                @Override
                public int getRemainingTimeInMillis() {
                    return 10000;
                }

                @Override
                public int getMemoryLimitInMB() {
                    return 512;
                }

                @Override
                public LambdaLogger getLogger() {
                    return new LambdaLogger() {
                        @Override
                        public void log(String message) {
                            System.out.println(message);
                        }

                        @Override
                        public void log(byte[] message) {
                            System.out.println(new String(message));
                        }
                    };
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
