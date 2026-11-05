package com.runnersz.support;

import com.runnersz.utiles.I18n;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

/**
 * Herramienta de desarrollo: carga la vista real y guarda una captura de cada pestaña. No forma
 * parte de las pruebas automáticas; se ejecuta a mano para revisar el diseño.
 */
public final class UiPreview {
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

  private UiPreview() {}

  public static void main(String[] args) throws Exception {
    Platform.startup(() -> {});
    CountDownLatch done = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          try {
            render();
          } catch (Throwable ex) {
            System.err.println("FALLO: " + ex);
            ex.printStackTrace();
          } finally {
            done.countDown();
          }
        });
    done.await();
    Platform.exit();
  }

  private static void render() throws Exception {
    for (String name : FONTS) {
      try (InputStream stream = UiPreview.class.getResourceAsStream("/com/runnersz/fonts/" + name)) {
        if (stream != null) Font.loadFont(stream, 13);
      }
    }
    FXMLLoader loader =
        new FXMLLoader(UiPreview.class.getResource("/com/runnersz/desktop.fxml"), I18n.bundle());
    Parent root = loader.load();
    Scene scene = new Scene(root, 1040, 800);
    scene
        .getStylesheets()
        .add(UiPreview.class.getResource("/com/runnersz/estilo/runnersz.css").toExternalForm());

    Stage stage = new Stage();
    stage.setScene(scene);
    stage.show();
    TabPane tabs = (TabPane) scene.lookup("#tabs");
    for (int i = 0; i < 3; i++) {
      if (tabs != null) tabs.getSelectionModel().select(i);
      root.applyCss();
      root.layout();
      WritableImage image = scene.snapshot(null);
      save(image, "target/ui-" + (i + 1) + ".png");
    }
    stage.hide();
  }

  private static void save(WritableImage image, String path) throws Exception {
    int width = (int) image.getWidth();
    int height = (int) image.getHeight();
    int[] pixels = new int[width * height];
    image
        .getPixelReader()
        .getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
    BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    buffered.setRGB(0, 0, width, height, pixels, 0, width);
    File file = new File(path);
    if (file.getParentFile() != null) file.getParentFile().mkdirs();
    ImageIO.write(buffered, "png", file);
    System.out.println("captura: " + file.getAbsolutePath());
  }
}
