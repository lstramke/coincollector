package io.github.lstramke.coincollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import io.github.lstramke.coincollector.configuration.ApplicationContext;
import io.github.lstramke.coincollector.configuration.InitService;
import io.github.lstramke.coincollector.exceptions.StorageInitializeException;
import io.github.lstramke.coincollector.services.SessionFilter;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static int PORT = 8080;
    private static HttpServer server;
    private static String DB_FILE_PATH = "coincollector.db";

    public static void main(String[] args) throws IOException {
        logger.info("✅ Starting CoinCollector...");

        DB_FILE_PATH = args.length > 0 ? args[0] : "coincollector.db";
        PORT = args.length > 1 ? Integer.parseInt(args[1]) : 8080;

        ApplicationContext context;
        try {
            context = InitService.initialize(DB_FILE_PATH);
        } catch (StorageInitializeException e) {
            logger.error("Application initialization failed: {}", e.getMessage());
            System.exit(1);
            return;
        }

        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
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

        server.createContext("/api/login", exchange -> {
            try {
                context.loginHandler().handle(exchange);
            } catch (IOException | RuntimeException e) {
                String errorJson = "{\"error\":\"An unexpected error occurred\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorJson.length());
                exchange.getResponseBody().write(errorJson.getBytes());
                exchange.getResponseBody().close();
            }
        });

        server.createContext("/api/registration", exchange -> {
            try{
                context.registrationHandler().handle(exchange);
            } catch (IOException | RuntimeException e) {
                String errorJson = "{\"error\":\"An unexpected error occurred\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorJson.length());
                exchange.getResponseBody().write(errorJson.getBytes());
                exchange.getResponseBody().close();
            }
        });

        server.createContext("/api/groups", SessionFilter.withSessionValidation(context.groupHandler(), context.sessionManager()));
        server.createContext("/api/collections", SessionFilter.withSessionValidation(context.collectionHandler(), context.sessionManager()));
        server.createContext("/api/coins", SessionFilter.withSessionValidation(context.coinHandler(), context.sessionManager()));
        server.createContext("/api/logout", SessionFilter.withSessionValidation(context.logoutHandler(), context.sessionManager()));
        
        server.setExecutor(null);
        server.start();
        
        logger.info("✅ Server started on http://localhost:{}", PORT);
        
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

    public static void stopServer() {
        if(server != null){
            server.stop(0);
            logger.info("server stopped");
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
