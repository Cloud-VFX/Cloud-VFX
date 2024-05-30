package pt.ulisboa.tecnico.cnv.metrics;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;

public class AggregatedMetricsClient {

    public static final String TABLE_NAME = "AggregatedMetrics";
    public static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
            .withRegion("eu-north-1")
            .build();
    public static final DynamoDB dynamoDB = new DynamoDB(client);

    public static void setupTable() {
        try {
            CreateTableRequest createTableRequest = new CreateTableRequest()
                    .withTableName(TABLE_NAME)
                    .withKeySchema(
                            new KeySchemaElement("MetricType", KeyType.HASH),
                            new KeySchemaElement("Timestamp", KeyType.RANGE))
                    .withAttributeDefinitions(
                            new AttributeDefinition("MetricType", ScalarAttributeType.S),
                            new AttributeDefinition("Timestamp", ScalarAttributeType.S))
                    .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L));

            Table table = dynamoDB.createTable(createTableRequest);
            table.waitForActive();
            System.out.println("Aggregated Metrics Table created successfully.");
        } catch (ResourceInUseException e) {
            System.out.println("Aggregated Metrics Table already exists.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
