package com.runnersz.controller;

import static com.runnersz.utiles.I18n.text;

import com.runnersz.utiles.I18n;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import com.runnersz.service.ApiClient;
import com.runnersz.service.FileOperationService;
import com.runnersz.service.FileService;
import com.runnersz.service.SettingsStore;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controlador de la ventana: construye los controles y traduce eventos en operaciones. Las
 * peticiones HTTP y el acceso a disco se delegan en servicios; aquí solo se actualizan controles
 * desde el hilo de JavaFX.
 */
public final class DesktopController {
  private final FileService files = new FileService();
  private final SettingsStore settings = new SettingsStore();
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread thread = new Thread(r, "runnersz-network-worker");
            thread.setDaemon(true);
            return thread;
          });
  private final StringProperty languageCode = new SimpleStringProperty(I18n.locale().getLanguage());
  private Stage stage;
  private Label status;
  private ProgressIndicator progress;
  private Button encryptButton, decryptButton, vigenereEncryptButton, vigenereDecryptButton;
  private TextField inputPath, outputPath;
  private PasswordField aesPassword;
  private TextArea plainText, resultText;
  private File selectedInput;

  /** Monta la ventana y enlaza las acciones del usuario. */
  public void start(Stage stage) {
    this.stage = stage;
    stage.titleProperty().bind(words("app.title"));
    BorderPane root = new BorderPane();
    root.getStyleClass().add("app-root");
    root.setTop(header());
    TabPane tabs = new TabPane();
    tabs.getStyleClass().add("main-tabs");
    tabs.getTabs()
        .addAll(
            tab("tab.files", fileView()), tab("tab.messages", vigenereView()));
    tabs.getTabs().forEach(tab -> tab.setClosable(false));
    root.setCenter(tabs);
    root.setBottom(footer());
    Scene scene = new Scene(root, 980, 720);
    scene
        .getStylesheets()
        .add(getClass().getResource("/com/runnersz/estilo/runnersz.css").toExternalForm());
    stage.setMinWidth(760);
    stage.setMinHeight(620);
    stage.setScene(scene);
    stage.show();
  }

  private HBox header() {
    Label brand = new Label("RUNNERSZ");
    brand.getStyleClass().add("brand");
    Label subtitle = label("brand.subtitle");
    subtitle.getStyleClass().add("eyebrow");
    subtitle.setWrapText(true);
    Button config = button("settings.open");
    ComboBox<String> language = new ComboBox<>();
    // Los nombres propios de idioma se mantienen reconocibles en los tres catálogos.
    language.getItems().addAll("Español", "Euskara", "English");
    language.setValue(switch (I18n.locale().getLanguage()) {
      case "eu" -> "Euskara";
      case "en" -> "English";
      default -> "Español";
    });
    language.accessibleTextProperty().bind(words("language"));
    language.setOnAction(e -> {
      String selected = language.getValue();
      String code = selected.equals("Euskara") ? "eu" : selected.equals("English") ? "en" : "es";
      I18n.setLanguage(code);
      languageCode.set(code);
    });
    accessible(config, AccessibleRole.BUTTON, "settings.open.help");
    config.setOnAction(e -> showSettings());
    VBox identity = new VBox(4, brand, subtitle);
    HBox.setHgrow(identity, Priority.ALWAYS);
    HBox bar = new HBox(16, identity, language, config);
    bar.setAlignment(Pos.CENTER_LEFT);
    bar.setPadding(new Insets(24, 32, 20, 32));
    bar.getStyleClass().add("header");
    return bar;
  }

  private VBox fileView() {
    Label title = label("files.title");
    title.getStyleClass().add("section-title");
    Label intro = label("files.intro");
    intro.getStyleClass().add("muted");
    inputPath = new TextField();
    accessible(inputPath, AccessibleRole.TEXT_FIELD, "files.input");
    inputPath.promptTextProperty().bind(words("files.input.prompt"));
    inputPath.accessibleHelpProperty().bind(words("files.input.help"));
    inputPath.setEditable(false);
    Button browseInput = button("files.choose");
    accessible(browseInput, AccessibleRole.BUTTON, "files.choose.help");
    browseInput.setOnAction(e -> chooseInput());
    HBox inputRow = new HBox(10, inputPath, browseInput);
    HBox.setHgrow(inputPath, Priority.ALWAYS);
    outputPath = new TextField();
    accessible(outputPath, AccessibleRole.TEXT_FIELD, "files.output");
    outputPath.promptTextProperty().bind(words("files.output.prompt"));
    outputPath.accessibleHelpProperty().bind(words("files.output.help"));
    outputPath.setEditable(false);
    Button browseOutput = button("files.saveAs");
    accessible(browseOutput, AccessibleRole.BUTTON, "files.saveAs.help");
    browseOutput.setOnAction(e -> chooseOutput());
    HBox outputRow = new HBox(10, outputPath, browseOutput);
    HBox.setHgrow(outputPath, Priority.ALWAYS);
    aesPassword = new PasswordField();
    accessible(aesPassword, AccessibleRole.PASSWORD_FIELD, "files.password");
    aesPassword.promptTextProperty().bind(words("files.password"));
    aesPassword.accessibleHelpProperty().bind(words("files.password.help"));
    Label note =
        label("files.note");
    note.getStyleClass().add("helper");
    note.setWrapText(true);
    VBox card =
        new VBox(
            14,
            fieldLabel("files.input"),
            inputRow,
            fieldLabel("files.output"),
            outputRow,
            fieldLabel("password"),
            aesPassword,
            note);
    card.getStyleClass().add("card");
    encryptButton = button("files.encrypt");
    accessible(encryptButton, AccessibleRole.BUTTON, "files.encrypt.help");
    encryptButton.setMinWidth(150);
    encryptButton.setOnAction(e -> runFileOperation(true));
    encryptButton.getStyleClass().add("primary-button");
    decryptButton = button("files.decrypt");
    accessible(decryptButton, AccessibleRole.BUTTON, "files.decrypt.help");
    decryptButton.setMinWidth(160);
    decryptButton.setOnAction(e -> runFileOperation(false));
    HBox actions = new HBox(12, encryptButton, decryptButton);
    actions.setAlignment(Pos.CENTER_LEFT);
    VBox content = new VBox(18, title, intro, card, actions);
    content.setPadding(new Insets(28, 36, 28, 36));
    content.getStyleClass().add("content");
    return content;
  }

  private VBox vigenereView() {
    Label title = label("vigenere.title");
    title.getStyleClass().add("section-title");
    Label intro =
        label("vigenere.intro");
    intro.getStyleClass().add("muted");
    intro.setWrapText(true);
    plainText = new TextArea();
    accessible(plainText, AccessibleRole.TEXT_AREA, "text.input");
    plainText.promptTextProperty().bind(words("text.prompt"));
    plainText.setWrapText(true);
    plainText.setPrefRowCount(8);
    plainText.accessibleHelpProperty().bind(words("text.help"));
    resultText = new TextArea();
    accessible(resultText, AccessibleRole.TEXT_AREA, "vigenere.result");
    resultText.promptTextProperty().bind(words("result.prompt"));
    resultText.setWrapText(true);
    resultText.setPrefRowCount(8);
    resultText.setEditable(false);
    TextField key = new TextField();
    accessible(key, AccessibleRole.TEXT_FIELD, "vigenere.key");
    key.promptTextProperty().bind(words("vigenere.key.prompt"));
    key.accessibleHelpProperty().bind(words("vigenere.key.help"));
    Button load = button("text.open");
    accessible(load, AccessibleRole.BUTTON, "text.open.help");
    load.setOnAction(e -> loadText());
    Button save = button("text.save");
    accessible(save, AccessibleRole.BUTTON, "text.save.help");
    save.setOnAction(e -> saveText());
    vigenereEncryptButton = button("encrypt");
    accessible(vigenereEncryptButton, AccessibleRole.BUTTON, "vigenere.encrypt.help");
    vigenereEncryptButton.setMinWidth(110);
    vigenereEncryptButton.getStyleClass().add("primary-button");
    vigenereEncryptButton.setOnAction(e -> runVigenere(key.getText(), "encrypt"));
    vigenereDecryptButton = button("decrypt");
    accessible(vigenereDecryptButton, AccessibleRole.BUTTON, "vigenere.decrypt.help");
    vigenereDecryptButton.setMinWidth(110);
    vigenereDecryptButton.setOnAction(e -> runVigenere(key.getText(), "decrypt"));
    VBox card =
        new VBox(
            12,
            fieldLabel("text.input"),
            plainText,
            new HBox(10, load),
            fieldLabel("key"),
            key,
            new HBox(10, vigenereEncryptButton, vigenereDecryptButton),
            fieldLabel("result"),
            resultText,
            save);
    card.getStyleClass().add("card");
    VBox content = new VBox(18, title, intro, card);
    content.setPadding(new Insets(28, 36, 28, 36));
    content.getStyleClass().add("content");
    ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    scroll.getStyleClass().add("content-scroll");
    return new VBox(scroll);
  }

  private HBox footer() {
    status = new Label();
    setStatus("status.initial");
    status.getStyleClass().add("status-text");
    progress = new ProgressIndicator();
    progress.setMaxSize(20, 20);
    progress.setVisible(false);
    progress.setAccessibleRole(AccessibleRole.PROGRESS_INDICATOR);
    progress.accessibleTextProperty().bind(words("status.busy"));
    HBox bar = new HBox(10, progress, status);
    bar.setAlignment(Pos.CENTER_LEFT);
    bar.setPadding(new Insets(12, 32, 16, 32));
    bar.getStyleClass().add("footer");
    return bar;
  }

  /** Cada propiedad conserva su clave: es posible cambiar de idioma sin perder el formulario. */
  private StringBinding words(String key, Object... arguments) {
    return Bindings.createStringBinding(
        () -> {
          // La lectura revalida la propiedad de JavaFX para recibir los siguientes cambios.
          // Consultar solo I18n.locale() dejaría de notificar después del primer cambio.
          languageCode.get();
          return text(key, arguments);
        },
        languageCode);
  }

  private Label label(String key) {
    Label label = new Label();
    label.textProperty().bind(words(key));
    return label;
  }

  private Button button(String key) {
    Button button = new Button();
    button.textProperty().bind(words(key));
    return button;
  }

  private Tab tab(String key, javafx.scene.Node content) {
    Tab tab = new Tab();
    tab.textProperty().bind(words(key));
    tab.setContent(content);
    return tab;
  }

  private void translateButtons(DialogPane pane) {
    for (ButtonType type : pane.getButtonTypes()) {
      String key = type == ButtonType.CANCEL ? "cancel" : type == ButtonType.OK ? "ok" : null;
      if (key != null) ((Button) pane.lookupButton(type)).textProperty().bind(words(key));
    }
  }

  private Label fieldLabel(String text) {
    Label label = label(text);
    label.getStyleClass().add("field-label");
    return label;
  }

  private void accessible(Control control, AccessibleRole role, String name) {
    control.setAccessibleRole(role);
    control.accessibleTextProperty().bind(words(name));
    if (control instanceof TextInputControl input) installEditMenu(input);
  }

  /** El menú JavaFX predeterminado usa el idioma del entorno; este sigue el selector. */
  private void installEditMenu(TextInputControl input) {
    ContextMenu menu = new ContextMenu();
    String[] keys = {"edit.undo", "edit.redo", "edit.cut", "edit.copy", "edit.paste",
        "edit.delete", "edit.selectAll"};
    Runnable[] actions = {input::undo, input::redo, input::cut, input::copy, input::paste,
        () -> input.replaceSelection(""), input::selectAll};
    for (int i = 0; i < keys.length; i++) {
      MenuItem item = new MenuItem();
      item.textProperty().bind(words(keys[i]));
      Runnable action = actions[i];
      int index = i;
      item.setOnAction(e -> action.run());
      switch (i) {
        case 0 -> item.disableProperty().bind(input.undoableProperty().not().or(input.editableProperty().not()));
        case 1 -> item.disableProperty().bind(input.redoableProperty().not().or(input.editableProperty().not()));
        case 2, 3 -> item.disableProperty().bind(Bindings.createBooleanBinding(
            () -> input instanceof PasswordField || input.getSelectedText().isEmpty()
                || (index == 2 && !input.isEditable()),
            input.selectedTextProperty(), input.editableProperty()));
        case 4, 5 -> item.disableProperty().bind(input.editableProperty().not());
        default -> { }
      }
      menu.getItems().add(item);
    }
    input.setContextMenu(menu);
  }

  private void chooseInput() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(text("chooser.input"));
    File file = chooser.showOpenDialog(stage);
    if (file == null) return;
    selectedInput = file;
    inputPath.setText(file.getAbsolutePath());
    String name = file.getName();
    String defaultName =
        name.endsWith(".enc")
            ? name.substring(0, name.length() - 4) + text("file.recovered")
            : name + ".enc";
    outputPath.setText(file.toPath().resolveSibling(defaultName).toString());
  }

  private void chooseOutput() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(text("chooser.output"));
    String existing = outputPath.getText();
    if (!existing.isBlank()) {
      Path p = Path.of(existing);
      if (p.getParent() != null && Files.isDirectory(p.getParent()))
        chooser.setInitialDirectory(p.getParent().toFile());
      chooser.setInitialFileName(p.getFileName().toString());
    }
    File file = chooser.showSaveDialog(stage);
    if (file != null) outputPath.setText(file.getAbsolutePath());
  }

  /** Valida la selección y solicita permiso antes de reemplazar una salida existente. */
  private void runFileOperation(boolean encrypt) {
    String password = aesPassword.getText();
    String output = outputPath.getText().trim();
    if (selectedInput == null || !selectedInput.isFile()) {
      alert(Alert.AlertType.WARNING, text("missing.file"), text("missing.file.help"));
      return;
    }
    if (password.isBlank()) {
      alert(Alert.AlertType.WARNING, text("missing.password"), text("missing.password.help"));
      aesPassword.requestFocus();
      return;
    }
    if (output.isBlank()) {
      alert(Alert.AlertType.WARNING, text("missing.output"), text("missing.output.help"));
      return;
    }
    Path in = selectedInput.toPath().toAbsolutePath().normalize(),
        out = Path.of(output).toAbsolutePath().normalize();
    if (in.equals(out)) {
      alert(
          Alert.AlertType.WARNING,
          text("invalid.output"),
          text("error.original"));
      return;
    }
    if (Files.exists(out) && !confirmOverwrite(out)) return;
    ApiClient api = new ApiClient(settings.snapshot());
    beginOperation(
        encrypt ? "status.encryptFile" : "status.decryptFile",
        () -> {
          // El servicio valida la integridad antes de publicar el resultado en disco.
          new FileOperationService(api, files).execute(in, out, password, encrypt);
          return out;
        },
        value -> alert(Alert.AlertType.INFORMATION, text("operation.completed"),
            text(encrypt ? "file.encrypted" : "file.decrypted", value)));
  }

  private void runVigenere(String key, String mode) {
    String text = plainText.getText();
    if (text.isEmpty()) {
      alert(
          Alert.AlertType.WARNING, text("missing.text"), text("missing.text.help"));
      plainText.requestFocus();
      return;
    }
    if (key == null || !key.matches("[A-Za-z]+")) {
      alert(Alert.AlertType.WARNING, text("invalid.key"), text("error.INVALID_KEY"));
      return;
    }
    ApiClient api = new ApiClient(settings.snapshot());
    beginOperation(
        mode.equals("encrypt") ? "status.encryptText" : "status.decryptText",
        () -> api.vigenere(text, key, mode),
        value -> resultText.setText(value));
  }

  private void loadText() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(text("chooser.textOpen"));
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(text("chooser.textFilter"), "*.txt"));
    File file = chooser.showOpenDialog(stage);
    if (file == null) return;
    try {
      plainText.setText(files.readText(file.toPath()));
      setStatus("text.loaded", file.getName());
    } catch (Exception ex) {
      showError(text("error.read.title"), ex);
    }
  }

  private void saveText() {
    if (resultText.getText().isEmpty()) {
      alert(Alert.AlertType.INFORMATION, text("missing.result"), text("missing.result.help"));
      return;
    }
    FileChooser chooser = new FileChooser();
    chooser.setTitle(text("chooser.textSave"));
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(text("chooser.textFilter"), "*.txt"));
    chooser.setInitialFileName(text("chooser.resultName"));
    File file = chooser.showSaveDialog(stage);
    if (file == null) return;
    if (Files.exists(file.toPath()) && !confirmOverwrite(file.toPath())) return;
    try {
      files.writeText(file.toPath(), resultText.getText());
      setStatus("text.saved", file.getName());
    } catch (Exception ex) {
      showError(text("error.save.title"), ex);
    }
  }

  /** Probar guarda tras autenticar; Guardar permite conservar los ajustes sin prueba HTTP. */
  private void showSettings() {
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.initOwner(stage);
    dialog.setTitle(text("settings.title"));
    dialog.setHeaderText(text("settings.header"));
    TextField base = new TextField(settings.baseUrl());
    accessible(base, AccessibleRole.TEXT_FIELD, "settings.server");
    base.promptTextProperty().bind(words("settings.server.prompt"));
    base.accessibleHelpProperty().bind(words("settings.server.help"));
    PasswordField token = new PasswordField();
    accessible(token, AccessibleRole.PASSWORD_FIELD, "settings.token");
    token.setText(settings.token());
    token.promptTextProperty().bind(words("settings.token"));
    token.accessibleHelpProperty().bind(words("settings.token.help"));
    VBox form =
        new VBox(10, fieldLabel("settings.server"), base, fieldLabel("settings.token"), token);
    form.setPadding(new Insets(12));
    dialog.getDialogPane().setContent(form);
    ButtonType test = new ButtonType(text("settings.test"), ButtonBar.ButtonData.LEFT);
    ButtonType save = new ButtonType(text("save"), ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(test, save, ButtonType.CANCEL);
    translateButtons(dialog.getDialogPane());
    dialog
        .getDialogPane()
        .lookupButton(test)
        .addEventFilter(
            javafx.event.ActionEvent.ACTION,
            event -> {
              event.consume();
              try {
                SettingsStore candidate = new SettingsStore(base.getText().trim(), token.getText());
                dialog.close();
                beginOperation(
                    "status.connection",
                    () -> {
                      new ApiClient(candidate.snapshot()).checkAuth();
                      return text("settings.verified");
                    },
                    value -> {
                      settings.update(base.getText(), token.getText());
                      alert(Alert.AlertType.INFORMATION, text("server.available"), text("settings.verified"));
                    });
              } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, text("settings.invalid"), I18n.errorMessage(ex));
              }
            });
    Optional<ButtonType> result = dialog.showAndWait();
    if (result.isPresent() && result.get() == save) {
      try {
        settings.update(base.getText().trim(), token.getText());
        setStatus("settings.saved");
      } catch (Exception ex) {
        showError(text("settings.error.title"), ex);
      }
    }
  }

  /** Trabajo que puede fallar: Task recoge el error y lo devuelve al hilo gráfico. */
  @FunctionalInterface
  private interface Work<T> {
    T run() throws Exception;
  }

  /** Ejecuta red/disco en segundo plano; los callbacks de Task se ejecutan en JavaFX. */
  private <T> void beginOperation(
      String message,
      Work<T> work,
      java.util.function.Consumer<T> onSuccess) {
    setBusy(true, message);
    Task<T> task =
        new Task<>() {
          @Override
          protected T call() throws Exception {
            return work.run();
          }
        };
    task.setOnSucceeded(
        e -> {
          setBusy(false, "status.ready");
          onSuccess.accept(task.getValue());
        });
    task.setOnFailed(
        e -> {
          setBusy(false, "status.failed");
          showError(text("error.operation.title"), task.getException());
        });
    worker.submit(task);
  }

  private boolean confirmOverwrite(Path path) {
    Alert alert =
        new Alert(
            Alert.AlertType.CONFIRMATION,
            text("confirm.body", path),
            ButtonType.CANCEL,
            ButtonType.OK);
    alert.initOwner(stage);
    alert.setTitle(text("confirm.title"));
    alert.setHeaderText(text("confirm.header"));
    translateButtons(alert.getDialogPane());
    return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
  }

  private void setBusy(boolean busy, String message) {
    progress.setVisible(busy);
    setStatus(message);
    encryptButton.setDisable(busy);
    decryptButton.setDisable(busy);
    vigenereEncryptButton.setDisable(busy);
    vigenereDecryptButton.setDisable(busy);
  }

  private void setStatus(String key, Object... arguments) {
    status.textProperty().bind(words(key, arguments));
  }

  private void alert(Alert.AlertType type, String title, String message) {
    Alert alert = new Alert(type);
    alert.initOwner(stage);
    alert.setTitle(title);
    alert.setHeaderText(title);
    Label content =
        new Label(
            message == null || message.isBlank() ? text("error.unknown") : message);
    content.setWrapText(true);
    content.setMaxWidth(560);
    alert.getDialogPane().setContent(content);
    alert.getDialogPane().setMinWidth(620);
    alert.getDialogPane().setPrefWidth(620);
    ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setMinWidth(100);
    translateButtons(alert.getDialogPane());
    alert.showAndWait();
  }

  private void showError(String title, Throwable error) {
    alert(Alert.AlertType.ERROR, title, I18n.errorMessage(error));
  }

  /** Detiene las tareas pendientes cuando JavaFX cierra la aplicación. */
  public void stop() {
    worker.shutdownNow();
  }
}
