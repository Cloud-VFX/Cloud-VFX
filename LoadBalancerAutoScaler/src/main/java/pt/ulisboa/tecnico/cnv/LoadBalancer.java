package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoadBalancer {
    private final ExecutorService executorService;
    private final HealthChecker healthChecker;
    private final RequestManager requestManager;

    public LoadBalancer() {
        this.executorService = Executors.newCachedThreadPool();
        this.requestManager = new RequestManager();
        this.healthChecker = new HealthChecker(requestManager);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", requestManager);
        server.setExecutor(executorService);
        server.start();
        System.out.println("LoadBalancer started on port " + port);
        healthChecker.scheduleHealthChecks();
    }
}
