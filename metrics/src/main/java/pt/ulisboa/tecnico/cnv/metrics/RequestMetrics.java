package pt.ulisboa.tecnico.cnv.metrics;

import com.amazonaws.services.dynamodbv2.document.Item;

import java.util.UUID;

import org.json.JSONObject;

public class RequestMetrics {
    public static enum MetricType {
        BLUR,
        ENHANCE,
        RAYTRACER
    }

    private String requestId;
    private int sInputSize;
    private int wInputSize;
    private boolean antiAlias;
    private boolean textureMap;
    private long startTime;
    private long endTime;
    private long startProcessingTime;
    private long endProcessingTime;
    private long processingTime;
    private int numberOfInstructions;
    private int numberOfBlocks;
    private int imageSize;
    private MetricType metricType;

    /**
     * Constructor for RequestMetrics
     */
    public RequestMetrics() {
        this.requestId = UUID.randomUUID().toString();
        this.startTime = System.currentTimeMillis();
        this.numberOfInstructions = 0;
        this.numberOfBlocks = 0;
    }

    /**
     * Set the end time of the request
     */
    public void setEndTime() {
        this.endTime = System.currentTimeMillis();
    }

    /**
     * Set the start processing time of the request
     */
    public void setStartProcessingTime() {
        this.startProcessingTime = System.currentTimeMillis();
    }

    /**
     * Set the end processing time of the request
     */
    public void setEndProcessingTime() {
        this.endProcessingTime = System.currentTimeMillis();
        this.processingTime = this.endProcessingTime - this.startProcessingTime;
    }

    public void createRaytracerInput(int sInputSize, int wInputSize, boolean antiAlias, boolean textureMap) {
        this.sInputSize = sInputSize;
        this.wInputSize = wInputSize;
        this.antiAlias = antiAlias;
        this.textureMap = textureMap;
    }

    /**
     * Set the number of instructions of the request
     * 
     * @param numberOfInstructions
     */
    public void setNumberOfInstructions(int numberOfInstructions) {
        this.numberOfInstructions = numberOfInstructions;
    }

    /**
     * update the number of instructions of the request
     * 
     * @param numberOfInstructions
     */
    public void updateNumberOfInstructions(int numberOfInstructions) {
        this.numberOfInstructions += numberOfInstructions;
        this.numberOfBlocks++;
    }

    /**
     * Get the duration of the request
     * 
     * @return long
     */
    public long getDurationInMs() {
        return this.endTime - this.startTime;
    }

    /**
     * Set the image size of the request
     * 
     * @param imageSize
     */
    public void setImageSize(int imageSize) {
        this.imageSize = imageSize;
    }

    /**
     * Get the start time of the request
     * 
     * @return long
     */
    public long getStartTime() {
        return this.startTime;
    }

    /**
     * Get the end time of the request
     * 
     * @return long
     */
    public long getEndTime() {
        return this.endTime;
    }

    /**
     * Get the start processing time of the request
     * 
     * @return long
     */
    public long getStartProcessingTime() {
        return this.startProcessingTime;
    }

    /**
     * Get the end processing time of the request
     * 
     * @return long
     */
    public long getEndProcessingTime() {
        return this.endProcessingTime;
    }

    /**
     * Get the processing time of the request
     * 
     * @return long
     */
    public long getProcessingTime() {
        return this.processingTime;
    }

    /**
     * Get the number of instructions of the request
     * 
     * @return int
     */
    public int getNumberOfInstructions() {
        return this.numberOfInstructions;
    }

    /**
     * Get the number of blocks of the request
     * 
     * @return int
     */
    public int getNumberOfBlocks() {
        return this.numberOfBlocks;
    }

    /**
     * Get the image size of the request
     * 
     * @return int
     */
    public int getImageSize() {
        return this.imageSize;
    }

    /**
     * Set the metric type of the request
     * 
     * @param metricType
     */
    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    /**
     * Get the metric type of the request
     * 
     * @return MetricType
     */
    public MetricType getMetricType() {
        return this.metricType;
    }

    /**
     * Get the scene input size of the request
     * 
     * @return int
     */
    public int getSInputSize() {
        return this.sInputSize;
    }

    /**
     * Get the world input size of the request
     * 
     * @return int
     */
    public int getWInputSize() {
        return this.wInputSize;
    }

    /**
     * Get the anti alias of the request
     * 
     * @return boolean
     */
    public boolean getAntiAlias() {
        return this.antiAlias;
    }

    /**
     * Get the texture map of the request
     * 
     * @return boolean
     */
    public boolean getTextureMap() {
        return this.textureMap;
    }

    /**
     * Get the request metrics in JSON format
     * 
     * @return JSONObject
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject(this);
        return json;
    }

    /**
     * Get the request metrics in string format
     * 
     * @return String
     */
    public String toString() {
        return this.toJSON().toString();
    }

    /*
     * Convert the RequestMetrics object to an Item object
     * 
     * @return Item
     */
    public Item toItem() {
        return new Item()
                .withPrimaryKey("RequestId", requestId)
                .withString("MetricType", metricType.name())
                .withInt("sInputSize", sInputSize)
                .withInt("wInputSize", wInputSize)
                .withBoolean("antiAlias", antiAlias)
                .withBoolean("textureMap", textureMap)
                .withLong("startTime", startTime)
                .withLong("endTime", endTime)
                .withLong("processingTime", processingTime)
                .withInt("numberOfInstructions", numberOfInstructions)
                .withInt("numberOfBlocks", numberOfBlocks)
                .withInt("imageSize", imageSize);
    }

    /*
     * Convert the Item object to a RequestMetrics object
     * 
     * @param item
     * 
     * @return RequestMetrics
     */
    public static RequestMetrics fromItem(Item item) {
        RequestMetrics metrics = new RequestMetrics();
        metrics.requestId = item.getString("RequestId");
        metrics.metricType = MetricType.valueOf(item.getString("MetricType"));
        metrics.sInputSize = item.getInt("sInputSize");
        metrics.wInputSize = item.getInt("wInputSize");
        metrics.antiAlias = item.getBoolean("antiAlias");
        metrics.textureMap = item.getBoolean("textureMap");
        metrics.startTime = item.getLong("startTime");
        metrics.endTime = item.getLong("endTime");
        metrics.processingTime = item.getLong("processingTime");
        metrics.numberOfInstructions = item.getInt("numberOfInstructions");
        metrics.numberOfBlocks = item.getInt("numberOfBlocks");
        metrics.imageSize = item.getInt("imageSize");
        return metrics;
    }

}
