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
import java.util.concurrent.*;

public class AutoScaler {
    private final AmazonEC2 ec2;
    private final AmazonCloudWatch cloudWatch;
    private final String amiId;
    private final String instanceType;
    private final String keyName;
    private final String securityGroup;
    private final String iamRole;
    private final InstanceMonitor instanceMonitor;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private volatile long lastScaleUpTime = 0;

    public AutoScaler(String accessKey, String secretKey, String amiId, String instanceType, String keyName,
            String securityGroup, String iamRole) {
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
        this.iamRole = iamRole;

        // TODO move to webserver
        scheduler.scheduleAtFixedRate(this::monitorAndScale, 0, 10, TimeUnit.SECONDS);
    }

    private void monitorAndScale() {
        Map<String, Double> cpuUsages = instanceMonitor.getInstancesCPUUsage();
        double averageCPUUsage = getAverageCpuUsage(cpuUsages);
        System.out.println("CPU Usages: " + cpuUsages);
        System.out.println("AvgCpu Usage: " + averageCPUUsage);

        if (averageCPUUsage > 90.0 && (System.currentTimeMillis() - lastScaleUpTime) > 60000) {
            
            scaleUp();
            lastScaleUpTime = System.currentTimeMillis();
        }

        Collection<ServerInstance> allInstances = SharedInstanceRegistry.getInstances();
        int totalInstances = allInstances.size();

        for (Map.Entry<String, Double> entry : cpuUsages.entrySet()) {
            String instanceId = entry.getKey();
            Double cpuUsage = entry.getValue();
            ServerInstance serverInstance = SharedInstanceRegistry.getInstance(instanceId);
            
            if (cpuUsage > 0 && cpuUsage < 15.0 && serverInstance != null && !serverInstance.isMarkedForTermination() && (System.currentTimeMillis() - lastScaleUpTime) > 60000) {
                if (totalInstances > 2) {
                    serverInstance.markForTermination();
                    System.out.println("Instance marked for termination: " + instanceId);
                    break;
                }
            }
        }
        // Check for instances that are marked for termination and can be safely
        // terminated
        allInstances.stream()
                .filter(ServerInstance::isMarkedForTermination)
                .filter(instance -> instance.getNumRunningRequests() == 0)
                .forEach(instance -> terminateInstance(instance.getInstanceId()));
    }

    private double getAverageCpuUsage(Map<String, Double> cpuUsages) {
        double totalCPUUsage = 0;
        int instanceCount = cpuUsages.size();

        for (double usage : cpuUsages.values()) {
            totalCPUUsage += usage;
        }

        return instanceCount > 0 ? totalCPUUsage / instanceCount : 0;
    }

    private void terminateInstance(String instanceId) {
        System.out.println("Terminating instance"+ instanceId);

        TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest()
                .withInstanceIds(instanceId);
        ec2.terminateInstances(terminateRequest);
        SharedInstanceRegistry.removeInstance(instanceId);
        System.out.println("Instance terminated: " + instanceId);
    }

    public void scaleUp() {
        System.out.println("Scaling up!");
        IamInstanceProfileSpecification iamInstanceProfile = new IamInstanceProfileSpecification()
        .withName(iamRole);

        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
                .withImageId(amiId)
                .withInstanceType(instanceType)
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName(keyName)
                .withSecurityGroupIds(securityGroup)
                .withIamInstanceProfile(iamInstanceProfile) // Setting the IAM role
                .withMonitoring(true); // Enable detailed monitoring

        RunInstancesResult result = ec2.runInstances(runInstancesRequest);
        Instance instance = result.getReservation().getInstances().get(0);
        String instanceId = instance.getInstanceId();
        String instanceAddress = waitForDNS(instanceId);

        SharedInstanceRegistry.addInstance(instanceId, new ServerInstance(instanceId, instanceAddress));
        System.out.println("Instance launched: " + instanceId + " with DNS: " + instanceAddress);
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
        DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
        DescribeInstancesResult result = ec2.describeInstances(request);

        for (Reservation reservation : result.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                if (instance.getInstanceId().equals(instanceId)) {
                    return instance.getPublicDnsName();
                }
            }
        }
        return null;
    }
}
