package com.runnersz.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runnersz.service.SettingsStore;
import com.runnersz.support.StubApi;
import com.runnersz.utiles.I18n;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas de la vista real: cargan {@code desktop.fxml} con el catálogo activo y ejercitan el
 * controlador. Los diálogos modales se cierran solos desde un vigilante en el hilo de JavaFX, así
 * que las pruebas no se bloquean ni necesitan interacción.
 *
 * <p>Sin pantalla disponible (por ejemplo un contenedor sin Xvfb) la clase se omite en lugar de
 * fallar.
 *
 * @author Jairo y Alain
 */
class DesktopControllerTest {
  private static final List<String> CLOSED = Collections.synchronizedList(new ArrayList<>());
  private static boolean toolkit;
  private static volatile boolean autoClose = true;
  private Parent root;
  private Scene scene;
  private DesktopController controller;

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
    Platform.runLater(DesktopControllerTest::watchDialogs);
  }

  @BeforeEach
  void loadView() throws Exception {
    Assumptions.assumeTrue(toolkit, "JavaFX necesita una pantalla para cargar la vista");
    Preferences.userRoot().node("/com/runnersz").clear();
    I18n.setLanguage("es");
    onFx(
        () -> {
          FXMLLoader loader =
              new FXMLLoader(
                  DesktopController.class.getResource("/com/runnersz/desktop.fxml"),
                  I18n.bundle());
          root = loader.load();
          scene = new Scene(root, 1040, 800);
          scene
              .getStylesheets()
              .add(
                  DesktopController.class
                      .getResource("/com/runnersz/estilo/runnersz.css")
                      .toExternalForm());
          root.applyCss();
          root.layout();
          controller = loader.getController();
          return null;
        });
  }

  @AfterEach
  void closeWindows() {
    autoClose = true;
    if (!toolkit) return;
    onFx(
        () -> {
          for (Window window : List.copyOf(Window.getWindows())) {
            if (window instanceof Stage stage && stage.isShowing()) stage.close();
          }
          return null;
        });
    if (controller != null) controller.stop();
  }

  @Test
  void cadaControlDeclaraSuRol() {
    Map<String, AccessibleRole> roles =
        Map.ofEntries(
            Map.entry("tabs", AccessibleRole.TAB_PANE),
            Map.entry("languageBox", AccessibleRole.COMBO_BOX),
            Map.entry("shiftSlider", AccessibleRole.SLIDER),
            Map.entry("showPassword", AccessibleRole.TOGGLE_BUTTON),
            Map.entry("progressBar", AccessibleRole.PROGRESS_INDICATOR),
            Map.entry("statusLabel", AccessibleRole.TEXT),
            Map.entry("caesarShiftValue", AccessibleRole.TEXT),
            Map.entry("inputPath", AccessibleRole.TEXT_FIELD),
            Map.entry("passwordField", AccessibleRole.PASSWORD_FIELD),
            Map.entry("passwordPlain", AccessibleRole.TEXT_FIELD),
            Map.entry("keyField", AccessibleRole.TEXT_FIELD),
            Map.entry("plainText", AccessibleRole.TEXT_AREA),
            Map.entry("vigenereResult", AccessibleRole.TEXT_AREA),
            Map.entry("caesarText", AccessibleRole.TEXT_AREA),
            Map.entry("caesarResult", AccessibleRole.TEXT_AREA),
            Map.entry("filesScroll", AccessibleRole.SCROLL_PANE),
            Map.entry("vigenereScroll", AccessibleRole.SCROLL_PANE),
            Map.entry("caesarScroll", AccessibleRole.SCROLL_PANE));
    roles.forEach(
        (id, expected) -> {
          Control control = (Control) root.lookup("#" + id);
          assertNotNull(control, id);
          assertEquals(expected, control.getAccessibleRole(), id);
        });
  }

  @Test
  void cadaBotonTieneRolNombreAyuda() {
    List<String> ids =
        List.of(
            "encryptButton",
            "decryptButton",
            "browseInput",
            "browseOutput",
            "settingsButton",
            "helpButton",
            "openTextButton",
            "saveTextButton",
            "copyTextButton",
            "swapTextButton",
            "vigenereEncryptButton",
            "vigenereDecryptButton",
            "caesarEncryptButton",
            "caesarDecryptButton",
            "caesarOpenButton",
            "caesarSaveButton",
            "caesarCopyButton",
            "cancelButton");
    for (String id : ids) {
      Button button = (Button) root.lookup("#" + id);
      assertNotNull(button, id);
      assertEquals(AccessibleRole.BUTTON, button.getAccessibleRole(), id);
      assertNotNull(button.getAccessibleText(), id);
      assertFalse(button.getAccessibleText().isBlank(), id);
      assertFalse(button.getAccessibleText().contains("_"), id);
      assertNotNull(button.getTooltip(), id);
    }
  }

  @Test
  void snapshotYApplyStateConservanElFormulario() {
    onFx(
        () -> {
          ((TextField) root.lookup("#inputPath")).setText("/tmp/a.txt");
          ((TextField) root.lookup("#outputPath")).setText("/tmp/a.txt.enc");
          ((PasswordField) root.lookup("#passwordField")).setText("secreta");
          ((TextArea) root.lookup("#plainText")).setText("HOLA");
          ((TextField) root.lookup("#keyField")).setText("CLAVE");
          ((TextArea) root.lookup("#vigenereResult")).setText("KHOOR");
          ((TextArea) root.lookup("#caesarText")).setText("ABC");
          ((TextArea) root.lookup("#caesarResult")).setText("BCD");
          ((Slider) root.lookup("#shiftSlider")).setValue(9);
          ((TabPane) root.lookup("#tabs")).getSelectionModel().select(2);
          return null;
        });
    UiState state = onFx(controller::snapshot);
    assertEquals("/tmp/a.txt", state.inputPath());
    assertEquals("/tmp/a.txt.enc", state.outputPath());
    assertEquals("secreta", state.password());
    assertEquals("HOLA", state.text());
    assertEquals("CLAVE", state.key());
    assertEquals("KHOOR", state.result());
    assertEquals("ABC", state.caesarText());
    assertEquals("BCD", state.caesarResult());
    assertEquals(9, state.shift());
    assertEquals(2, state.tab());

    onFx(
        () -> {
          for (String id : List.of("inputPath", "outputPath", "keyField")) {
            ((TextField) root.lookup("#" + id)).clear();
          }
          for (String id : List.of("plainText", "vigenereResult", "caesarText", "caesarResult")) {
            ((TextArea) root.lookup("#" + id)).clear();
          }
          ((PasswordField) root.lookup("#passwordField")).clear();
          ((Slider) root.lookup("#shiftSlider")).setValue(1);
          ((TabPane) root.lookup("#tabs")).getSelectionModel().select(0);
          controller.applyState(state);
          return null;
        });
    assertEquals("/tmp/a.txt", text("#inputPath"));
    assertEquals("KHOOR", text("#vigenereResult"));
    assertEquals("9", text("#caesarShiftValue"));
    assertEquals(2, onFx(() -> ((TabPane) root.lookup("#tabs")).getSelectionModel().getSelectedIndex()));
  }

  @Test
  void aplicarEstadoRecortaLaPestanaFueraDeRango() {
    onFx(
        () -> {
          controller.applyState(new UiState("a", "b", "c", "d", "e", "f", "g", "h", 99, 9));
          return null;
        });
    assertEquals(2, onFx(() -> ((TabPane) root.lookup("#tabs")).getSelectionModel().getSelectedIndex()));
    assertEquals(25, onFx(controller::snapshot).shift());
  }

  @Test
  void elegirEntradaProponeElDestinoYAnunciaElTamano(@TempDir Path dir) throws Exception {
    Path input = Files.writeString(dir.resolve("nota.txt"), "hola", StandardCharsets.UTF_8);
    selectInput(input.toFile());
    assertEquals(input.toAbsolutePath().toString(), text("#inputPath"));
    assertEquals("nota.txt.enc", Path.of(text("#outputPath")).getFileName().toString());
    assertEquals("nota.txt · 4 B", text("#statusLabel"));
  }

  @Test
  void unFicheroCifradoProponeElNombreRecuperado(@TempDir Path dir) throws Exception {
    Path enc = Files.writeString(dir.resolve("nota.txt.enc"), "x", StandardCharsets.UTF_8);
    selectInput(enc.toFile());
    assertEquals("nota.txt.recuperado", Path.of(text("#outputPath")).getFileName().toString());
  }

  @Test
  void unFicheroInexistenteNoCambiaLaSeleccion(@TempDir Path dir) {
    selectInput(dir.resolve("fantasma.txt").toFile());
    assertEquals("", text("#inputPath"));
  }

  @Test
  void elDesplazamientoSeRedondeaYSeAnuncia() {
    onFx(
        () -> {
          ((Slider) root.lookup("#shiftSlider")).setValue(7.4);
          return null;
        });
    assertEquals(7, onFx(controller::snapshot).shift());
    assertEquals("7", text("#caesarShiftValue"));
    String spoken = onFx(() -> ((Control) root.lookup("#caesarShiftValue")).getAccessibleText());
    assertTrue(spoken.startsWith("Desplazamiento: "), spoken);
    assertTrue(spoken.endsWith(": 7"), spoken);

    onFx(
        () -> {
          ((Slider) root.lookup("#shiftSlider")).setValue(25);
          return null;
        });
    assertEquals("25", text("#caesarShiftValue"));
  }

  @Test
  void mostrarLaContrasenaAlternaLosDosCampos() {
    onFx(
        () -> {
          ((ToggleButton) root.lookup("#showPassword")).setSelected(true);
          return null;
        });
    assertTrue(onFx(() -> root.lookup("#passwordPlain").isVisible()));
    assertFalse(onFx(() -> root.lookup("#passwordField").isVisible()));
    String label = onFx(() -> ((ToggleButton) root.lookup("#showPassword")).getText());
    assertTrue(label == null || label.isEmpty(), "el ojo no lleva texto, solo el icono");
    assertEquals(
        "Ocultar",
        onFx(() -> ((ToggleButton) root.lookup("#showPassword")).getAccessibleText()),
        "el nombre accesible anuncia el estado");

    onFx(
        () -> {
          ((TextField) root.lookup("#passwordPlain")).setText("clave");
          return null;
        });
    assertEquals("clave", onFx(controller::snapshot).password());

    onFx(
        () -> {
          ((ToggleButton) root.lookup("#showPassword")).setSelected(false);
          return null;
        });
    assertTrue(onFx(() -> root.lookup("#passwordField").isVisible()));
    assertEquals("clave", onFx(controller::snapshot).password());

    // La clave de Vigenère se escribe enmascarada y el ojo la enseña sin perder el texto.
    assertTrue(onFx(() -> root.lookup("#keyMasked").isVisible()), "la clave arranca enmascarada");
    assertFalse(onFx(() -> root.lookup("#keyField").isVisible()));
    onFx(
        () -> {
          ((PasswordField) root.lookup("#keyMasked")).setText("CLAVE");
          return null;
        });
    assertEquals("CLAVE", text("#keyField"), "los dos campos comparten el texto");
    onFx(
        () -> {
          ((ToggleButton) root.lookup("#showKey")).setSelected(true);
          return null;
        });
    assertTrue(onFx(() -> root.lookup("#keyField").isVisible()));
    assertFalse(onFx(() -> root.lookup("#keyMasked").isVisible()));
    assertEquals("CLAVE", onFx(controller::snapshot).key());
  }

  @Test
  void losContadoresSiguenAlTextoEscrito() {
    onFx(
        () -> {
          ((TextArea) root.lookup("#plainText")).setText("Hola");
          return null;
        });
    assertEquals("4 caracteres", text("#textCounter"));
    onFx(
        () -> {
          ((TextArea) root.lookup("#caesarText")).setText("ABCDE");
          return null;
        });
    assertEquals("5 caracteres", text("#caesarCounter"));
  }

  @Test
  void elResultadoSeCopiaYSeIntercambia() {
    onFx(
        () -> {
          ((TextArea) root.lookup("#vigenereResult")).setText("KHOOR");
          return null;
        });
    assertTrue(fireAndClose("copyTextButton").isEmpty());
    String copied;
    try {
      copied = onFx(() -> Clipboard.getSystemClipboard().getString());
    } catch (RuntimeException unavailable) {
      copied = null;
    }
    Assumptions.assumeTrue(copied != null, "El portapapeles no está disponible sin pantalla");
    assertEquals("KHOOR", copied);

    assertTrue(fireAndClose("swapTextButton").isEmpty());
    assertEquals("KHOOR", text("#plainText"));
    assertEquals("", text("#vigenereResult"));
    assertTrue(onFx(() -> root.lookup("#toast").isVisible()));
  }

  @Test
  void sinDatosSeAvisaYNoSeLanzaLaOperacion() {
    assertEquals(List.of("Falta el fichero"), fireAndClose("encryptButton"));
    assertEquals(List.of("Falta el fichero"), fireAndClose("decryptButton"));
    assertTrue(fireAndClose("cancelButton").isEmpty());

    onFx(
        () -> {
          ((TextField) root.lookup("#keyField")).setText("123");
          return null;
        });
    assertEquals(List.of("Falta el texto"), fireAndClose("vigenereEncryptButton"));

    onFx(
        () -> {
          ((TextArea) root.lookup("#plainText")).setText("HOLA");
          return null;
        });
    assertEquals(List.of("Clave no válida"), fireAndClose("vigenereEncryptButton"));
    assertEquals(List.of("Falta el texto"), fireAndClose("caesarDecryptButton"));
    assertEquals(List.of("Sin resultado"), fireAndClose("copyTextButton"));
    assertEquals(List.of("Sin resultado"), fireAndClose("swapTextButton"));
    assertEquals(List.of("Sin resultado"), fireAndClose("saveTextButton"));
  }

  @Test
  void sinConfiguracionPideLosAjustesAntesDeOperar(@TempDir Path dir) throws Exception {
    Path input = Files.writeString(dir.resolve("nota.txt"), "hola", StandardCharsets.UTF_8);
    onFx(
        () -> {
          ((TextField) root.lookup("#inputPath")).setText(input.toString());
          ((PasswordField) root.lookup("#passwordField")).setText("clave");
          ((TextField) root.lookup("#outputPath")).setText(dir.resolve("salida.enc").toString());
          return null;
        });
    assertEquals(
        List.of("Configuración pendiente", "Ajustes de conexión"),
        fireAndClose("encryptButton"),
        "primero el aviso y después los ajustes");
  }

  @Test
  @SuppressWarnings("unchecked")
  void cambiarDeIdiomaAvisaAlRecargarLaVista() {
    AtomicInteger calls = new AtomicInteger();
    controller.setLanguageHandler(state -> calls.incrementAndGet());
    try {
      onFx(
          () -> {
            ((ComboBox<String>) root.lookup("#languageBox")).setValue("English");
            return null;
          });
      waitUntil(() -> calls.get() > 0);
      assertEquals("en", I18n.locale().getLanguage());
      assertTrue(calls.get() > 0);
    } finally {
      I18n.setLanguage("es");
    }
  }

  @Test
  void losTamanosSeMuestranEnLaUnidadAdecuada() throws Exception {
    assertEquals("0 B", DesktopController.formatSize(0));
    assertEquals("1 B", DesktopController.formatSize(1));
    assertEquals("1023 B", DesktopController.formatSize(1023));
    assertEquals("1.0 KiB", DesktopController.formatSize(1024));
    assertEquals("1.0 MiB", DesktopController.formatSize(1024 * 1024));
    assertEquals("20.0 MiB", DesktopController.formatSize(20L * 1024 * 1024));

    Method detail = DesktopController.class.getDeclaredMethod("detail", long.class, long.class);
    detail.setAccessible(true);
    assertEquals("1.0 KiB / 20.0 MiB", detail.invoke(null, 1024L, 20L * 1024 * 1024));
    assertEquals("4 B", detail.invoke(null, 4L, 0L));
  }

  @Test
  void cifrarUnFicheroDesdeLaInterfazPublicaElResultado(@TempDir Path dir) throws Exception {
    Path input = Files.writeString(dir.resolve("nota.txt"), "hola", StandardCharsets.UTF_8);
    Path output = dir.resolve("nota.txt.enc");
    try (StubApi api = new StubApi()) {
      api.respond(200, "cifrado".getBytes(StandardCharsets.UTF_8));
      configure(api.baseUrl());
      fillFileForm(input, output, "clave");
      assertTrue(fireAndClose("encryptButton").isEmpty(), "el aviso llega al terminar la operación");

      waitUntil(() -> Files.exists(output));
      assertTrue(Files.exists(output), "el resultado se publica en el destino");
      assertEquals("cifrado", Files.readString(output));
      assertEquals("/api/v1/files/encrypt", api.last().path());
      assertTrue(api.last().authorization().contains("token-de-prueba"));

      waitUntil(() -> text("#passwordField").isEmpty());
      assertEquals("", text("#passwordField"), "la contraseña se borra al terminar");
      assertTrue(onFx(() -> root.lookup("#toast").isVisible()));
      waitUntil(() -> CLOSED.contains("Guarda la contraseña"));
      assertTrue(CLOSED.contains("Guarda la contraseña"), "avisa de que la contraseña se borra");
    }
  }

  @Test
  void alCifrarUnFicheroAvisaYOfreceCopiarLaContrasena(@TempDir Path dir) throws Exception {
    Path input = Files.writeString(dir.resolve("nota.txt"), "hola", StandardCharsets.UTF_8);
    Path output = dir.resolve("nota.txt.enc");
    try (StubApi api = new StubApi()) {
      api.respond(200, "cifrado".getBytes(StandardCharsets.UTF_8));
      configure(api.baseUrl());
      fillFileForm(input, output, "clave");
      autoClose = false;
      fireLater("encryptButton");
      waitUntil(this::dialogShowing);

      assertNull(onFx(() -> dialogPane().getHeaderText()), "el aviso no lleva cabecera");
      assertEquals(
          "La clave se borrará de la aplicación al cerrar este aviso.",
          onFx(() -> texts(dialogPane().getContent())).trim(),
          "el aviso solo muestra el texto del borrado");
      clickDialogButton("Copiar contraseña");
      waitUntil(() -> !dialogShowing());

      String copied;
      try {
        copied = onFx(() -> Clipboard.getSystemClipboard().getString());
      } catch (RuntimeException unavailable) {
        copied = null;
      }
      Assumptions.assumeTrue(copied != null, "El portapapeles no está disponible sin pantalla");
      assertEquals("clave", copied, "el aviso copia la contraseña antes de borrarla");
      assertEquals("", text("#passwordField"), "la contraseña desaparece del formulario");
    } finally {
      autoClose = true;
    }
  }

  @Test
  void alCifrarConVigenereAvisaDeQueLaClaveSeBorra() throws Exception {
    try (StubApi api = new StubApi()) {
      api.respond(200, "{\"result\":\"KHOOR\"}".getBytes(StandardCharsets.UTF_8));
      configure(api.baseUrl());
      onFx(
          () -> {
            ((TextArea) root.lookup("#plainText")).setText("HOLA");
            ((TextField) root.lookup("#keyField")).setText("CLAVE");
            return null;
          });
      autoClose = false;
      fireLater("vigenereEncryptButton");
      waitUntil(this::dialogShowing);

      assertNull(onFx(() -> dialogPane().getHeaderText()), "el aviso no lleva cabecera");
      assertEquals(
          "La clave se borrará de la aplicación al cerrar este aviso.",
          onFx(() -> texts(dialogPane().getContent())).trim(),
          "el aviso solo muestra el texto del borrado");
      clickDialogButton("Cerrar");
      waitUntil(() -> !dialogShowing());

      assertEquals("KHOOR", text("#vigenereResult"));
      assertEquals("", text("#keyField"), "la clave desaparece del formulario");
    } finally {
      autoClose = true;
    }
  }

  @Test
  void descifrarSinIntegridadNoPublicaElResultado(@TempDir Path dir) throws Exception {
    Path input = Files.writeString(dir.resolve("nota.txt.enc"), "datos", StandardCharsets.UTF_8);
    Path output = dir.resolve("nota.txt.recuperado");
    try (StubApi api = new StubApi()) {
      api.respond(200, "texto".getBytes(StandardCharsets.UTF_8));
      configure(api.baseUrl());
      fillFileForm(input, output, "clave");
      fireAsync("decryptButton");

      waitUntil(() -> !CLOSED.isEmpty());
      assertEquals(
          List.of("No se pudo completar la operación"), List.copyOf(CLOSED), "aviso de integridad");
      assertFalse(Files.exists(output), "sin X-Integrity no se publica nada");
      assertTrue(text("#statusLabel").contains("no se completó"));
    }
  }

  @Test
  void guardarAjustesDesdeElDialogoVerificaLaConexion() throws Exception {
    try (StubApi api = new StubApi()) {
      api.respond(204, "");
      autoClose = false;
      fireLater("settingsButton");
      waitUntil(this::dialogShowing);
      fillSettings(api.baseUrl(), "token-de-prueba");

      clickDialogButton("Probar");
      waitUntil(
          () ->
              "Conexión y autenticación correctas. Ajustes guardados."
                  .equals(dialogMessage()));
      assertTrue(api.last().path().endsWith("/auth/check"));
      assertTrue(api.last().authorization().contains("token-de-prueba"));

      clickDialogButton("Guardar");
      waitUntil(() -> !dialogShowing());
      assertEquals("Configurado", text("#connectionChip"));
      assertEquals("Ajustes guardados localmente.", text("#statusLabel"));
    } finally {
      autoClose = true;
    }
  }

  @Test
  void unTokenRechazadoMarcaSinConexion() throws Exception {
    try (StubApi api = new StubApi()) {
      api.respond(401, "");
      autoClose = false;
      fireLater("settingsButton");
      waitUntil(this::dialogShowing);
      fillSettings(api.baseUrl(), "token-de-prueba");

      clickDialogButton("Probar");
      waitUntil(
          () -> {
            String message = dialogMessage();
            return message != null && message.contains("token");
          });
      assertEquals("Sin conexión", text("#connectionChip"));

      clickDialogButton("Cancelar");
      waitUntil(() -> !dialogShowing());
      assertEquals("Sin conexión", text("#connectionChip"), "cerrar sin guardar no configura nada");
    } finally {
      autoClose = true;
    }
  }

  @Test
  void cancelarElReemplazoNoTocaElDestinoNiLlamaALaApi(@TempDir Path dir) throws Exception {
    Path input = Files.writeString(dir.resolve("nota.txt"), "hola", StandardCharsets.UTF_8);
    Path output = Files.writeString(dir.resolve("nota.txt.enc"), "anterior", StandardCharsets.UTF_8);
    try (StubApi api = new StubApi()) {
      api.respond(200, "nuevo");
      configure(api.baseUrl());
      fillFileForm(input, output, "clave");
      autoClose = false;
      fireLater("encryptButton");
      waitUntil(this::dialogShowing);

      clickDialogButton("Cancelar");
      waitUntil(() -> !dialogShowing());
      assertEquals("anterior", Files.readString(output));
      assertNull(api.last(), "el reemplazo cancelado no llega al servidor");
    } finally {
      autoClose = true;
    }
  }

  @Test
  void elMenuDeEdicionEstaTraducidoYRespetaLaSeleccion() {
    onFx(
        () -> {
          TextArea area = (TextArea) root.lookup("#plainText");
          area.setText("HOLA MUNDO");
          area.selectAll();
          ContextMenu menu = area.getContextMenu();
          assertNotNull(menu);
          menu.getOnShowing().handle(null);
          assertEquals("Cortar", menu.getItems().get(2).getText());
          assertFalse(menu.getItems().get(2).isDisable(), "hay selección: se puede cortar");
          assertEquals("Pegar", menu.getItems().get(4).getText());
          assertEquals("Seleccionar todo", menu.getItems().get(6).getText());
          menu.getItems().get(5).fire();
          return null;
        });
    assertEquals("", text("#plainText"));
  }

  @Test
  void unaRutaInvalidaSeDescarta() throws Exception {
    Method pathOrNull = DesktopController.class.getDeclaredMethod("pathOrNull", String.class);
    pathOrNull.setAccessible(true);
    assertNull(pathOrNull.invoke(null, " "));
    assertNull(pathOrNull.invoke(null, (Object) null));
    assertNull(pathOrNull.invoke(null, "a\u0000b"));
    assertEquals(Path.of("x"), pathOrNull.invoke(null, " x "));
  }

  @Test
  void losMensajesSoloAdmitenLetrasCifrasYEspacios() {
    for (String id : List.of("plainText", "caesarText")) {
      assertNotNull(
          onFx(() -> ((TextArea) root.lookup("#" + id)).getTextFormatter()),
          id + " lleva el filtro de entrada instalado");
    }
    assertEquals("Hola Mundo 42", DesktopController.onlyText("Ho#la Mu@ndo 4$2"));
    assertEquals("Año nuevo\nlinea 2", DesktopController.onlyText("Año nuevo\nlinea 2"));
    assertEquals("", DesktopController.onlyText("¡¿@#$%&*()[]{}<>/\\|~"));
    assertEquals("", DesktopController.onlyText(null));
  }

  @Test
  void elFiltroRechazaLosSignosAlTeclear() {
    for (String id : List.of("plainText", "caesarText")) {
      String typed =
          onFx(
              () -> {
                TextArea area = (TextArea) root.lookup("#" + id);
                area.requestFocus();
                area.clear();
                Event.fireEvent(area, keyTyped("a"));
                Event.fireEvent(area, keyTyped("#"));
                return area.getText();
              });
      assertEquals("a", typed, id + ": la tecla del signo no debe llegar al texto");
    }
  }

  @Test
  void elVisorDelRegistroSeAbreConSuRuta() {
    autoClose = true;
    CLOSED.clear();
    onFx(
        () -> {
          controller.showLogs();
          return null;
        });
    assertTrue(CLOSED.contains("Registro de la aplicación"), CLOSED.toString());
  }

  @Test
  void elBotonDeAyudaExplicaLaAplicacionYLosAtajos() {
    autoClose = false;
    try {
      fireLater("helpButton");
      waitUntil(this::dialogShowing);
      assertEquals("Ayuda y atajos de teclado", onFx(() -> dialogPane().getHeaderText()));
      String content = onFx(() -> texts(dialogPane().getContent()));
      assertTrue(content.contains("AES"), content);
      assertTrue(content.contains("Ctrl+1"), content);
      assertTrue(content.contains("Ctrl+Intro"), content);
      assertTrue(content.contains("F1"), content);
      clickDialogButton("Cerrar");
      waitUntil(() -> !dialogShowing());
    } finally {
      autoClose = true;
    }
  }

  @Test
  void losAtajosCambianDePestanaYAvisanSinDatos() {
    onFx(
        () -> {
          controller.installShortcuts(scene);
          return null;
        });
    onFx(
        () -> {
          press(KeyCode.DIGIT3, KeyCombination.SHORTCUT_DOWN);
          return null;
        });
    assertEquals(2, tabIndex());

    CLOSED.clear();
    onFx(
        () -> {
          press(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN);
          return null;
        });
    waitUntil(() -> CLOSED.contains("Falta el texto"));
    assertEquals(List.of("Falta el texto"), List.copyOf(CLOSED), "Ctrl+Intro cifra César");

    CLOSED.clear();
    onFx(
        () -> {
          press(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN);
          press(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN);
          return null;
        });
    waitUntil(() -> CLOSED.contains("Falta el fichero"));
    assertEquals(List.of("Falta el fichero"), List.copyOf(CLOSED), "Ctrl+Intro cifra Ficheros");

    CLOSED.clear();
    onFx(
        () -> {
          press(KeyCode.DIGIT3, KeyCombination.SHORTCUT_DOWN);
          press(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
          return null;
        });
    waitUntil(() -> CLOSED.contains("Falta el texto"));
    assertEquals(List.of("Falta el texto"), List.copyOf(CLOSED), "Ctrl+Mayús+Intro descifra");
  }

  @Test
  void ningunDialogoRecortaElTextoDeSusBotones() {
    autoClose = false;
    try {
      checkDialog("helpButton", "Cerrar");
      checkDialog("settingsButton", "Cancelar");
      checkDialog("encryptButton", "Cerrar");
      onFx(
          () -> {
            Platform.runLater(controller::showLogs);
            return null;
          });
      waitUntil(this::dialogShowing);
      assertButtonsWhole();
      clickDialogButton("Cerrar");
      waitUntil(() -> !dialogShowing());
    } finally {
      autoClose = true;
    }
  }

  @Test
  void elBotonDeAyudaEsUnCirculo() {
    onFx(
        () -> {
          Button help = (Button) root.lookup("#helpButton");
          double radius =
              help.getBackground().getFills().get(0).getRadii().getTopLeftHorizontalRadius();
          assertEquals(34, help.getWidth(), 0.01, "ocupa lo mismo que un botón de la cabecera");
          assertEquals(help.getWidth() / 2, radius, 0.01, "el radio cubre medio botón: círculo");
          return null;
        });
  }

  @Test
  void laTipografiaUsaEmSobreUnaBaseDe16Px() {
    assertEquals(16.0, onFx(() -> font("#statusLabel")), 0.01, "cuerpo: 1 em = 16 px = 12 pt");
    assertEquals(16.0, onFx(() -> font("#plainText")), 0.01, "campos: 1 em");
    assertEquals(16.0, onFx(() -> font("#encryptButton")), 0.01, "botones: 1 em");
    assertEquals(30.0, onFx(() -> font("#brandName")), 0.01, "marca: 1,875 em");
    assertEquals(27.0, onFx(() -> font("#filesTitle")), 0.01, "títulos: 1,6875 em ≈ 20,25 pt");
    assertEquals(19.0, onFx(() -> font("#vigenereResult")), 0.01, "resultados: 1,1875 em ≈ 14,25 pt");
  }

  private double font(String id) {
    javafx.scene.Node node = root.lookup(id);
    if (node instanceof javafx.scene.control.Labeled labelled) {
      return labelled.getFont().getSize();
    }
    return ((javafx.scene.control.TextInputControl) node).getFont().getSize();
  }

  // ---------------------------------------------------------------- utilidades

  private String text(String id) {
    return onFx(
        () -> {
          Control control = (Control) root.lookup(id.startsWith("#") ? id : "#" + id);
          if (control instanceof Label label) return label.getText();
          if (control instanceof TextInputControl input) return input.getText();
          return null;
        });
  }

  private void fillFileForm(Path input, Path output, String password) {
    onFx(
        () -> {
          ((TextField) root.lookup("#inputPath")).setText(input.toString());
          ((TextField) root.lookup("#outputPath")).setText(output.toString());
          ((PasswordField) root.lookup("#passwordField")).setText(password);
          return null;
        });
  }

  /** Guarda la configuración en el controlador sin pasar por el diálogo de Ajustes. */
  private void configure(String baseUrl) {
    onFx(
        () -> {
          Field field = DesktopController.class.getDeclaredField("settings");
          field.setAccessible(true);
          ((SettingsStore) field.get(controller)).update(baseUrl, "token-de-prueba");
          return null;
        });
  }

  /** Pulsa el botón y vuelve enseguida: la operación continúa en segundo plano. */
  private void fireAsync(String buttonId) {
    CLOSED.clear();
    onFx(
        () -> {
          ((Button) root.lookup("#" + buttonId)).fire();
          return null;
        });
  }

  /** Pulsa el botón sin esperar: el manejador abre un diálogo modal y se queda en él. */
  private void fireLater(String buttonId) {
    CLOSED.clear();
    Platform.runLater(() -> ((Button) root.lookup("#" + buttonId)).fire());
  }

  private boolean dialogShowing() {
    return onFx(() -> dialogPane() != null);
  }

  private DialogPane dialogPane() {
    for (Window window : List.copyOf(Window.getWindows())) {
      if (window instanceof Stage stage && stage.isShowing() && stage.getScene() != null) {
        Parent content = stage.getScene().getRoot();
        if (content instanceof DialogPane pane) return pane;
      }
    }
    return null;
  }

  private void clickDialogButton(String label) {
    onFx(
        () -> {
          DialogPane pane = dialogPane();
          if (pane == null) throw new IllegalStateException("No hay ningún diálogo abierto");
          for (ButtonType type : pane.getButtonTypes()) {
            Button button = (Button) pane.lookupButton(type);
            if (button != null && label.equals(button.getText())) {
              button.fire();
              return null;
            }
          }
          throw new IllegalStateException("El diálogo no tiene el botón «" + label + "»");
        });
  }

  /** Escribe la dirección y el token en el diálogo de Ajustes que esté abierto. */
  private void fillSettings(String baseUrl, String token) {
    onFx(
        () -> {
          DialogPane pane = dialogPane();
          if (pane == null || !(pane.getContent() instanceof VBox form)) return null;
          ((TextField) form.getChildren().get(1)).setText(baseUrl);
          ((PasswordField) form.getChildren().get(3)).setText(token);
          return null;
        });
  }

  /** Texto del mensaje del diálogo de Ajustes (último hijo del formulario). */
  private String dialogMessage() {
    return onFx(
        () -> {
          DialogPane pane = dialogPane();
          if (pane == null || !(pane.getContent() instanceof VBox form)) return null;
          List<Node> nodes = form.getChildren();
          if (nodes.isEmpty()) return null;
          return nodes.get(nodes.size() - 1) instanceof Label label ? label.getText() : null;
        });
  }

  private static KeyEvent keyTyped(String character) {
    return new KeyEvent(
        KeyEvent.KEY_TYPED, character, "", KeyCode.UNDEFINED, false, false, false, false);
  }

  private void selectInput(File file) {
    onFx(
        () -> {
          Method method = DesktopController.class.getDeclaredMethod("selectInput", File.class);
          method.setAccessible(true);
          try {
            method.invoke(controller, file);
            return null;
          } catch (InvocationTargetException ex) {
            throw new IllegalStateException(ex.getCause());
          }
        });
  }

  /** Ejecuta el atajo tal como lo haría JavaFX al pulsar la combinación. */
  private void press(KeyCode code, KeyCombination.Modifier... modifiers) {
    Runnable action = scene.getAccelerators().get(new KeyCodeCombination(code, modifiers));
    assertNotNull(action, "atajo registrado: " + code);
    action.run();
  }

  private int tabIndex() {
    return onFx(() -> ((TabPane) root.lookup("#tabs")).getSelectionModel().getSelectedIndex());
  }

  /** Texto de todas las etiquetas del contenedor, para revisar lo que muestra un diálogo. */
  private static String texts(Node node) {
    StringBuilder all = new StringBuilder();
    if (node instanceof javafx.scene.control.Labeled labelled && labelled.getText() != null) {
      all.append(labelled.getText()).append('\n');
    }
    if (node instanceof Parent parent) {
      for (Node child : parent.getChildrenUnmodifiable()) all.append(texts(child));
    }
    return all.toString();
  }

  /** Abre el diálogo de un botón, revisa que sus botones se lean enteros y lo cierra. */
  private void checkDialog(String buttonId, String closeLabel) {
    fireLater(buttonId);
    waitUntil(this::dialogShowing);
    assertButtonsWhole();
    clickDialogButton(closeLabel);
    waitUntil(() -> !dialogShowing());
  }

  /** Ningún botón del diálogo abierto puede medir menos que su ancho preferido: se vería «Cer…». */
  private void assertButtonsWhole() {
    onFx(
        () -> {
          DialogPane pane = dialogPane();
          assertNotNull(pane, "hay un diálogo abierto");
          pane.applyCss();
          pane.layout();
          for (ButtonType type : pane.getButtonTypes()) {
            Button button = (Button) pane.lookupButton(type);
            assertNotNull(button, type.getText());
            assertTrue(
                button.getWidth() + 0.5 >= button.prefWidth(-1),
                "«"
                    + button.getText()
                    + "» se recorta: ancho "
                    + button.getWidth()
                    + " < "
                    + button.prefWidth(-1));
          }
          return null;
        });
  }

  /**
   * Pulsa el botón y devuelve los títulos de los diálogos modales que se han cerrado. El vigilante
   * es único y permanente, así que ninguna prueba hereda diálogos de otra.
   */
  private List<String> fireAndClose(String buttonId) {
    CLOSED.clear();
    onFx(
        () -> {
          ((Button) root.lookup("#" + buttonId)).fire();
          return null;
        });
    return List.copyOf(CLOSED);
  }

  /** Cierra cualquier ventana modal y anota su título; se reprograma en cada ciclo de JavaFX. */
  private static void watchDialogs() {
    if (autoClose) {
      for (Window window : List.copyOf(Window.getWindows())) {
        // Solo los diálogos (raíz DialogPane): la ventana de otra prueba no se toca.
        if (window instanceof Stage stage
            && stage.isShowing()
            && stage.getScene() != null
            && stage.getScene().getRoot() instanceof DialogPane) {
          CLOSED.add(stage.getTitle() == null ? "" : stage.getTitle());
          stage.close();
        }
      }
    }
    Platform.runLater(DesktopControllerTest::watchDialogs);
  }

  private static void waitUntil(BooleanSupplier condition) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (System.nanoTime() < deadline) {
      if (condition.getAsBoolean()) return;
      try {
        Thread.sleep(25);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return;
      }
    }
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
