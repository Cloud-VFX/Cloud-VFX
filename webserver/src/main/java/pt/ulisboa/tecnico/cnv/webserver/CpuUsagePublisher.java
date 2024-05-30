package pt.ulisboa.tecnico.cnv.webserver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import com.amazonaws.AmazonClientException;

public class CpuUsagePublisher {
    private static final AmazonCloudWatch cloudWatch = AmazonCloudWatchClientBuilder.standard()
            .build();

    public static void publishCpuUsage() {
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
}
