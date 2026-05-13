package io.github.lstramke.coincollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import io.github.lstramke.coincollector.configuration.DatabaseTableProperties;
import io.github.lstramke.coincollector.configuration.SqliteInitializer;
import io.github.lstramke.coincollector.exceptions.StorageInitializeException;

@EnableConfigurationProperties(DatabaseTableProperties.class)
@SpringBootApplication
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static ApplicationContext context;

    public static void main(String[] args) throws IOException {
        logger.info("✅ Starting CoinCollector...");

        var app = new SpringApplication(App.class);
        app.setHeadless(false);
        context = app.run(args);
        var env = context.getEnvironment();
        final int PORT = Integer.parseInt(env.getProperty("coincollector.port"));

        //var dbInitializer = context.getBean(SqliteInitializer.class);
        //try {
        //    dbInitializer.init();
        //} catch (StorageInitializeException e) {
        //    logger.error("Database initialization failed: {}", e.getMessage());
        //    System.exit(1);
        //    return;
        //}
        
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
        if (context != null) {
            SpringApplication.exit(context);
            logger.info("Application shutdown");
        }
    }

}
