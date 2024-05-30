package pt.ulisboa.tecnico.cnv.loadbalancerautoscaler;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class InstanceMonitor {
    private final AmazonCloudWatch cloudWatch;

    public InstanceMonitor(AmazonCloudWatch cloudWatch) {
        this.cloudWatch = cloudWatch;
    }

    public void logAverageCPUUsage() {
        Collection<ServerInstance> instances = SharedInstanceRegistry.getInstances();
        if (instances.isEmpty()) {
            System.out.println("No instances to monitor.");
            return;
        }

        double totalCPUUsage = 0;
        int instanceCount = 0;

        for (ServerInstance instance : instances) {
            double cpuUsage = getInstanceCPUUsage(instance.getInstanceId());
            if (cpuUsage >= 0) {
                totalCPUUsage += cpuUsage;
                instanceCount++;
            }
        }

        if (instanceCount > 0) {
            double averageCPUUsage = totalCPUUsage / instanceCount;
            logCPUUsage(averageCPUUsage);
        }
    }

    private double getInstanceCPUUsage(String instanceId) {
        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                .withNamespace("AWS/EC2")
                .withMetricName("CPUUtilization")
                .withDimensions(new Dimension().withName("InstanceId").withValue(instanceId))
                .withStartTime(new Date(System.currentTimeMillis() - 300000)) // 5 minutes ago
                .withEndTime(new Date())
                .withPeriod(60)
                .withStatistics(Statistic.Average);

        GetMetricStatisticsResult result = cloudWatch.getMetricStatistics(request);
        List<Datapoint> datapoints = result.getDatapoints();

        if (!datapoints.isEmpty()) {
            return datapoints.get(0).getAverage();
        } else {
            System.out.println("No data points returned for instance: " + instanceId);
            return -1;
        }
    }

    private void logCPUUsage(double averageCPUUsage) {
        System.out.println("Average CPU Usage: " + averageCPUUsage + "%");

        try (FileWriter writer = new FileWriter("cpu_usage.log", true)) {
            writer.write(new Date() + " - Average CPU Usage: " + averageCPUUsage + "%\n");
        } catch (IOException e) {
            System.err.println("Failed to log CPU usage: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
