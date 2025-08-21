package com.example.mapug;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class MapApplication extends Application {
    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        // Load the map.html file from resources
        String url = getClass().getResource("/map.html").toExternalForm();
        webEngine.load(url);

        Scene scene = new Scene(webView, 1000, 700);
        stage.setTitle("University of Ghana - Campus Map");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
