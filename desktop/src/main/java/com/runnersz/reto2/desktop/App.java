package com.runnersz.reto2.desktop;

import com.runnersz.reto2.desktop.controller.MainController;
import java.net.URL;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    private MainController controller;

    @Override
    public void start(Stage stage) {
        controller = new MainController();
        Scene scene = new Scene(controller.getView(), 960, 700);
        URL stylesheet = App.class.getResource("/styles.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        stage.setTitle("Reto2 RunnersZ · Cliente de escritorio");
        stage.setMinWidth(820);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> controller.shutdown());
        stage.show();
        controller.connectBackend();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }
}
