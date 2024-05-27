package pt.ulisboa.tecnico.cnv;

import java.util.ArrayList;
import java.util.List;

import io.github.cdimascio.dotenv.Dotenv;

public class App {
    public static void main(String[] args) throws Exception {
        //LoadBalancer loadBalancer = new LoadBalancer();
        //loadBalancer.start(8080);
        Dotenv dotenv = Dotenv.load();

        String keyName = dotenv.get("AWS_KEY_NAME");
        String securityGroup = dotenv.get("AWS_SECURITY_GROUP");
        String accessKey = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String amiId = "ami-0414abd23b09a9c44";
        String instanceType = "t3.micro";
        
        
        System.out.println("Testing env variables, accessKey:" + accessKey);
        AutoScaler autoScaler = new AutoScaler(accessKey, secretKey, amiId, instanceType, keyName, securityGroup);

        // Periodically check and scale
        // This can be done using a scheduled executor or similar mechanism
    }
}
