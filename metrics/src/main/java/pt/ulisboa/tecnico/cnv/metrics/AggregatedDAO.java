package pt.ulisboa.tecnico.cnv.metrics;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;

public class AggregatedDAO {

    private final DynamoDB dynamoDB;
    private final Table table;

    public AggregatedDAO() {
        this.dynamoDB = AggregatedMetricsClient.dynamoDB;
        this.table = dynamoDB.getTable(AggregatedMetricsClient.TABLE_NAME);
    }

    /**
     * Create a new aggregated metrics
     */
    public void createAggregatedMetrics(AggregatedMetrics metrics) {
        table.putItem(metrics.toItem());
    }

    /*
     * Read the aggregated metrics with the largest timestamp
     * This method is used to get the most recent aggregated metrics
     */
    public AggregatedMetrics readLatestAggregatedMetrics(String metricType) {
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":metricType", new AttributeValue().withS(metricType));

        QueryRequest queryRequest = new QueryRequest()
                .withTableName(AggregatedMetricsClient.TABLE_NAME)
                .withKeyConditionExpression("MetricType = :metricType")
                .withExpressionAttributeValues(expressionAttributeValues)
                .withScanIndexForward(false) // false to sort in descending order by sort key
                .withLimit(1); // Get only the latest item

        QueryResult queryResult = AggregatedMetricsClient.client.query(queryRequest);

        if (!queryResult.getItems().isEmpty()) {
            Item item = ItemUtils.toItem(queryResult.getItems().get(0));
            return AggregatedMetrics.fromItem(item);
        } else {
            return null; // No items found
        }
    }

    public Map<String, AggregatedMetrics> readLatestAggregatedMetricsForAllMetricsTypes() {
        Map<String, AggregatedMetrics> metricsMap = new HashMap<>();
        // Metrics Type
        String[] metricsType = { "RAYTRACER", "BLUR", "ENHANCE" };

        for (String metricType : metricsType) {
            AggregatedMetrics metrics = readLatestAggregatedMetrics(metricType);
            if (metrics != null)
                metricsMap.put(metricType, metrics);
        }

        return metricsMap;
    }
}
