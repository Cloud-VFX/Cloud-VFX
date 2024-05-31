package pt.ulisboa.tecnico.cnv.webserver;

import java.io.IOException;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.URI;

public class RootHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange he) throws IOException {
        // Handling CORS
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if (he.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            he.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            he.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            he.sendResponseHeaders(204, -1);
            return;
        }

        // Send "Hello, World!" response
        String response = "Hello, World!";
        he.sendResponseHeaders(200, response.length());
        he.getResponseBody().write(response.getBytes());
        he.getResponseBody().close();
    }
}