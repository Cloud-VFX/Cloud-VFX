package pt.ulisboa.tecnico.cnv.webserver;

import org.json.JSONObject;

public class RequestMetrics {
    public static enum MetricType {
        BLUR,
        ENHANCE,
        RAYTRACER
    }

    private long startTime;
    private long endTime;
    private long startProcessingTime;
    private long endProcessingTime;
    private long processingTime;
    private int imageSize;
    private double maxRamUsage;
    private double maxCpuUsage;
    private MetricType metricType;

    /**
     * Constructor for RequestMetrics
     */
    public RequestMetrics() {
        this.startTime = System.currentTimeMillis();
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
     * Set the maximum ram usage of the request
     * 
     * @param maxRamUsage
     */
    public void setMaxRamUsage(double maxRamUsage) {
        this.maxRamUsage = maxRamUsage;
    }

    /**
     * Update the maximum ram usage of the request
     * 
     * @param maxRamUsage
     */
    public synchronized void updateMaxRamUsage(double maxRamUsage) {
        if (maxRamUsage > this.maxRamUsage) {
            this.maxRamUsage = maxRamUsage;
        }
    }

    /**
     * Set the maximum cpu usage of the request
     * 
     * @param maxCpuUsage
     */
    public void setMaxCpuUsage(double maxCpuUsage) {
        this.maxCpuUsage = maxCpuUsage;
    }

    /**
     * Update the maximum cpu usage of the request
     * 
     * @param maxCpuUsage
     */
    public synchronized void updateMaxCpuUsage(double maxCpuUsage) {
        if (maxCpuUsage > this.maxCpuUsage) {
            this.maxCpuUsage = maxCpuUsage;
        }
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
     * Get the image size of the request
     * 
     * @return int
     */
    public int getImageSize() {
        return this.imageSize;
    }

    /**
     * Get the maximum ram usage of the request
     * 
     * @return int
     */
    public double getMaxRamUsage() {
        return this.maxRamUsage;
    }

    /**
     * Get the maximum cpu usage of the request
     * 
     * @return int
     */
    public double getMaxCpuUsage() {
        return this.maxCpuUsage;
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
}