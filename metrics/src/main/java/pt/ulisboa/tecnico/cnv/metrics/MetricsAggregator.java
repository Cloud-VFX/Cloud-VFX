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
        List<RequestMetrics> allMetrics = new ArrayList<>();

        // Group metrics by type
        for (Map<String, AttributeValue> item : result.getItems()) {
            try {
                Item dynamoItem = ItemUtils.toItem(item);
                RequestMetrics metrics = RequestMetrics.fromItem(dynamoItem);
                metricsByType.computeIfAbsent(metrics.getMetricType().name(), k -> new ArrayList<>()).add(metrics);
                allMetrics.add(metrics);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid item: " + item);
                e.printStackTrace();
            }
        }

        // Calculate global min and max for inputs and outputs
        double globalMinInput = Double.POSITIVE_INFINITY;
        double globalMaxInput = Double.NEGATIVE_INFINITY;
        double globalMinOutput = Double.POSITIVE_INFINITY;
        double globalMaxOutput = Double.NEGATIVE_INFINITY;

        for (RequestMetrics metrics : allMetrics) {
            double input_normalizing = metrics.getMetricType() == RequestMetrics.MetricType.RAYTRACER
                    ? metrics.getSInputSize() + (metrics.getTextureMap() ? 1 : 0)
                    : metrics.getImageSize();
            double output_normalizing = 0.7 * metrics.getNumberOfInstructions() + 0.2 * metrics.getNumberOfBlocks()
                    + 0.1 * metrics.getProcessingTime();

            if (input_normalizing < globalMinInput)
                globalMinInput = input_normalizing;
            if (input_normalizing > globalMaxInput)
                globalMaxInput = input_normalizing;
            if (output_normalizing < globalMinOutput)
                globalMinOutput = output_normalizing;
            if (output_normalizing > globalMaxOutput)
                globalMaxOutput = output_normalizing;
        }

        // Aggregate and train model for each type
        for (Map.Entry<String, List<RequestMetrics>> entry : metricsByType.entrySet()) {
            System.out.println("Aggregating and training model for " + entry.getKey());
            String metricType = entry.getKey();
            List<RequestMetrics> metrics = entry.getValue();
            if (metrics.isEmpty())
                continue;

            // Train model
            ModelCoefficients coefficients = trainModel(metricType, metrics, globalMinInput, globalMaxInput,
                    globalMinOutput, globalMaxOutput);

            // Ensure coefficients are finite
            if (!Double.isFinite(coefficients.alpha) || !Double.isFinite(coefficients.beta)) {
                System.err.println("Non-finite coefficients: " + coefficients);
                continue;
            }

            // Create AggregatedMetrics object
            AggregatedMetrics aggregatedMetrics = new AggregatedMetrics(
                    coefficients.alpha,
                    coefficients.beta,
                    (long) globalMaxInput,
                    (long) globalMinInput,
                    (long) globalMaxOutput,
                    (long) globalMinOutput,
                    coefficients.function,
                    metricType,
                    System.currentTimeMillis());

            // Store aggregated metrics
            Item aggregatedItem = aggregatedMetrics.toItem();

            aggregatedMetricsTable.putItem(aggregatedItem);
        }

        return "Aggregation and model training complete";
    }

    private ModelCoefficients trainModel(String metricType, List<RequestMetrics> metrics, double globalMinInput,
            double globalMaxInput, double globalMinOutput, double globalMaxOutput) {
        System.out.println("Training model for " + metricType);
        // PRint the first 5 metrics
        for (int i = 0; i < Math.min(5, metrics.size()); i++) {
            System.out.println(metrics.get(i).toString());
        }

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
                    double input = metric.getSInputSize() + (metric.getTextureMap() ? 1 : 0);
                    double output = 0.7 * metric.getNumberOfInstructions() + 0.2 * metric.getNumberOfBlocks()
                            + 0.1 * metric.getProcessingTime();
                    inputs.add(input);
                    outputs.add(output);
                }
                break;
        }

        // Normalize inputs globally to [0, 1]
        for (int i = 0; i < inputs.size(); i++) {
            inputs.set(i, (inputs.get(i) - globalMinInput) / (globalMaxInput - globalMinInput));
        }

        // Normalize outputs globally to [0, 100]
        for (int i = 0; i < outputs.size(); i++) {
            outputs.set(i, 100 * (outputs.get(i) - globalMinOutput) / (globalMaxOutput - globalMinOutput));
        }

        // Perform linear regression to find the best fit line
        double lambda = 0.1; // Regularization parameter
        double[] coefficients = linearRegressionWithL2(inputs, outputs, lambda);
        double alpha = coefficients[0];
        double beta = coefficients[1];

        String function = "complexity = " + alpha + " * input + " + beta;

        return new ModelCoefficients(alpha, beta, function);
    }

    private double[] linearRegressionWithL2(List<Double> x, List<Double> y, double lambda) {
        System.out.println("Performing linear regression with L2 regularization with " + x.size() + " data points");
        System.out.println("X: " + x);
        System.out.println("Y: " + y);
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
        double denominator = sumX2 - (sumX * sumX / n) + lambda;

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
