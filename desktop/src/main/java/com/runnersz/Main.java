package com.runnersz;

import com.runnersz.controller.DesktopController;
import com.runnersz.controller.UiState;
import com.runnersz.utiles.I18n;
import java.io.IOException;
import java.io.InputStream;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Ciclo de vida de JavaFX. Carga la vista FXML, registra las tipografías empaquetadas y reconstruye
 * la escena cuando cambia el idioma, conservando lo que el usuario había escrito.
 *
 * @author Jairo y Alain
 */
public final class Main extends Application {
  private static final String[] FONTS = {
    "Inter-Regular.ttf",
    "Inter-Medium.ttf",
    "Inter-SemiBold.ttf",
    "Inter-Bold.ttf",
    "Inter-ExtraBold.ttf",
    "OpenSans-Regular.ttf",
    "OpenSans-SemiBold.ttf",
    "OpenSans-Bold.ttf"
  };

  private Stage stage;
  private DesktopController controller;

  @Override
  public void start(Stage stage) {
    this.stage = stage;
    loadFonts();
    show(null);
  }

  private void show(UiState state) {
    try {
      FXMLLoader loader =
          new FXMLLoader(Main.class.getResource("/com/runnersz/desktop.fxml"), I18n.bundle());
      Parent root = loader.load();
      DesktopController next = loader.getController();
      next.attach(stage);
      next.setLanguageHandler(this::show);

      Scene scene = new Scene(root, 1040, 780);
      scene.getStylesheets().add(Main.class.getResource("/com/runnersz/estilo/runnersz.css").toExternalForm());
      stage.setScene(scene);
      stage.setMinWidth(860);
      stage.setMinHeight(660);
      stage.setTitle(I18n.text("app.title"));
      if (state != null) next.applyState(state);
      stage.show();
      controller = next;
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo cargar la vista", ex);
    }
  }

  /** Registra las familias del contrato (§9.2) antes de crear la escena. */
  private static void loadFonts() {
    for (String name : FONTS) {
      try (InputStream stream = Main.class.getResourceAsStream("/com/runnersz/fonts/" + name)) {
        if (stream != null) Font.loadFont(stream, 13);
      } catch (IOException ignored) {
        // La interfaz conserva una familia del sistema si una fuente no se puede leer.
      }
    }
  }

  @Override
  public void stop() {
    if (controller != null) controller.stop();
  }

  /**
   * Arranca la aplicación JavaFX.
   *
   * @param args argumentos de la línea de órdenes; no se interpretan
   */
  public static void main(String[] args) {
    launch(args);
  }
}
