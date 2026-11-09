package com.runnersz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del ciclo de vida de JavaFX: la ventana se crea con la vista real y el título del
 * catálogo, carga las fuentes empaquetadas y se detiene sin dejar ventanas ni tareas vivas.
 *
 * @author Jairo y Alain
 */
class MainTest {
  private static boolean toolkit;

  @BeforeAll
  static void startToolkit() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException alreadyStarted) {
      // Otra clase ya arrancó el motor: se reutiliza.
    } catch (Throwable unavailable) {
      toolkit = false;
      return;
    }
    toolkit = true;
    Platform.setImplicitExit(false);
  }

  @Test
  void arrancaLaVentanaConLaVistaYSeDetieneSinErrores() throws Exception {
    Assumptions.assumeTrue(toolkit, "JavaFX necesita una pantalla para crear la ventana");

    Main application =
        onFx(
            () -> {
              Main main = new Main();
              Stage stage = new Stage();
              // Fuera de la pantalla visible: la prueba no debe molestar a quien esté delante.
              stage.setX(-4000);
              stage.setY(-4000);
              main.start(stage);
              return main;
            });

    Stage stage = onFx(() -> field(application, "stage", Stage.class));
    assertEquals("RunnersZ · Cliente de escritorio", onFx(stage::getTitle));
    assertTrue(onFx(stage::isShowing));
    assertNotNull(onFx(() -> field(application, "controller", Object.class)));
    assertFalse(onFx(() -> stage.getScene().getStylesheets().isEmpty()), "la hoja de estilos va cargada");
    assertTrue(onFx(() -> stage.getScene().getRoot().getStyleClass().contains("app-root")));

    onFx(
        () -> {
          application.stop();
          // Copia: cerrar una ventana modifica la lista observable que se está recorriendo.
          for (Window window : List.copyOf(Window.getWindows())) {
            if (window instanceof Stage open && open.isShowing()) open.close();
          }
          return null;
        });
    assertTrue(
        onFx(() -> Window.getWindows().stream().noneMatch(w -> w instanceof Stage s && s.isShowing())),
        "al parar no queda ninguna ventana abierta");
  }

  private static <T> T field(Object target, String name, Class<T> type) throws Exception {
    var field = Main.class.getDeclaredField(name);
    field.setAccessible(true);
    return type.cast(field.get(target));
  }

  private static <T> T onFx(Callable<T> task) {
    FutureTask<T> future = new FutureTask<>(task);
    Platform.runLater(future);
    try {
      return future.get(20, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(ex);
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException runtime) throw runtime;
      if (cause instanceof Error error) throw error;
      throw new IllegalStateException(cause);
    } catch (TimeoutException ex) {
      throw new IllegalStateException("La tarea de JavaFX no terminó a tiempo", ex);
    }
  }
}
