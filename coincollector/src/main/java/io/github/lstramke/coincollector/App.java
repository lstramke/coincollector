package io.github.lstramke.coincollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import io.github.lstramke.coincollector.configuration.DatabaseTableProperties;
import io.github.lstramke.coincollector.configuration.SqliteInitializer;
import io.github.lstramke.coincollector.exceptions.StorageInitializeException;
import io.github.lstramke.coincollector.handler.CoinHandler;
import io.github.lstramke.coincollector.handler.CollectionHandler;
import io.github.lstramke.coincollector.handler.GroupHandler;
import io.github.lstramke.coincollector.handler.LogoutHandler;
import io.github.lstramke.coincollector.handler.SessionFilter;

@EnableConfigurationProperties(DatabaseTableProperties.class)
@SpringBootApplication
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static HttpServer server;

    public static void main(String[] args) throws IOException {
        logger.info("✅ Starting CoinCollector...");

        var app = new SpringApplication(App.class);
        app.setHeadless(false);
        var ctx = app.run(args);
        var env = ctx.getEnvironment();
        final int PORT = Integer.parseInt(env.getProperty("coincollector.port"));

        var dbInitializer = ctx.getBean(SqliteInitializer.class);
        try {
            dbInitializer.init();
        } catch (StorageInitializeException e) {
            logger.error("Database initialization failed: {}", e.getMessage());
            System.exit(1);
            return;
        }

        var logoutHandler = ctx.getBean(LogoutHandler.class);
        var groupHandler = ctx.getBean(GroupHandler.class);
        var collectionHandler = ctx.getBean(CollectionHandler.class);
        var coinHandler = ctx.getBean(CoinHandler.class);
        var sessionFilter = ctx.getBean(SessionFilter.class);

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
                exchange.close();
                is.close();
            } else {
                String notFound = "404 - Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                exchange.getResponseBody().write(notFound.getBytes());
                exchange.close();
            }
        });
        

        server.createContext("/api/groups", sessionFilter.withSessionValidation(groupHandler));
        server.createContext("/api/collections", sessionFilter.withSessionValidation(collectionHandler));
        server.createContext("/api/coins", sessionFilter.withSessionValidation(coinHandler));
        server.createContext("/api/logout", sessionFilter.withSessionValidation(logoutHandler));

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
