package pt.ulisboa.tecnico.cnv;

import java.util.ArrayList;
import java.util.List;

import io.github.cdimascio.dotenv.Dotenv;

public class App {
    public static void main(String[] args) throws Exception {

        Dotenv dotenv = Dotenv.load();

        String keyName = dotenv.get("AWS_KEY_NAME");
        String securityGroup = dotenv.get("AWS_SECURITY_GROUP");
        String accessKey = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String amiId = "ami-0414abd23b09a9c44";
        String instanceType = "t3.micro";
        
        System.out.println("Testing env variables, accessKey: " + accessKey);
        
        // Initialize AutoScaler
        AutoScaler autoScaler = new AutoScaler(accessKey, secretKey, amiId, instanceType, keyName, securityGroup);
        
        // Initialize and start the LoadBalancer
        LoadBalancer loadBalancer = new LoadBalancer(); // Assuming LoadBalancer does not require any arguments
        int loadBalancerPort = 8080; // Choose an appropriate port for the load balancer
        loadBalancer.start(loadBalancerPort); // Start the load balancer

        System.out.println("LoadBalancer is running on port " + loadBalancerPort);
    }
}
