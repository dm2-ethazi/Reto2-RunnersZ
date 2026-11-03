package com.runnersz.controller;

import com.runnersz.utiles.I18n;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/** Regresión ejecutable sin JUnit: cambia idiomas repetidamente sin mostrar ventanas. */
public final class LanguageSwitchRegression {
  private static int checks;

  public static void main(String[] args) throws Exception {
    // Debe establecerse antes de cargar I18n o SettingsStore: nunca toca preferencias del usuario.
    System.setProperty("java.util.prefs.PreferencesFactory", MemoryFactory.class.getName());
    CountDownLatch finished = new CountDownLatch(1);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Platform.startup(() -> {
      try {
        exerciseSelector();
      } catch (Throwable error) {
        failure.set(error);
      } finally {
        finished.countDown();
      }
    });
    try {
      if (!finished.await(30, TimeUnit.SECONDS)) throw new AssertionError("Tiempo agotado");
      if (failure.get() != null) throw new AssertionError("Regresión de idioma", failure.get());
      System.out.println("PASS: " + checks + " comprobaciones; sin ventanas ni preferencias reales");
    } finally {
      Platform.exit();
    }
  }

  private static void exerciseSelector() throws Exception {
    I18n.setLanguage("es");
    DesktopController controller = new DesktopController();
    try {
      List<Node> nodes = new ArrayList<>();
      for (String view : List.of("header", "fileView", "vigenereView", "footer")) {
        collect((Node) invoke(controller, view, new Class<?>[0]), nodes);
      }
      ComboBox<?> selector = (ComboBox<?>) nodes.stream()
          .filter(node -> node instanceof ComboBox<?>).findFirst().orElseThrow();
      TextArea input = (TextArea) field(controller, "plainText");
      TextArea result = (TextArea) field(controller, "resultText");
      PasswordField password = (PasswordField) field(controller, "aesPassword");
      TextField inputPath = (TextField) field(controller, "inputPath");
      TextField outputPath = (TextField) field(controller, "outputPath");
      input.setText("Kaixo, hello, ¡hola!");
      result.setText("User content");
      password.setText("test-only-password");
      inputPath.setText("input.dat");
      outputPath.setText("output.enc");
      Tab tab = (Tab) invoke(controller, "tab", new Class<?>[] {String.class, Node.class},
          "tab.files", new Label());
      DialogPane pane = new DialogPane();
      pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
      invoke(controller, "translateButtons", new Class<?>[] {DialogPane.class}, pane);

      for (int round = 0; round < 4; round++) {
        for (String code : List.of("eu", "en", "es")) {
          String displayName = switch (code) {
            case "eu" -> "Euskara";
            case "en" -> "English";
            default -> "Español";
          };
          selector.getSelectionModel().select(selector.getItems().indexOf(displayName));
          selector.fireEvent(new ActionEvent());
          // No leer languageCode: hacerlo aquí ocultaría precisamente el fallo de invalidación.
          check(I18n.locale().getLanguage().equals(code), "idioma seleccionado");
          check(((Button) field(controller, "encryptButton")).getText()
              .equals(I18n.text("files.encrypt")), "botón AES: " + code);
          check(tab.getText().equals(I18n.text("tab.files")), "pestaña: " + code);
          check(inputPath.getPromptText().equals(I18n.text("files.input.prompt")), "placeholder");
          check(inputPath.getAccessibleText().equals(I18n.text("files.input")), "nombre accesible");
          check(inputPath.getAccessibleHelp().equals(I18n.text("files.input.help")), "ayuda accesible");
          check(((Button) pane.lookupButton(ButtonType.CANCEL)).getText()
              .equals(I18n.text("cancel")), "cancelar");
          check(((Button) pane.lookupButton(ButtonType.OK)).getText()
              .equals(I18n.text("ok")), "aceptar");
          check(input.getContextMenu().getItems().get(3).getText()
              .equals(I18n.text("edit.copy")), "menú de edición");
          check(input.getText().equals("Kaixo, hello, ¡hola!")
              && result.getText().equals("User content"), "conserva mensajes");
          check(password.getText().equals("test-only-password")
              && inputPath.getText().equals("input.dat")
              && outputPath.getText().equals("output.enc"), "conserva formulario");
          check(Preferences.userRoot().node("/org/example").get("language", "").equals(code),
              "idioma guardado");
        }
      }
    } finally {
      controller.stop();
    }
  }

  private static Object field(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static Object invoke(Object target, String name, Class<?>[] types, Object... args)
      throws Exception {
    Method method = target.getClass().getDeclaredMethod(name, types);
    method.setAccessible(true);
    return method.invoke(target, args);
  }

  private static void collect(Node node, List<Node> nodes) {
    nodes.add(node);
    if (node instanceof ScrollPane scroll) collect(scroll.getContent(), nodes);
    else if (node instanceof Parent parent)
      for (Node child : parent.getChildrenUnmodifiable()) collect(child, nodes);
  }

  private static void check(boolean condition, String name) {
    if (!condition) throw new AssertionError(name);
    checks++;
  }

  /** Almacén efímero utilizado exclusivamente por esta prueba en su propio proceso Java. */
  public static final class MemoryFactory implements PreferencesFactory {
    private final Preferences root = new MemoryPreferences(null, "");
    @Override public Preferences userRoot() { return root; }
    @Override public Preferences systemRoot() { return root; }
  }

  private static final class MemoryPreferences extends AbstractPreferences {
    private final Map<String, String> values = new HashMap<>();
    private MemoryPreferences(AbstractPreferences parent, String name) { super(parent, name); }
    @Override protected void putSpi(String key, String value) { values.put(key, value); }
    @Override protected String getSpi(String key) { return values.get(key); }
    @Override protected void removeSpi(String key) { values.remove(key); }
    @Override protected void removeNodeSpi() { values.clear(); }
    @Override protected String[] keysSpi() { return values.keySet().toArray(String[]::new); }
    @Override protected String[] childrenNamesSpi() { return new String[0]; }
    @Override protected AbstractPreferences childSpi(String name) {
      return new MemoryPreferences(this, name);
    }
    @Override protected void syncSpi() { }
    @Override protected void flushSpi() { }
  }
}
