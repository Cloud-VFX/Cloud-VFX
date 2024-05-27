package pt.ulisboa.tecnico.cnv;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AutoScaler {
    private final AmazonEC2 ec2;
    private final AmazonCloudWatch cloudWatch;
    private final String amiId;
    private final String instanceType;
    private final String keyName;
    private final String securityGroup;
    private final Set<String> instanceIds = Collections.synchronizedSet(new HashSet<>());

    public AutoScaler(String accessKey, String secretKey, String amiId, String instanceType, String keyName, String securityGroup) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        this.ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion(Regions.EU_NORTH_1)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
        this.cloudWatch = AmazonCloudWatchClientBuilder.standard()
                .withRegion(Regions.EU_NORTH_1)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
        this.amiId = amiId;
        this.instanceType = instanceType;
        this.keyName = keyName;
        this.securityGroup = securityGroup;

        scaleUp();
        // Schedule the CPU usage logging task
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::logAverageCPUUsage, 0, 60, TimeUnit.SECONDS);
    }

    public void scaleUp() {
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
                .withImageId(amiId)
                .withInstanceType(instanceType)
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName(keyName)
                .withSecurityGroupIds(securityGroup)
                .withMonitoring(true); // Enable detailed monitoring

        RunInstancesResult result = ec2.runInstances(runInstancesRequest);
        Instance instance = result.getReservation().getInstances().get(0);
        String instanceId = instance.getInstanceId();
        String instanceAddress = instance.getPublicDnsName();

        // Register the new instance in the shared registry
        SharedInstanceRegistry.addInstance(instanceId, new ServerInstance(instanceId, instanceAddress));
        instanceIds.add(instanceId);

        System.out.println("Instance launched: " + instanceId);
    }

    public void scaleDown() {
        List<Instance> instances = getRunningInstances();

        if (!instances.isEmpty()) {
            Instance instance = instances.get(0);
            String instanceId = instance.getInstanceId();

            TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest()
                    .withInstanceIds(instanceId);
            ec2.terminateInstances(terminateRequest);

            // Remove the instance from the shared registry
            SharedInstanceRegistry.removeInstance(instanceId);
            instanceIds.remove(instanceId);

            System.out.println("Instance terminated: " + instanceId);
        }
    }

    private List<Instance> getRunningInstances() {
        return ec2.describeInstances().getReservations().stream()
                .flatMap(reservation -> reservation.getInstances().stream())
                .filter(instance -> instance.getState().getName().equals(InstanceStateName.Running.toString()))
                .collect(Collectors.toList());
    }

    private void logAverageCPUUsage() {
        if (instanceIds.isEmpty()) {
            System.out.println("No instances to monitor.");
            return;
        }

        double totalCPUUsage = 0;
        int instanceCount = 0;

        for (String instanceId : instanceIds) {
            double cpuUsage = getInstanceCPUUsage(instanceId);
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
            e.printStackTrace();
        }
    }
}
