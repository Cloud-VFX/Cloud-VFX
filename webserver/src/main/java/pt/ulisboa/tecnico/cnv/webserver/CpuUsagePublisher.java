package pt.ulisboa.tecnico.cnv.webserver;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import java.lang.management.ManagementFactory;
import java.net.URI;

import com.sun.management.OperatingSystemMXBean;
import com.amazonaws.AmazonClientException;

public class CpuUsagePublisher {
    private static final AmazonCloudWatch cloudWatch = AmazonCloudWatchClientBuilder.standard().build();
    private static String instanceId = null;

    public static void main(String[] args) {
        instanceId = getInstanceId(); // Retrieve instance ID only once on startup
    }

    public void publishCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        double cpuLoad = osBean.getSystemCpuLoad() * 100;

        if (cpuLoad == -1.0) {
            System.out.println("CPU load information not available.");
            return;
        }

        MetricDatum datum = new MetricDatum()
                .withMetricName("CPUUtilization")
                .withUnit(StandardUnit.Percent)
                .withValue(cpuLoad)
                .withStorageResolution(1);  // Setting high-resolution metrics

        if (instanceId != null) {
            datum.withDimensions(new Dimension().withName("InstanceId").withValue(instanceId));
        }

        PutMetricDataRequest request = new PutMetricDataRequest()
                .withNamespace("Custom/Metrics")
                .withMetricData(datum);

        try {
            cloudWatch.putMetricData(request);
            System.out.println("Successfully published CPU usage to CloudWatch: " + cpuLoad + "%");
        } catch (AmazonClientException ace) {
            System.err.println("Error publishing CPU usage to CloudWatch: " + ace.getMessage());
        }
    }

    public static String getInstanceId() {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://169.254.169.254/latest/meta-data/instance-id"))
            .GET()
            .build();
    try {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    } catch (Exception e) {
        System.err.println("Error retrieving instance ID: " + e.getMessage());
        return null;
    }
}
}
