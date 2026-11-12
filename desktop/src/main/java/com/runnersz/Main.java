package com.runnersz;

import com.runnersz.controller.DesktopController;
import com.runnersz.controller.UiState;
import com.runnersz.utiles.I18n;
import com.runnersz.utiles.Log;
import com.runnersz.utiles.Logs;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Ciclo de vida de JavaFX. Carga la vista FXML, registra las tipografías empaquetadas y reconstruye
 * la escena cuando cambia el idioma, conservando lo que el usuario había escrito.
 *
 * @author Jairo y Alain
 */
public final class Main extends Application {
  private static final Log LOG = Log.of(Main.class);

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
    Path log = Logs.start();
    LOG.info(
        "Arranca la aplicación %s (%s %s, Java %s)",
        version(),
        System.getProperty("os.name"),
        System.getProperty("os.arch"),
        System.getProperty("java.version"));
    LOG.info("Registro en %s", log);
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
      installIcon(stage);
      if (state != null) next.applyState(state);
      stage.show();
      controller = next;
    } catch (IOException ex) {
      LOG.error("No se pudo cargar la vista", ex);
      throw new IllegalStateException("No se pudo cargar la vista", ex);
    }
  }

  /**
   * Icono de la ventana y de la barra de tareas: el PNG empaquetado en recursos. Se carga una sola
   * vez, aunque la vista se recree al cambiar de idioma.
   *
   * @param window ventana principal
   */
  private static void installIcon(Stage window) {
    if (!window.getIcons().isEmpty()) return;
    try (InputStream stream = Main.class.getResourceAsStream("/com/runnersz/icono/javafx.png")) {
      if (stream != null) window.getIcons().add(new Image(stream));
    } catch (IOException ex) {
      LOG.debug("No se pudo cargar el icono de la ventana: %s", ex);
    }
  }

  /** Registra las familias del contrato (§9.2) antes de crear la escena. */
  private static void loadFonts() {
    for (String name : FONTS) {
      try (InputStream stream = Main.class.getResourceAsStream("/com/runnersz/fonts/" + name)) {
        if (stream != null) Font.loadFont(stream, 13);
      } catch (IOException ex) {
        // La interfaz conserva una familia del sistema si una fuente no se puede leer.
        LOG.debug("No se pudo cargar la fuente %s: %s", name, ex);
      }
    }
  }

  @Override
  public void stop() {
    if (controller != null) controller.stop();
    LOG.info("La aplicación se cierra");
  }

  /** Versión del JAR empaquetado, o {@code desarrollo} cuando se ejecuta desde clases. */
  private static String version() {
    Package info = Main.class.getPackage();
    String version = info == null ? null : info.getImplementationVersion();
    return version == null || version.isBlank() ? "desarrollo" : version;
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
