package com.runnersz;

import com.runnersz.controller.DesktopController;
import javafx.application.Application;
import javafx.stage.Stage;

/** Ciclo de vida de JavaFX; la ventana y sus eventos pertenecen al controlador. */
public final class Main extends Application {
  private final DesktopController controller = new DesktopController();

  @Override
  public void start(Stage stage) {
    controller.start(stage);
  }

  @Override
  public void stop() {
    controller.stop();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
