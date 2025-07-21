package io.github.lstramke.coincollector;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
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
