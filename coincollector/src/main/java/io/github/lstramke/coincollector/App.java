package io.github.lstramke.coincollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.awt.Desktop;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        log.info("✅ Starting CoinCollector...");

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            InputStream is = App.class.getResourceAsStream("/static" + path);
            
            if (is != null) {
                byte[] response = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", getContentType(path));
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.getResponseBody().close();
                is.close();
            } else {
                String notFound = "404 - Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                exchange.getResponseBody().write(notFound.getBytes());
                exchange.getResponseBody().close();
            }
        });
        
        server.setExecutor(null);
        server.start();
        
        log.info("✅ Server started on http://localhost:{}", PORT);
        
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:" + PORT));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "text/plain";
    }
}
