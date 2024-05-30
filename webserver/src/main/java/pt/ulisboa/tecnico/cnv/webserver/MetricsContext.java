package pt.ulisboa.tecnico.cnv.webserver;

import pt.ulisboa.tecnico.cnv.metrics.RequestMetrics;

public class MetricsContext {
    /**
     * ThreadLocal to store the metrics of the request
     */
    private static final ThreadLocal<RequestMetrics> metrics = new ThreadLocal<>();

    /**
     * Start the metrics of the request
     */
    public static void start() {
        metrics.set(new RequestMetrics());
    }

    /**
     * End the metrics of the request
     */
    public static void end() {
        metrics.get().setEndTime();
    }

    /**
     * Set the image size of the request
     * 
     * @param imageSize
     */
    public static void setImageSize(int imageSize) {
        metrics.get().setImageSize(imageSize);
    }

    /**
     * Set the number of blocks of the request
     * 
     * @param numberOfBlocks
     */
    public static void createRaytracerInput(int sInputSize, int wInputSize, boolean antiAlias, boolean textureMap) {
        metrics.get().createRaytracerInput(sInputSize, wInputSize, antiAlias, textureMap);
    }

    /**
     * Set the start processing time of the request
     */
    public static void setStartProcessingTime() {
        metrics.get().setStartProcessingTime();
    }

    /**
     * Set the end processing time of the request
     */
    public static void setEndProcessingTime() {
        metrics.get().setEndProcessingTime();
    }

    /**
     * Set the number of instructions of the request
     * 
     * @param numberOfInstructions
     */
    public static void setNumberOfInstructions(int numberOfInstructions) {
        metrics.get().setNumberOfInstructions(numberOfInstructions);
    }

    /**
     * Update the number of instructions of the request
     * 
     * @param numberOfInstructions
     */
    public static void updateNumberOfInstructions(int numberOfInstructions) {
        metrics.get().updateNumberOfInstructions(numberOfInstructions);
    }

    /**
     * Set the metric type of the request
     * 
     * @param metricType
     */
    public static void setMetricType(RequestMetrics.MetricType metricType) {
        metrics.get().setMetricType(metricType);
    }

    /**
     * Get the metrics of the request
     * 
     * @return RequestMetrics
     */
    public static RequestMetrics getMetrics() {
        return metrics.get();
    }
}
