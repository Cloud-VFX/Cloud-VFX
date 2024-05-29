package pt.ulisboa.tecnico.cnv.metrics;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.json.JSONObject;

import java.util.*;

public class MetricsAggregator implements RequestHandler<Object, String> {

    private static final Table aggregatedMetricsTable = AggregatedMetricsClient.dynamoDB
            .getTable(AggregatedMetricsClient.TABLE_NAME);

    @Override
    public String handleRequest(Object input, Context context) {
        // Scan the RequestMetrics table
        ScanRequest scanRequest = new ScanRequest().withTableName(DynamoDBClient.TABLE_NAME);
        ScanResult result = DynamoDBClient.client.scan(scanRequest);

        Map<String, List<RequestMetrics>> metricsByType = new HashMap<>();

        // Group metrics by type
        for (Map<String, AttributeValue> item : result.getItems()) {
            try {
                Item dynamoItem = ItemUtils.toItem(item);
                RequestMetrics metrics = RequestMetrics.fromItem(dynamoItem);
                metricsByType.computeIfAbsent(metrics.getMetricType().name(), k -> new ArrayList<>()).add(metrics);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid item: " + item);
                e.printStackTrace();
            }
        }

        // Aggregate and train model for each type
        for (Map.Entry<String, List<RequestMetrics>> entry : metricsByType.entrySet()) {
            String metricType = entry.getKey();
            List<RequestMetrics> metrics = entry.getValue();
            if (metrics.isEmpty())
                continue;

            // Train model
            ModelCoefficients coefficients = trainModel(metricType, metrics);

            // Ensure coefficients are finite
            if (!Double.isFinite(coefficients.alpha) || !Double.isFinite(coefficients.beta)) {
                System.err.println("Non-finite coefficients: " + coefficients);
                continue;
            }

            // Store aggregated result
            long timestamp = System.currentTimeMillis();
            JSONObject coeffsJson = new JSONObject();
            coeffsJson.put("Alpha", coefficients.alpha);
            coeffsJson.put("Beta", coefficients.beta);

            Item aggregatedItem = new Item()
                    .withPrimaryKey("MetricType", metricType, "Timestamp", String.valueOf(timestamp))
                    .withString("Coefficients", coeffsJson.toString());

            aggregatedMetricsTable.putItem(aggregatedItem);
        }

        return "Aggregation and model training complete";
    }

    private ModelCoefficients trainModel(String metricType, List<RequestMetrics> metrics) {
        // Extract inputs and outputs for linear regression
        List<Double> inputs = new ArrayList<>();
        List<Double> outputs = new ArrayList<>();

        switch (metricType) {
            case "BLUR":
            case "ENHANCE":
                for (RequestMetrics metric : metrics) {
                    double input = metric.getImageSize();
                    double output = 0.7 * metric.getNumberOfInstructions() + 0.2 * metric.getNumberOfBlocks()
                            + 0.1 * metric.getProcessingTime();
                    inputs.add(input);
                    outputs.add(output);
                }
                break;
            case "RAYTRACER":
                // Handle multidimensional input for raytracer
                // This is a placeholder for more complex multidimensional regression logic
                for (RequestMetrics metric : metrics) {
                    double input = metric.getSInputSize() + metric.getWInputSize() + (metric.getTextureMap() ? 1 : 0);
                    double output = 0.7 * metric.getNumberOfInstructions() + 0.2 * metric.getNumberOfBlocks()
                            + 0.1 * metric.getProcessingTime();
                    inputs.add(input);
                    outputs.add(output);
                }
                break;
        }

        // Perform linear regression to find the best fit line
        double[] coefficients = linearRegression(inputs, outputs);
        double alpha = coefficients[0];
        double beta = coefficients[1];

        String function = "complexity = " + alpha + " * input + " + beta;

        return new ModelCoefficients(alpha, beta, function);
    }

    private double[] linearRegression(List<Double> x, List<Double> y) {
        int n = x.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += x.get(i);
            sumY += y.get(i);
            sumXY += x.get(i) * y.get(i);
            sumX2 += x.get(i) * x.get(i);
        }

        double xMean = sumX / n;
        double yMean = sumY / n;

        double numerator = sumXY - (sumX * sumY / n);
        double denominator = sumX2 - (sumX * sumX / n);

        double alpha = numerator / denominator;
        double beta = yMean - alpha * xMean;

        return new double[] { alpha, beta };
    }

    static class ModelCoefficients {
        double alpha;
        double beta;
        String function;

        ModelCoefficients(double alpha, double beta, String function) {
            this.alpha = alpha;
            this.beta = beta;
            this.function = function;
        }
    }
}
