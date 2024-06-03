package pt.ulisboa.tecnico.cnv.loadbalancerautoscaler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import io.github.cdimascio.dotenv.Dotenv;
import pt.ulisboa.tecnico.cnv.metrics.MetricsAggregator;

public class App {
    public static void main(String[] args) throws Exception {

        Dotenv dotenv = Dotenv.load();
        String keyName = dotenv.get("AWS_KEY_NAME");
        String securityGroup = dotenv.get("AWS_SECURITY_GROUP");
        String accessKey = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String amiId = dotenv.get("AWS_AMI_ID");
        String iamRole = dotenv.get("IAM_ROLE_NAME");
        String instanceType = "t3.micro";
        if (keyName == null || securityGroup == null || accessKey == null || secretKey == null || amiId == null) {
            // System.out.println(keyName);
            // System.out.println(securityGroup);
            // System.out.println(accessKey);
            // System.out.println(secretKey);
            // System.out.println(amiId);
            // System.out.println(iamRole);
            // System.out.println(instanceType);
            System.out.println("Please set the environment variables");
            System.exit(1);
        }

        LoadBalancer loadBalancer = new LoadBalancer();
        int loadBalancerPort = 8080;
        loadBalancer.start(loadBalancerPort);

        System.out.println("LoadBalancer is running on port " + loadBalancerPort);

        System.out.println("Starting AutoScaler...");
        AutoScaler autoScaler = new AutoScaler(accessKey, secretKey, amiId, instanceType, keyName, securityGroup,
                iamRole);
        System.out.println("AutoScaler started");
        autoScaler.scaleUp();
        autoScaler.scaleUp();

        // Scheduled periodic aggregation of metrics
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        MetricsAggregator aggregator = new MetricsAggregator();
        scheduler.scheduleAtFixedRate(() -> handleAggregation(aggregator), 10, 60,
                TimeUnit.SECONDS);
    }

    private static void handleAggregation(MetricsAggregator aggregator) {
        try {
            aggregator.handleRequest(null, new Context() {
                @Override
                public String getAwsRequestId() {
                    return "localRequestId";
                }

                @Override
                public String getLogGroupName() {
                    return "localLogGroup";
                }

                @Override
                public String getLogStreamName() {
                    return "localLogStream";
                }

                @Override
                public String getFunctionName() {
                    return "localFunction";
                }

                @Override
                public String getFunctionVersion() {
                    return "1.0";
                }

                @Override
                public String getInvokedFunctionArn() {
                    return "localArn";
                }

                @Override
                public CognitoIdentity getIdentity() {
                    return null;
                }

                @Override
                public ClientContext getClientContext() {
                    return null;
                }

                @Override
                public int getRemainingTimeInMillis() {
                    return 10000;
                }

                @Override
                public int getMemoryLimitInMB() {
                    return 512;
                }

                @Override
                public LambdaLogger getLogger() {
                    return new LambdaLogger() {
                        @Override
                        public void log(String message) {
                            System.out.println(message);
                        }

                        @Override
                        public void log(byte[] message) {
                            System.out.println(new String(message));
                        }
                    };
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
