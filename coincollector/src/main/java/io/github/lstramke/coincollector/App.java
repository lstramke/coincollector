package io.github.lstramke.coincollector;

import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    @Override
    public void start(Stage stage) {

        log.info("✅ Logback Konfiguration funktioniert!");
        WebView webView = new WebView();
        java.net.URL resource = getClass().getResource("/frontend/index.html");
        if (resource != null) {
            String url = resource.toExternalForm();
            webView.getEngine().load(url);
        } else {
            webView.getEngine().loadContent("<html><body><h2>Frontend resource not found.</h2></body></html>");
        }

        stage.setScene(new Scene(webView, 800, 600));
        stage.setTitle("CoinCollector");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
