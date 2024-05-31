package pt.ulisboa.tecnico.cnv.loadbalancerautoscaler;

import java.util.ArrayList;
import java.util.List;

import io.github.cdimascio.dotenv.Dotenv;

public class App {
    public static void main(String[] args) throws Exception {

        // TODO : remove later
        Dotenv dotenv = Dotenv.load();
        String keyName = dotenv.get("AWS_KEY_NAME");
        String securityGroup = dotenv.get("AWS_SECURITY_GROUP");
        String accessKey = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String iamRole = dotenv.get("IAM_ROLE_NAME");
        String amiId = "ami-09346642be89eda35";
        String instanceType = "t3.micro";

        System.out.println("Testing env variables, accessKey: " + accessKey);

        AutoScaler autoScaler = new AutoScaler(accessKey, secretKey, amiId, instanceType, keyName, securityGroup, iamRole);
        autoScaler.scaleUp();
        autoScaler.scaleUp();

        LoadBalancer loadBalancer = new LoadBalancer();
        int loadBalancerPort = 8080;
        loadBalancer.start(loadBalancerPort);

        System.out.println("LoadBalancer is running on port " + loadBalancerPort);
    }
}
