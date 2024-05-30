package pt.ulisboa.tecnico.cnv.loadbalancerautoscaler;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
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
    private final InstanceMonitor instanceMonitor;

    public AutoScaler(String accessKey, String secretKey, String amiId, String instanceType, String keyName,
            String securityGroup) {
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
        this.instanceMonitor = new InstanceMonitor(cloudWatch);

        // Schedule the CPU usage logging task
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(instanceMonitor::logAverageCPUUsage, 0, 60, TimeUnit.SECONDS);
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
        String instanceAddress = waitForDNS(instanceId);

        // Register the new instance in the shared registry
        SharedInstanceRegistry.addInstance(instanceId, new ServerInstance(instanceId, instanceAddress));

        System.out.println("Instance launched: " + instanceId + " with DNS: " + instanceAddress);
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

            System.out.println("Instance terminated: " + instanceId);
        }
    }

    private List<Instance> getRunningInstances() {
        return ec2.describeInstances().getReservations().stream()
                .flatMap(reservation -> reservation.getInstances().stream())
                .filter(instance -> instance.getState().getName().equals(InstanceStateName.Running.toString()))
                .collect(Collectors.toList());
    }

    private String waitForDNS(String instanceId) {
        final int maxRetries = 10;
        final long sleepInterval = 1000; // 1 second

        for (int i = 0; i < maxRetries; i++) {
            String dnsName = getDNS(instanceId);
            if (dnsName != null && !dnsName.isEmpty()) {
                return dnsName;
            }

            try {
                System.out.println("Waiting for DNS to be available for instance: " + instanceId);
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread was interrupted while waiting for DNS.");
                return null;
            }
        }

        System.err.println("DNS not available for instance " + instanceId + " after " + maxRetries + " retries.");
        return null;
    }

    private String getDNS(String instanceId) {
        try {
            DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
            DescribeInstancesResult result = ec2.describeInstances(request);

            for (Reservation reservation : result.getReservations()) {
                for (Instance instance : reservation.getInstances()) {
                    if (instance.getInstanceId().equals(instanceId)) {
                        return instance.getPublicDnsName();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving DNS for instance " + instanceId + ": " + e.getMessage());
        }
        return null;
    }
}
