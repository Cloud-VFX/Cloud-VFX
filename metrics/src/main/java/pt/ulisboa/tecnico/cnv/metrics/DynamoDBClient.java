package pt.ulisboa.tecnico.cnv.metrics;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;

public class DynamoDBClient {
        public static final String TABLE_NAME = "RequestMetrics";
        public static final String GSI_NAME = "MetricTypeIndex";
        public static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                        .withRegion("eu-north-1")
                        .build();

        public static final DynamoDB dynamoDB = new DynamoDB(client);

        public AmazonDynamoDB getClient() {
                return client;
        }

        /**
         * Setup the DynamoDB table
         */
        public static void setupTable() {
                try {
                        // Define the primary key schema
                        // Crazy magic down here
                        CreateTableRequest createTableRequest = new CreateTableRequest()
                                        .withTableName(TABLE_NAME)
                                        .withKeySchema(
                                                        new KeySchemaElement("RequestId", KeyType.HASH),
                                                        new KeySchemaElement("MetricType", KeyType.RANGE))
                                        .withAttributeDefinitions(
                                                        new AttributeDefinition("RequestId", ScalarAttributeType.S),
                                                        new AttributeDefinition("MetricType", ScalarAttributeType.S),
                                                        new AttributeDefinition("StartTime", ScalarAttributeType.N))
                                        .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L))
                                        .withGlobalSecondaryIndexes(
                                                        new GlobalSecondaryIndex()
                                                                        .withIndexName(GSI_NAME)
                                                                        .withKeySchema(
                                                                                        new KeySchemaElement(
                                                                                                        "MetricType",
                                                                                                        KeyType.HASH),
                                                                                        new KeySchemaElement(
                                                                                                        "StartTime",
                                                                                                        KeyType.RANGE))
                                                                        .withProvisionedThroughput(
                                                                                        new ProvisionedThroughput(10L,
                                                                                                        10L))
                                                                        .withProjection(new Projection()
                                                                                        .withProjectionType(
                                                                                                        ProjectionType.ALL)));

                        // Create the table if it does not exist
                        Table table = dynamoDB.createTable(createTableRequest);
                        table.waitForActive();
                        System.out.println("Table created successfully.");
                } catch (ResourceInUseException e) {
                        System.out.println("Table already exists.");
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
}
