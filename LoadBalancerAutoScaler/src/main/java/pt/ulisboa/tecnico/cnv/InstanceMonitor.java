package pt.ulisboa.tecnico.cnv;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class InstanceMonitor {
    private final AmazonCloudWatch cloudWatch;

    public InstanceMonitor(AmazonCloudWatch cloudWatch) {
        this.cloudWatch = cloudWatch;
    }

    public Map<String, Double> getInstancesCPUUsage() {
        Map<String, Double> cpuUsages = new HashMap<>();
        Collection<ServerInstance> instances = SharedInstanceRegistry.getInstances();

        for (ServerInstance instance : instances) {
            double cpuUsage = getInstanceCPUUsage(instance.getInstanceId());
            cpuUsages.put(instance.getInstanceId(), cpuUsage);
        }

        return cpuUsages;
    }

    private double getInstanceCPUUsage(String instanceId) {
        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
            .withNamespace("Custom/Metrics")
            .withMetricName("CPUUtilization")
            .withDimensions(new Dimension().withName("InstanceId").withValue(instanceId))
            .withStartTime(new Date(System.currentTimeMillis() - 300000)) // 5 minutes ago
            .withEndTime(new Date())
            .withPeriod(60)
            .withStatistics(Statistic.Average);

        GetMetricStatisticsResult result = cloudWatch.getMetricStatistics(request);
        List<Datapoint> datapoints = result.getDatapoints();

        return datapoints.isEmpty() ? -1 : datapoints.get(0).getAverage();
    }
}
