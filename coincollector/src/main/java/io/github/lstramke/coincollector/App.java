package io.github.lstramke.coincollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import com.sun.net.httpserver.HttpServer;

import io.github.lstramke.coincollector.configuration.DataSourceAutoActivateForeignKeys;
import io.github.lstramke.coincollector.configuration.SqliteInitializer;
import io.github.lstramke.coincollector.configuration.StorageInitializer;
import io.github.lstramke.coincollector.exceptions.StorageInitializeException;
import io.github.lstramke.coincollector.handler.LoginHandler;
import io.github.lstramke.coincollector.handler.RegistrationHandler;
import io.github.lstramke.coincollector.model.UserFactory;
import io.github.lstramke.coincollector.repositories.UserStorageRepository;
import io.github.lstramke.coincollector.repositories.sqlite.UserSqliteRepository;
import io.github.lstramke.coincollector.services.UserStorageService;
import io.github.lstramke.coincollector.services.UserStorageServiceImpl;

import java.util.List;

import javax.sql.DataSource;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        log.info("✅ Starting CoinCollector...");

        // TODO: ADD FULL INIT SERVICE
        String dbFilePath = "coincollector.db";
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbFilePath);
        
        DataSource configuredDataSource = new DataSourceAutoActivateForeignKeys(dataSource);
        List<String> tableNames = List.of("users", "euroCoinCollectionGroups", "euroCoinCollections", "euroCoins");
        StorageInitializer storageInitializer = new SqliteInitializer(configuredDataSource, tableNames);
        
        try {
            storageInitializer.init();
        } catch (StorageInitializeException e) {
            log.error("Database initialization failed: {}", e.getMessage());
            System.exit(1);
        }

        UserFactory userFactory = new UserFactory();
        UserStorageRepository userStorageRepository = new UserSqliteRepository(tableNames.get(0), userFactory);
        UserStorageService userStorageService = new UserStorageServiceImpl(userStorageRepository, configuredDataSource);
        LoginHandler loginHandler = new LoginHandler(userStorageService);
        RegistrationHandler registrationHandler = new RegistrationHandler(userStorageService);
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

        server.createContext("/api/login", exchange -> {
            try {
                loginHandler.handle(exchange);
            } catch (IOException e) {
                String errorJson = "{\"error\":\"An unexpected error occurred\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorJson.length());
                exchange.getResponseBody().write(errorJson.getBytes());
                exchange.getResponseBody().close();
            }
        });

        server.createContext("/api/registration", exchange -> {
            try{
                registrationHandler.handle(exchange);
            } catch (IOException e) {
                String errorJson = "{\"error\":\"An unexpected error occurred\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorJson.length());
                exchange.getResponseBody().write(errorJson.getBytes());
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
