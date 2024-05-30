package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.json.JSONObject;
import org.json.JSONException;

public class RequestParamsHandler {
    private final Map<String, String> queryParams;
    private final String requestType;
    private final String payload;
    private final URI requestUri;

    public RequestParamsHandler(HttpExchange exchange, String payload) throws IOException {
        this.requestUri = exchange.getRequestURI();
        this.queryParams = parseQueryParams(requestUri.getQuery());
        this.requestType = determineRequestType();
        this.payload = payload;
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> queryParams = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length > 1) {
                    queryParams.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return queryParams;
    }

    private String determineRequestType() {
        if (requestUri.getPath().contains("raytracer")) {
            return "raytracer";
        } else if (requestUri.getPath().contains("blurimage")) {
            return "blur";
        } else if (requestUri.getPath().contains("enhanceimage")) {
            return "enhance";
        }
        return "unknown";  // Handle error or default case as needed
    }

    public Map<String, Object> getRequestDetails() {
        Map<String, Object> details = new HashMap<>();
        details.put("requestType", requestType);
        details.put("imageSize", calculateImageSize());
        details.put("hasAntiAlias", hasAntiAlias());
        details.put("hasTextureMap", hasTextureMap());
        return details;
    }

    private boolean hasAntiAlias() {
        return requestType.equals("raytracer") && queryParams.getOrDefault("aa", "false").equals("true");
    }

    private boolean hasTextureMap() {
        if (!requestType.equals("raytracer")) return false;
        try {
            JSONObject json = new JSONObject(payload);
            return json.has("texmap");
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int calculateImageSize() {
        if (requestType.equals("raytracer")) {
            int sceneCols = Integer.parseInt(queryParams.getOrDefault("scols", "0"));
            int sceneRows = Integer.parseInt(queryParams.getOrDefault("srows", "0"));
            return (sceneCols * sceneRows);
        } else if (requestType.equals("enhance") || requestType.equals("blur")) {
            return calculateImageSizeFromBase64Payload();
        }
        return 0;
    }

    private int calculateImageSizeFromBase64Payload() {
        try {
            String base64Image = payload.substring(payload.indexOf(",") + 1);
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            try (InputStream in = new ByteArrayInputStream(imageBytes)) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    return image.getWidth() * image.getHeight();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0; // In case of error or invalid data
    }
}
