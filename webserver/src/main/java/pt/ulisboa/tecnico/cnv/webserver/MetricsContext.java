package pt.ulisboa.tecnico.cnv.webserver;

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
