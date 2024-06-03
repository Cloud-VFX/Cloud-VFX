package pt.ulisboa.tecnico.cnv.metrics;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Data Access Object for the metrics
 */
public class MetricsDAO {

    private final DynamoDB dynamoDB;
    private final Table table;

    public MetricsDAO() {
        this.dynamoDB = DynamoDBClient.dynamoDB;
        this.table = dynamoDB.getTable(DynamoDBClient.TABLE_NAME);
    }

    /**
     * Create a new metrics
     * 
     * @param metrics
     */
    public void createMetrics(RequestMetrics metrics) {
        table.putItem(metrics.toItem());
    }

    /**
     * Read the metrics of a request
     * 
     * @param requestId
     * @return
     */
    public RequestMetrics readMetrics(String requestId) {
        Item item = table.getItem("RequestId", requestId);
        if (item != null) {
            return RequestMetrics.fromItem(item);
        }
        return null;
    }

    /**
     * Update the metrics of a request
     * 
     * @param requestId
     * @param updatedMetrics
     */
    public void updateMetrics(String requestId, RequestMetrics updatedMetrics) {
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(":s", updatedMetrics.getSInputSize());
        valueMap.put(":w", updatedMetrics.getWInputSize());
        valueMap.put(":a", updatedMetrics.getAntiAlias());
        valueMap.put(":t", updatedMetrics.getTextureMap());
        valueMap.put(":st", updatedMetrics.getStartTime());
        valueMap.put(":et", updatedMetrics.getEndTime());
        valueMap.put(":pt", updatedMetrics.getProcessingTime());
        valueMap.put(":ni", updatedMetrics.getNumberOfInstructions());
        valueMap.put(":nb", updatedMetrics.getNumberOfBlocks());
        valueMap.put(":is", updatedMetrics.getImageSize());

        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey("RequestId", requestId)
                .withUpdateExpression(
                        "set sInputSize = :s, wInputSize = :w, antiAlias = :a, textureMap = :t, startTime = :st, endTime = :et, processingTime = :pt, numberOfInstructions = :ni, numberOfBlocks = :nb, imageSize = :is")
                .withValueMap(valueMap)
                .withReturnValues(ReturnValue.UPDATED_NEW);

        UpdateItemOutcome outcome = table.updateItem(updateItemSpec);

        // System.out.println("UpdateItem succeeded:\n" + outcome.getItem().toJSONPretty());
    }

    /**
     * Delete the metrics of a request
     * 
     * @param requestId
     */
    public void deleteMetrics(String requestId) {
        DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
                .withPrimaryKey("RequestId", requestId);
        table.deleteItem(deleteItemSpec);
    }

    /**
     * Scan all the metrics
     * 
     * @return
     */
    public ArrayList<RequestMetrics> scanMetrics() {
        ArrayList<RequestMetrics> metrics = new ArrayList<>();
        ItemCollection<ScanOutcome> items = table.scan();
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            metrics.add(RequestMetrics.fromItem(item));
        }
        return metrics;
    }
}
