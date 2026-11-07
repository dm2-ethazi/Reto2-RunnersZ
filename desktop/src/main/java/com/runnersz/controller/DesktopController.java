package com.runnersz.controller;

import static com.runnersz.utiles.I18n.text;
import org.kordamp.ikonli.javafx.FontIcon;

import com.runnersz.model.ConnectionSettings;
import com.runnersz.service.ApiClient;
import com.runnersz.service.FileOperationService;
import com.runnersz.service.FileService;
import com.runnersz.service.SettingsStore;
import com.runnersz.utiles.I18n;
import com.runnersz.utiles.Log;
import com.runnersz.utiles.Logs;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controlador de la ventana declarada en {@code desktop.fxml}. Traduce eventos en operaciones y
 * actualiza los controles desde el hilo de JavaFX; la red y el disco viven en servicios.
 *
 * @author Jairo y Alain
 */
public final class DesktopController {
  private static final String MIB = "%.1f MiB";
  private static final String KIB = "%.1f KiB";
  private static final Log LOG = Log.of(DesktopController.class);

  private final FileService files = new FileService();
  private final SettingsStore settings = new SettingsStore();
  private final ExecutorService worker =
      Executors.newCachedThreadPool(
          runnable -> {
            Thread thread = new Thread(runnable, "runnersz-worker");
            thread.setDaemon(true);
            return thread;
          });

  private Stage stage;
  private Consumer<UiState> languageHandler;
  private Task<?> running;
  private Runnable lastAction;
  private Path lastDirectory;
  private PauseTransition toastTimer;

  @FXML private TabPane tabs;
  @FXML private Label connectionChip;
  @FXML private ComboBox<String> languageBox;
  @FXML private Button settingsButton;

  @FXML private VBox fileCard;
  @FXML private TextField inputPath;
  @FXML private Button browseInput;
  @FXML private TextField outputPath;
  @FXML private Button browseOutput;
  @FXML private PasswordField passwordField;
  @FXML private TextField passwordPlain;
  @FXML private ToggleButton showPassword;
  @FXML private Button encryptButton;
  @FXML private Button decryptButton;

  @FXML private TextArea plainText;
  @FXML private TextField keyField;
  @FXML private Button vigenereEncryptButton;
  @FXML private Button vigenereDecryptButton;
  @FXML private TextArea vigenereResult;
  @FXML private Button openTextButton;
  @FXML private Button saveTextButton;
  @FXML private Button copyTextButton;
  @FXML private Button swapTextButton;
  @FXML private Label textCounter;
  @FXML private Label dropHint;
  @FXML private ScrollPane filesScroll;
  @FXML private ScrollPane vigenereScroll;
  @FXML private ScrollPane caesarScroll;
  @FXML private Label brandName;
  @FXML private Label brandSubtitle;
  @FXML private Label filesTitle;
  @FXML private Label filesIntro;
  @FXML private Label inputLabel;
  @FXML private Label outputLabel;
  @FXML private Label passwordLabel;
  @FXML private Label filesNote;
  @FXML private Label vigenereTitle;
  @FXML private Label vigenereIntro;
  @FXML private Label textInputLabel;
  @FXML private Label keyLabel;
  @FXML private Label vigenereResultLabel;
  @FXML private Label caesarTitle;
  @FXML private Label caesarIntro;
  @FXML private Label caesarInputLabel;
  @FXML private Label caesarShiftLabel;
  @FXML private Label caesarResultLabel;

  @FXML private TextArea caesarText;
  @FXML private Slider shiftSlider;
  @FXML private Label caesarShiftValue;
  @FXML private Button caesarEncryptButton;
  @FXML private Button caesarDecryptButton;
  @FXML private TextArea caesarResult;
  @FXML private Button caesarOpenButton;
  @FXML private Button caesarSaveButton;
  @FXML private Button caesarCopyButton;
  @FXML private Label caesarCounter;

  @FXML private Label toast;
  @FXML private Label statusLabel;
  @FXML private ProgressBar progressBar;
  @FXML private Label progressLabel;
  @FXML private Button cancelButton;

  /**
   * Enlaza la ventana y los servicios que dependen de ella.
   *
   * @param stage ventana principal que usan los diálogos y los selectores de archivos
   */
  public void attach(Stage stage) {
    this.stage = stage;
  }

  /**
   * Registra la acción que recarga la vista al cambiar de idioma.
   *
   * @param handler consumidor que recibe el estado del formulario ya relleno
   */

  public void setLanguageHandler(Consumer<UiState> handler) {
    this.languageHandler = handler;
  }

  @FXML
  private void initialize() {
    shiftSlider.valueProperty().addListener((observable, previous, value) -> updateShift());
    updateShift();
    icon(encryptButton, "fas-lock");
    icon(decryptButton, "fas-lock-open");
    icon(vigenereEncryptButton, "fas-lock");
    icon(vigenereDecryptButton, "fas-lock-open");
    icon(caesarEncryptButton, "fas-lock");
    icon(caesarDecryptButton, "fas-lock-open");
    icon(settingsButton, "fas-cog");
    languageBox.getItems().setAll("Español", "Euskara", "English");
    languageBox.setValue(switch (I18n.locale().getLanguage()) {
      case "eu" -> "Euskara";
      case "en" -> "English";
      default -> "Español";
    });
    languageBox.setAccessibleText(text("language"));
    languageBox.valueProperty().addListener((observable, previous, value) -> changeLanguage(value));

    accessible(inputPath, "files.input");
    accessible(outputPath, "files.output");
    accessible(passwordField, "files.password");
    accessible(passwordPlain, "files.password");
    accessible(keyField, "vigenere.key");
    accessible(plainText, "text.input");
    accessible(vigenereResult, "vigenere.result");
    accessible(caesarText, "caesar.text");
    accessible(caesarResult, "caesar.result");
    accessible(shiftSlider, "caesar.shift");

    passwordPlain.textProperty().bindBidirectional(passwordField.textProperty());
    showPassword.selectedProperty().addListener((observable, previous, visible) -> {
      passwordPlain.setVisible(visible);
      passwordPlain.setManaged(visible);
      passwordField.setVisible(!visible);
      passwordField.setManaged(!visible);
      (visible ? passwordPlain : passwordField).requestFocus();
      showPassword.setText(text(visible ? "password.hide" : "password.show"));
      showPassword.setAccessibleText(text(visible ? "password.hide" : "password.show"));
    });
    showPassword.setText(text("password.show"));

    plainText.textProperty().addListener((observable, previous, value) -> updateCounter(value, textCounter));
    caesarText.textProperty().addListener((observable, previous, value) -> updateCounter(value, caesarCounter));
    updateCounter("", textCounter);
    updateCounter("", caesarCounter);
    restrictToText(plainText);
    restrictToText(caesarText);

    installAccessibility();
    installTooltips();
    configureDropTarget();
    progressBar.setVisible(false);
    progressBar.setManaged(false);
    progressLabel.setVisible(false);
    progressLabel.setManaged(false);
    cancelButton.setVisible(false);
    cancelButton.setManaged(false);
    toast.setVisible(false);
    toast.setManaged(false);

    refreshConnectionChip();
    setStatus(settings.isConfigured() ? "status.ready" : "status.initial");
  }

  /**
   * Restaura el formulario cuando cambia el idioma y se recarga la vista.
   *
   * @param state valores que había escrito el usuario, o {@code null} en el primer arranque
   */
  /**
   * Añade la ayuda emergente a cada control: se muestra al pasar el ratón y al enfocar con el
   * teclado. Se reinstala al recargar la vista, así que sigue el idioma activo.
   */
  private void installTooltips() {
    tip(connectionChip, "chip.help");
    tip(languageBox, "language.help");
    tip(settingsButton, "settings.open.help");
    tip(inputPath, "files.input.help");
    tip(browseInput, "files.choose.help");
    tip(outputPath, "files.output.help");
    tip(browseOutput, "files.saveAs.help");
    tip(passwordField, "files.password.help");
    tip(passwordPlain, "files.password.help");
    tip(showPassword, "password.show.help");
    tip(encryptButton, "files.encrypt.help");
    tip(decryptButton, "files.decrypt.help");
    tip(dropHint, "files.drop.help");
    tip(plainText, "text.help");
    tip(openTextButton, "text.open.help");
    tip(textCounter, "text.counter.help");
    tip(swapTextButton, "text.swap.help");
    tip(keyField, "vigenere.key.help");
    tip(vigenereEncryptButton, "vigenere.encrypt.help");
    tip(vigenereDecryptButton, "vigenere.decrypt.help");
    tip(vigenereResult, "vigenere.result.help");
    tip(saveTextButton, "text.save.help");
    tip(copyTextButton, "copy.help");
    tip(caesarText, "caesar.text.help");
    tip(shiftSlider, "caesar.shift.help");
    tip(caesarShiftValue, "caesar.shift.help");
    tip(caesarEncryptButton, "caesar.encrypt.help");
    tip(caesarDecryptButton, "caesar.decrypt.help");
    tip(caesarResult, "caesar.result.help");
    tip(caesarOpenButton, "text.open.help");
    tip(caesarSaveButton, "text.save.help");
    tip(caesarCopyButton, "copy.help");
    tip(caesarCounter, "text.counter.help");
    tip(toast, "status.help");
    tip(statusLabel, "status.help");
    tip(progressLabel, "progress.help");
    tip(progressBar, "progress.help");
    tip(cancelButton, "cancelOperation.help");
    tip(tabs, "tabs.help");
    tip(filesScroll, "scroll.help");
    tip(vigenereScroll, "scroll.help");
    tip(caesarScroll, "scroll.help");
    tip(brandName, "app.title");
    tip(brandSubtitle, "app.title");
    tip(filesTitle, "files.intro");
    tip(filesIntro, "files.intro");
    tip(inputLabel, "files.input.help");
    tip(outputLabel, "files.output.help");
    tip(passwordLabel, "files.password.help");
    tip(filesNote, "files.password.help");
    tip(vigenereTitle, "vigenere.intro");
    tip(vigenereIntro, "vigenere.intro");
    tip(textInputLabel, "text.help");
    tip(keyLabel, "vigenere.key.help");
    tip(vigenereResultLabel, "vigenere.result.help");
    tip(caesarTitle, "caesar.intro");
    tip(caesarIntro, "caesar.intro");
    tip(caesarInputLabel, "caesar.text.help");
    tip(caesarShiftLabel, "caesar.shift.help");
    tip(caesarResultLabel, "caesar.result.help");
    String[] tabKeys = {"tab.files.help", "tab.vigenere.help", "tab.caesar.help"};
    for (int i = 0; i < tabs.getTabs().size() && i < tabKeys.length; i++) {
      tabs.getTabs().get(i).setTooltip(tooltip(tabKeys[i]));
    }
  }

  private Tooltip tooltip(String key) {
    Tooltip tooltip = new Tooltip(text(key));
    tooltip.setWrapText(true);
    tooltip.setMaxWidth(340);
    tooltip.setShowDelay(Duration.millis(350));
    tooltip.setHideDelay(Duration.millis(120));
    return tooltip;
  }

  private void tip(javafx.scene.control.Control control, String key) {
    if (control != null) control.setTooltip(tooltip(key));
  }

  public void applyState(UiState state) {
    if (state == null) return;
    inputPath.setText(state.inputPath());
    outputPath.setText(state.outputPath());
    passwordField.setText(state.password());
    passwordPlain.setText(state.password());
    plainText.setText(state.text());
    keyField.setText(state.key());
    vigenereResult.setText(state.result());
    caesarText.setText(state.caesarText());
    caesarResult.setText(state.caesarResult());
    if (state.shift() != null) shiftSlider.setValue(state.shift());
    tabs.getSelectionModel().select(Math.max(0, Math.min(state.tab(), tabs.getTabs().size() - 1)));
  }

  /**
   * Copia el contenido actual del formulario para poder reconstruir la vista.
   *
   * @return instantánea inmutable de los campos visibles
   */
  public UiState snapshot() {
    return new UiState(
        inputPath.getText(),
        outputPath.getText(),
        passwordText(),
        plainText.getText(),
        keyField.getText(),
        vigenereResult.getText(),
        caesarText.getText(),
        caesarResult.getText(),
        shift(),
        tabs.getSelectionModel().getSelectedIndex());
  }

  // ---------------------------------------------------------------- ficheros

  @FXML
  private void handleChooseInput() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(text("chooser.input"));
    if (lastDirectory != null && Files.isDirectory(lastDirectory)) {
      chooser.setInitialDirectory(lastDirectory.toFile());
    }
    File file = chooser.showOpenDialog(stage);
    if (file != null) selectInput(file);
  }

  private void selectInput(File file) {
    if (!file.isFile()) return;
    lastDirectory = file.toPath().getParent();
    inputPath.setText(file.getAbsolutePath());
    String name = file.getName();
    String defaultName =
        name.endsWith(".enc")
            ? name.substring(0, name.length() - 4) + text("file.recovered")
            : name + ".enc";
    outputPath.setText(file.toPath().resolveSibling(defaultName).toString());
    setStatus("files.selected", name, formatSize(file.length()));
  }

  @FXML
  private void handleChooseOutput() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(text("chooser.output"));
    String existing = outputPath.getText();
    if (!existing.isBlank()) {
      Path path = Path.of(existing).toAbsolutePath();
      if (path.getParent() != null && Files.isDirectory(path.getParent())) {
        chooser.setInitialDirectory(path.getParent().toFile());
      }
      if (path.getFileName() != null) chooser.setInitialFileName(path.getFileName().toString());
    } else if (lastDirectory != null && Files.isDirectory(lastDirectory)) {
      chooser.setInitialDirectory(lastDirectory.toFile());
    }
    File file = chooser.showSaveDialog(stage);
    if (file != null) {
      outputPath.setText(file.getAbsolutePath());
      lastDirectory = file.toPath().getParent();
    }
  }

  @FXML
  private void handleEncrypt() {
    handleFileOperation(true);
  }

  @FXML
  private void handleDecrypt() {
    handleFileOperation(false);
  }

  private void handleFileOperation(boolean encrypt) {
    String output = outputPath.getText().trim();
    Path input = pathOrNull(inputPath.getText());
    if (input == null || !Files.isRegularFile(input)) {
      warn("missing.file", "missing.file.help");
      return;
    }
    if (passwordText().isBlank()) {
      warn("missing.password", "missing.password.help");
      (passwordPlain.isVisible() ? passwordPlain : passwordField).requestFocus();
      return;
    }
    if (output.isBlank()) {
      warn("missing.output", "missing.output.help");
      return;
    }
    Path target = Path.of(output).toAbsolutePath().normalize();
    if (input.toAbsolutePath().normalize().equals(target)) {
      warn("invalid.output", "error.original");
      return;
    }
    if (Files.exists(target) && !confirmOverwrite(target)) return;
    if (!ensureConfigured()) return;

    String password = passwordText();
    beginOperation(
        encrypt ? "status.encryptFile" : "status.decryptFile",
        progress -> {
          ApiClient api = new ApiClient(settings.snapshot());
          new FileOperationService(api, files).execute(input, target, password, encrypt, progress);
          return target;
        },
        value -> {
          passwordField.clear();
          passwordPlain.clear();
          showToast(text(encrypt ? "file.encrypted" : "file.decrypted", value.toString()));
          setStatus("status.ready");
          // Al cifrar, la contraseña desaparece del formulario: conviene tenerla guardada.
          if (encrypt) warnSavePassword(password, false);
        },
        () -> handleFileOperation(encrypt));
  }

  // ---------------------------------------------------------------- Vigenère

  @FXML
  private void handleVigenereEncrypt() {
    handleVigenere("encrypt");
  }

  @FXML
  private void handleVigenereDecrypt() {
    handleVigenere("decrypt");
  }

  private void handleVigenere(String mode) {
    String body = plainText.getText();
    String key = keyField.getText() == null ? "" : keyField.getText().trim();
    if (body.isEmpty()) {
      warn("missing.text", "missing.text.help");
      plainText.requestFocus();
      return;
    }
    if (!key.matches("[A-Za-z]+")) {
      warn("invalid.key", "invalid.key.help");
      keyField.requestFocus();
      return;
    }
    if (!ensureConfigured()) return;
    beginOperation(
        "encrypt".equals(mode) ? "status.encryptText" : "status.decryptText",
        progress -> new ApiClient(settings.snapshot()).vigenere(body, key, mode),
        value -> {
          vigenereResult.setText(value);
          vigenereResult.requestFocus();
          setStatus("status.ready");
          if ("encrypt".equals(mode)) {
            warnSavePassword(key, true);
            keyField.clear();
          }
        },
        () -> handleVigenere(mode));
  }

  // ------------------------------------------------------------------ César

  @FXML
  private void handleCaesarEncrypt() {
    handleCaesar("encrypt");
  }

  @FXML
  private void handleCaesarDecrypt() {
    handleCaesar("decrypt");
  }

  private void handleCaesar(String mode) {
    String body = caesarText.getText();
    if (body.isEmpty()) {
      warn("missing.text", "missing.text.help");
      caesarText.requestFocus();
      return;
    }
    int shift = shift();
    if (!ensureConfigured()) return;
    beginOperation(
        "encrypt".equals(mode) ? "status.encryptText" : "status.decryptText",
        progress -> new ApiClient(settings.snapshot()).caesar(body, shift, mode),
        value -> {
          caesarResult.setText(value);
          caesarResult.requestFocus();
          setStatus("status.ready");
        },
        () -> handleCaesar(mode));
  }

  // ------------------------------------------------------------ texto común

  @FXML
  private void handleOpenText() {
    TextArea target = tabs.getSelectionModel().getSelectedIndex() == 2 ? caesarText : plainText;
    FileChooser chooser = new FileChooser();
    chooser.setTitle(text("chooser.textOpen"));
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(text("chooser.textFilter"), "*.txt"));
    if (lastDirectory != null && Files.isDirectory(lastDirectory)) {
      chooser.setInitialDirectory(lastDirectory.toFile());
    }
    File file = chooser.showOpenDialog(stage);
    if (file == null) return;
    try {
      String loaded = files.readText(file.toPath());
      // Los mensajes solo admiten letras, cifras y espacios: los signos no se cifran.
      target.setText(onlyText(loaded));
      lastDirectory = file.toPath().getParent();
      setStatus("text.loaded", file.getName());
    } catch (IOException ex) {
      showFailure(text("error.read.title"), ex, null);
    }
  }

  @FXML
  private void handleSaveText() {
    TextArea source = tabs.getSelectionModel().getSelectedIndex() == 2 ? caesarResult : vigenereResult;
    if (source.getText().isEmpty()) {
      warn("missing.result", "missing.result.help");
      return;
    }
    FileChooser chooser = new FileChooser();
    chooser.setTitle(text("chooser.textSave"));
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(text("chooser.textFilter"), "*.txt"));
    chooser.setInitialFileName(text("chooser.resultName"));
    if (lastDirectory != null && Files.isDirectory(lastDirectory)) {
      chooser.setInitialDirectory(lastDirectory.toFile());
    }
    File file = chooser.showSaveDialog(stage);
    if (file == null) return;
    if (Files.exists(file.toPath()) && !confirmOverwrite(file.toPath())) return;
    try {
      files.writeText(file.toPath(), source.getText());
      lastDirectory = file.toPath().getParent();
      setStatus("text.saved", file.getName());
    } catch (IOException ex) {
      showFailure(text("error.save.title"), ex, null);
    }
  }

  @FXML
  private void handleCopyResult() {
    TextArea source = tabs.getSelectionModel().getSelectedIndex() == 2 ? caesarResult : vigenereResult;
    if (source.getText().isEmpty()) {
      warn("missing.result", "missing.result.help");
      return;
    }
    ClipboardContent content = new ClipboardContent();
    content.putString(source.getText());
    Clipboard.getSystemClipboard().setContent(content);
    showToast(text("copy.done"));
  }

  @FXML
  private void handleSwapText() {
    String result = vigenereResult.getText();
    if (result.isEmpty()) {
      warn("missing.result", "missing.result.help");
      return;
    }
    plainText.setText(result);
    vigenereResult.clear();
    plainText.requestFocus();
    showToast(text("text.swapped"));
  }

  // --------------------------------------------------------------- ajustes

  @FXML
  private void handleSettings() {
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.initOwner(stage);
    dialog.setTitle(text("settings.title"));
    dialog.setHeaderText(text("settings.header"));

    TextField base = new TextField(settings.baseUrl());
    base.setPromptText(text("settings.server.prompt"));
    base.setAccessibleText(text("settings.server"));
    base.setAccessibleHelp(text("settings.server.help"));
    PasswordField token = new PasswordField();
    token.setText(settings.token());
    token.setPromptText(text("settings.token.prompt"));
    token.setAccessibleText(text("settings.token"));
    token.setAccessibleHelp(text("settings.token.help"));

    Label result = new Label();
    result.setWrapText(true);
    result.getStyleClass().add("helper");

    VBox form =
        new VBox(
            10,
            fieldLabel("settings.server"),
            base,
            fieldLabel("settings.token"),
            token,
            result);
    form.setPadding(new Insets(14));
    form.setPrefWidth(520);
    dialog.getDialogPane().setContent(form);

    ButtonType test = new ButtonType(text("settings.test"), ButtonBar.ButtonData.LEFT);
    ButtonType save = new ButtonType(text("save"), ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(test, save, ButtonType.CANCEL);
    translate(dialog.getDialogPane());
    Button testButton = (Button) dialog.getDialogPane().lookupButton(test);
    Button saveButton = (Button) dialog.getDialogPane().lookupButton(save);
    Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
    tip(base, "settings.server.help");
    tip(token, "settings.token.help");
    tip(testButton, "settings.test.help");
    tip(saveButton, "save.help");
    tip(cancelButton, "cancel.help");

    ButtonType logs = new ButtonType(text("logs.open"), ButtonBar.ButtonData.LEFT);
    dialog.getDialogPane().getButtonTypes().add(logs);
    Button logsButton = (Button) dialog.getDialogPane().lookupButton(logs);
    tip(logsButton, "logs.open.help");
    logsButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          event.consume();
          showLogs();
        });

    testButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          event.consume();
          SettingsStore candidate;
          try {
            candidate = new SettingsStore(base.getText(), token.getText());
          } catch (IllegalArgumentException ex) {
            result.setText(ex.getMessage());
            result.getStyleClass().setAll("helper", "invalid");
            return;
          }
          testButton.setDisable(true);
          saveButton.setDisable(true);
          result.setText(text("status.connection"));
          result.getStyleClass().setAll("helper");
          Task<Void> check =
              new Task<>() {
                @Override
                protected Void call() throws Exception {
                  new ApiClient(candidate.snapshot()).checkAuth();
                  return null;
                }
              };
          check.setOnSucceeded(
              done -> {
                testButton.setDisable(false);
                saveButton.setDisable(false);
                try {
                  settings.update(base.getText(), token.getText());
                  result.setText(text("settings.verified"));
                  result.getStyleClass().setAll("helper", "valid");
                  refreshConnectionChip();
                  setStatus("settings.saved");
                } catch (IllegalArgumentException ex) {
                  result.setText(ex.getMessage());
                  result.getStyleClass().setAll("helper", "invalid");
                }
              });
          check.setOnFailed(
              failed -> {
                testButton.setDisable(false);
                saveButton.setDisable(false);
                result.setText(I18n.errorMessage(check.getException()));
                result.getStyleClass().setAll("helper", "invalid");
                markDisconnected();
              });
          worker.submit(check);
        });

    Optional<ButtonType> answer = dialog.showAndWait();
    if (answer.isPresent() && answer.get() == save) {
      try {
        settings.update(base.getText(), token.getText());
        refreshConnectionChip();
        setStatus("settings.saved");
      } catch (IllegalArgumentException ex) {
        showFailure(text("settings.error.title"), ex, this::handleSettings);
      }
    }
  }

  /**
   * Visor del registro: últimas líneas, ruta del fichero y acceso a la carpeta que lo contiene. Se
   * abre desde Ajustes sin cerrar el diálogo, de modo que el usuario puede volver a sus datos.
   */
  void showLogs() {
    Path file = Logs.file();
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.initOwner(stage);
    dialog.setTitle(text("logs.title"));
    dialog.setHeaderText(text("logs.header"));

    Label path = new Label(file.toString());
    path.setWrapText(true);
    path.getStyleClass().add("helper");
    path.setAccessibleText(text("logs.path") + ": " + file);

    TextArea view = new TextArea();
    String tail = Logs.lastLines(file, Logs.VIEWER_LINES);
    view.setText(tail.isBlank() ? text("logs.empty") : tail);
    view.setEditable(false);
    view.setWrapText(false);
    view.getStyleClass().add("mono");
    view.setPrefRowCount(18);
    view.setPrefColumnCount(90);
    view.setAccessibleText(text("logs.title"));
    view.setAccessibleHelp(text("logs.open.help"));

    Label notice = new Label();
    notice.setWrapText(true);
    notice.getStyleClass().add("helper");

    VBox panel = new VBox(10, path, view, notice);
    panel.setPadding(new Insets(14));
    panel.setPrefWidth(760);
    dialog.getDialogPane().setContent(panel);

    ButtonType folder = new ButtonType(text("logs.openFolder"), ButtonBar.ButtonData.LEFT);
    ButtonType copyPath = new ButtonType(text("logs.copyPath"), ButtonBar.ButtonData.LEFT);
    dialog.getDialogPane().getButtonTypes().addAll(folder, copyPath, ButtonType.CLOSE);
    translate(dialog.getDialogPane());
    Button folderButton = (Button) dialog.getDialogPane().lookupButton(folder);
    Button copyButton = (Button) dialog.getDialogPane().lookupButton(copyPath);
    tip(folderButton, "logs.openFolder.help");
    tip(copyButton, "logs.copyPath.help");
    folderButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          event.consume();
          notice.setText(openFolder(file.getParent()) ? "" : text("logs.folderError"));
        });
    copyButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          event.consume();
          ClipboardContent selection = new ClipboardContent();
          selection.putString(file.toString());
          Clipboard.getSystemClipboard().setContent(selection);
          notice.setText(text("logs.copied"));
        });

    dialog.showAndWait();
  }

  /**
   * Abre la carpeta del registro con el explorador del sistema.
   *
   * @param folder carpeta que se quiere mostrar
   * @return {@code true} si el sistema la abrió
   */
  private boolean openFolder(Path folder) {
    try {
      if (folder == null
          || !Desktop.isDesktopSupported()
          || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        return false;
      }
      Desktop.getDesktop().open(folder.toFile());
      return true;
    } catch (IOException | RuntimeException ex) {
      LOG.warn("No se pudo abrir la carpeta del registro %s: %s", folder, ex);
      return false;
    }
  }

  /**
   * Aviso tras cifrar: la contraseña o la clave se borran de la interfaz, así que se ofrecen
   * copiarlas antes de cerrar el aviso. Sin ellas el resultado no se puede descifrar.
   *
   * @param secret contraseña AES o clave de Vigenère que se acaba de usar
   * @param key {@code true} si el secreto es la clave de Vigenère
   */
  private void warnSavePassword(String secret, boolean key) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.initOwner(stage);
    alert.setTitle(text(key ? "reminder.key.title" : "reminder.title"));
    alert.setHeaderText(text(key ? "reminder.key.header" : "reminder.header"));
    alert.getDialogPane().setContent(wrapped(text(key ? "reminder.key.body" : "reminder.body")));
    alert.getDialogPane().setPrefWidth(620);
    ButtonType copy =
        new ButtonType(
            text(key ? "reminder.key.copy" : "reminder.copy"), ButtonBar.ButtonData.LEFT);
    alert.getButtonTypes().setAll(copy, new ButtonType(text("close"), ButtonBar.ButtonData.OK_DONE));
    translate(alert.getDialogPane());
    Button copyButton = (Button) alert.getDialogPane().lookupButton(copy);
    tip(copyButton, key ? "reminder.key.copy.help" : "reminder.copy.help");
    Optional<ButtonType> answer = alert.showAndWait();
    if (answer.isPresent() && answer.get() == copy) copySecret(secret, key);
  }

  /**
   * Copia el secreto al portapapeles y lo anuncia; nunca se muestra en pantalla.
   *
   * @param secret contraseña o clave que se copia
   * @param key {@code true} si el secreto es la clave de Vigenère
   */
  private void copySecret(String secret, boolean key) {
    if (secret == null || secret.isBlank()) return;
    ClipboardContent content = new ClipboardContent();
    content.putString(secret);
    Clipboard.getSystemClipboard().setContent(content);
    showToast(text(key ? "reminder.key.copied" : "reminder.copied"));
  }

  private boolean ensureConfigured() {
    if (settings.isConfigured()) return true;
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.initOwner(stage);
    alert.setTitle(text("settings.required.title"));
    alert.setHeaderText(text("settings.required.header"));
    alert.getDialogPane().setContent(wrapped(text("settings.required.body")));
    ButtonType open = new ButtonType(text("settings.open"), ButtonBar.ButtonData.OK_DONE);
    alert.getButtonTypes().setAll(open, ButtonType.CLOSE);
    translate(alert.getDialogPane());
    alert.showAndWait();
    handleSettings();
    return false;
  }

  private void refreshConnectionChip() {
    boolean ready = settings.isConfigured();
    connectionChip.setText(text(ready ? "chip.configured" : "chip.notConfigured"));
    connectionChip.getStyleClass().setAll("chip", ready ? "chip-ok" : "chip-ko");
    connectionChip.setAccessibleText(connectionChip.getText());
  }

  private void markDisconnected() {
    connectionChip.setText(text("chip.error"));
    connectionChip.getStyleClass().setAll("chip", "chip-error");
    connectionChip.setAccessibleText(connectionChip.getText());
  }

  // ------------------------------------------------------------- infraestructura

  /** Trabajo que puede informar del progreso real de transferencia. */
  @FunctionalInterface
  private interface Work<T> {
    T run(ApiClient.Progress progress) throws Exception;
  }

  private <T> void beginOperation(
      String messageKey, Work<T> work, Consumer<T> onSuccess, Runnable retry) {
    if (running != null) return;
    lastAction = retry;
    setBusy(true, messageKey);
    Task<T> task =
        new Task<>() {
          @Override
          protected T call() throws Exception {
            return work.run(
                (done, total) -> {
                  updateProgress(done, total > 0 ? total : -1);
                  updateMessage(detail(done, total));
                });
          }
        };
    running = task;
    progressBar.progressProperty().bind(task.progressProperty());
    progressLabel.textProperty().bind(task.messageProperty());

    task.setOnSucceeded(
        event -> {
          finish();
          onSuccess.accept(task.getValue());
        });
    task.setOnCancelled(
        event -> {
          finish();
          setStatus("status.cancelled");
        });
    task.setOnFailed(
        event -> {
          finish();
          if (task.isCancelled()) {
            setStatus("status.cancelled");
            return;
          }
          setStatus("status.failed");
          showFailure(text("error.operation.title"), task.getException(), retry);
        });
    worker.submit(task);
  }

  private void finish() {
    running = null;
    progressBar.progressProperty().unbind();
    progressLabel.textProperty().unbind();
    progressBar.setProgress(0);
    progressLabel.setText("");
    setBusy(false, null);
  }

  private void setBusy(boolean busy, String messageKey) {
    progressBar.setVisible(busy);
    progressBar.setManaged(busy);
    progressLabel.setVisible(busy);
    progressLabel.setManaged(busy);
    cancelButton.setVisible(busy);
    cancelButton.setManaged(busy);
    encryptButton.setDisable(busy);
    decryptButton.setDisable(busy);
    vigenereEncryptButton.setDisable(busy);
    vigenereDecryptButton.setDisable(busy);
    caesarEncryptButton.setDisable(busy);
    caesarDecryptButton.setDisable(busy);
    settingsButton.setDisable(busy);
    if (messageKey != null) setStatus(messageKey);
  }

  @FXML
  private void handleCancel() {
    if (running == null) return;
    running.cancel();
    setStatus("status.cancelled");
  }

  private void changeLanguage(String selected) {
    String code = "Euskara".equals(selected) ? "eu" : "English".equals(selected) ? "en" : "es";
    if (code.equals(I18n.locale().getLanguage())) return;
    I18n.setLanguage(code);
    if (languageHandler != null) {
      Platform.runLater(() -> languageHandler.accept(snapshot()));
    }
  }

  private void configureDropTarget() {
    fileCard.setOnDragOver(
        event -> {
          Dragboard board = event.getDragboard();
          if (board.hasFiles() && board.getFiles().size() == 1) {
            event.acceptTransferModes(TransferMode.COPY);
            if (!fileCard.getStyleClass().contains("card-drop")) {
              fileCard.getStyleClass().add("card-drop");
            }
          }
          event.consume();
        });
    fileCard.setOnDragExited(
        event -> {
          fileCard.getStyleClass().remove("card-drop");
          event.consume();
        });
    fileCard.setOnDragDropped(
        event -> {
          fileCard.getStyleClass().remove("card-drop");
          List<File> dropped = event.getDragboard().getFiles();
          boolean done = dropped != null && !dropped.isEmpty();
          if (done) selectInput(dropped.get(0));
          event.setDropCompleted(done);
          event.consume();
        });
  }

  private void updateCounter(String value, Label target) {
    target.setText(text("text.counter", value == null ? 0 : value.length()));
  }

  /**
   * Rechaza los signos mientras se escribe o se pega en los mensajes de Vigenère y César: el
   * servidor solo cifra A–Z, así que aceptar símbolos daría la falsa impresión de que se protegen.
   */
  private void restrictToText(TextInputControl area) {
    area.setTextFormatter(
        new TextFormatter<String>(
            change -> {
              if (!change.isContentChange()) return change;
              return onlyText(change.getText()).equals(change.getText()) ? change : null;
            }));
  }

  /**
   * Texto que admite el mensaje: letras (también con acentos), cifras, espacios y saltos de línea.
   *
   * @param value texto escrito, pegado o leído de un fichero
   * @return el mismo texto sin los signos que no se cifran
   */
  static String onlyText(String value) {
    if (value == null) return "";
    StringBuilder clean = new StringBuilder(value.length());
    value
        .codePoints()
        .filter(c -> Character.isLetter(c) || Character.isDigit(c) || Character.isWhitespace(c))
        .forEach(clean::appendCodePoint);
    return clean.toString();
  }

  /** Desplazamiento elegido en la barra, redondeado a un entero entre 1 y 25. */
  /**
   * Crea el candado del botón. Se hace en código y no en el FXML para que la vista se abra en
   * cualquier Scene Builder sin añadir Ikonli a su classpath.
   */
  private static void icon(Button button, String literal) {
    FontIcon icon = new FontIcon(literal);
    icon.setIconSize(15);
    button.setGraphic(icon);
  }

  private int shift() {
    return (int) Math.round(shiftSlider.getValue());
  }

  /** Mantiene al día el número visible del desplazamiento, también para lectores de pantalla. */
  private void updateShift() {
    int shift = shift();
    caesarShiftValue.setText(Integer.toString(shift));
    caesarShiftValue.setAccessibleText(text("caesar.shift") + ": " + shift);
  }

  /** Anuncia el resultado en el idioma activo sin abrir diálogos bloqueantes. */
  private void showToast(String message) {
    toast.setText(message);
    toast.setAccessibleText(message);
    toast.setVisible(true);
    toast.setManaged(true);
    if (toastTimer != null) toastTimer.stop();
    toastTimer = new PauseTransition(Duration.seconds(5));
    toastTimer.setOnFinished(finished -> {
      toast.setVisible(false);
      toast.setManaged(false);
    });
    toastTimer.play();
  }

  private void setStatus(String key, Object... arguments) {
    String message = text(key, arguments);
    statusLabel.setText(message);
    statusLabel.setAccessibleText(message);
  }

  private void warn(String titleKey, String helpKey) {
    showFailure(text(titleKey), I18n.failure(helpKey), null);
  }

  private void showFailure(String title, Throwable error, Runnable retry) {
    LOG.error("Fallo mostrado al usuario («%s»): %s", error, title, I18n.errorMessage(error));
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.initOwner(stage);
    alert.setTitle(title);
    alert.setHeaderText(title);
    alert.getDialogPane().setContent(wrapped(I18n.errorMessage(error)));
    alert.getDialogPane().setPrefWidth(620);
    ButtonType retryType = new ButtonType(text("retry"), ButtonBar.ButtonData.OK_DONE);
    ButtonType settingsType = new ButtonType(text("goSettings"), ButtonBar.ButtonData.LEFT);
    alert.getButtonTypes().setAll(settingsType, retryType, ButtonType.CLOSE);
    translate(alert.getDialogPane());
    Optional<ButtonType> answer = alert.showAndWait();
    if (answer.isEmpty()) return;
    if (answer.get() == retryType && retry != null) {
      Platform.runLater(retry);
    } else if (answer.get() == settingsType) {
      handleSettings();
    }
  }

  private boolean confirmOverwrite(Path path) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.initOwner(stage);
    alert.setTitle(text("confirm.title"));
    alert.setHeaderText(text("confirm.header"));
    alert.getDialogPane().setContent(wrapped(text("confirm.body", path)));
    alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
    translate(alert.getDialogPane());
    return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
  }

  private void translate(javafx.scene.control.DialogPane pane) {
    for (ButtonType type : pane.getButtonTypes()) {
      String key =
          type == ButtonType.CANCEL
              ? "cancel"
              : type == ButtonType.OK
                  ? "ok"
                  : type == ButtonType.CLOSE ? "close" : null;
      if (key != null) {
        Button button = (Button) pane.lookupButton(type);
        if (button != null) button.setText(text(key));
      }
    }
  }

  private Label wrapped(String message) {
    Label label = new Label(message == null || message.isBlank() ? text("error.unknown") : message);
    label.setWrapText(true);
    label.setMaxWidth(560);
    return label;
  }

  private Label fieldLabel(String key) {
    Label label = new Label(text(key));
    label.getStyleClass().add("field-label");
    return label;
  }

  private void accessible(javafx.scene.control.Control control, String key) {
    control.setAccessibleText(text(key));
    if (control instanceof TextInputControl input) installEditMenu(input);
  }

  /**
   * Aplica el modelo ARIA de la §9.3: cada control declara su rol y los botones de icono su nombre
   * accesible. Se ejecuta al cargar la vista, así que también sigue el idioma activo al recargar.
   */
  private void installAccessibility() {
    role(tabs, AccessibleRole.TAB_PANE);
    role(languageBox, AccessibleRole.COMBO_BOX);
    role(connectionChip, AccessibleRole.TEXT);
    role(showPassword, AccessibleRole.TOGGLE_BUTTON);
    role(shiftSlider, AccessibleRole.SLIDER);
    role(caesarShiftValue, AccessibleRole.TEXT);
    role(textCounter, AccessibleRole.TEXT);
    role(caesarCounter, AccessibleRole.TEXT);
    role(dropHint, AccessibleRole.TEXT);
    role(filesNote, AccessibleRole.TEXT);
    role(progressBar, AccessibleRole.PROGRESS_INDICATOR);
    role(progressLabel, AccessibleRole.TEXT);
    role(statusLabel, AccessibleRole.TEXT);
    role(toast, AccessibleRole.TEXT);
    role(inputPath, AccessibleRole.TEXT_FIELD);
    role(outputPath, AccessibleRole.TEXT_FIELD);
    role(passwordField, AccessibleRole.PASSWORD_FIELD);
    role(passwordPlain, AccessibleRole.TEXT_FIELD);
    role(keyField, AccessibleRole.TEXT_FIELD);
    role(plainText, AccessibleRole.TEXT_AREA);
    role(vigenereResult, AccessibleRole.TEXT_AREA);
    role(caesarText, AccessibleRole.TEXT_AREA);
    role(caesarResult, AccessibleRole.TEXT_AREA);
    for (ScrollPane pane : List.of(filesScroll, vigenereScroll, caesarScroll)) {
      role(pane, AccessibleRole.SCROLL_PANE);
    }
    for (Label heading :
        List.of(
            brandName,
            brandSubtitle,
            filesTitle,
            filesIntro,
            inputLabel,
            outputLabel,
            passwordLabel,
            vigenereTitle,
            vigenereIntro,
            textInputLabel,
            keyLabel,
            vigenereResultLabel,
            caesarTitle,
            caesarIntro,
            caesarInputLabel,
            caesarShiftLabel,
            caesarResultLabel)) {
      role(heading, AccessibleRole.TEXT);
      heading.setAccessibleText(heading.getText());
    }
    for (Button button :
        List.of(
            encryptButton,
            decryptButton,
            browseInput,
            browseOutput,
            settingsButton,
            openTextButton,
            saveTextButton,
            copyTextButton,
            swapTextButton,
            vigenereEncryptButton,
            vigenereDecryptButton,
            caesarEncryptButton,
            caesarDecryptButton,
            caesarOpenButton,
            caesarSaveButton,
            caesarCopyButton,
            cancelButton)) {
      role(button, AccessibleRole.BUTTON);
      String current = button.getAccessibleText();
      if (current == null || current.isBlank()) {
        // El guion bajo del FXML es un mnemónico: no forma parte del nombre accesible.
        button.setAccessibleText(button.getText().replace("_", "").trim());
      }
    }
  }

  private static void role(javafx.scene.control.Control control, AccessibleRole value) {
    if (control != null) control.setAccessibleRole(value);
  }

  /** El menú JavaFX predeterminado usa el idioma del entorno; este sigue el selector. */
  private void installEditMenu(TextInputControl input) {
    javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
    String[] keys = {
      "edit.undo", "edit.redo", "edit.cut", "edit.copy", "edit.paste", "edit.delete",
      "edit.selectAll"
    };
    Runnable[] actions = {
      input::undo,
      input::redo,
      input::cut,
      input::copy,
      input::paste,
      () -> input.replaceSelection(""),
      input::selectAll
    };
    for (int i = 0; i < keys.length; i++) {
      javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(text(keys[i]));
      int index = i;
      item.setOnAction(event -> actions[index].run());
      switch (i) {
        case 0 -> item.setDisable(!input.isUndoable() && !input.isEditable());
        case 1 -> item.setDisable(!input.isRedoable());
        case 2, 3 -> item.setDisable(true);
        case 4, 5 -> item.setDisable(!input.isEditable());
        default -> { }
      }
      menu.getItems().add(item);
    }
    menu.setOnShowing(
        event -> {
          for (int i = 0; i < keys.length; i++) {
            javafx.scene.control.MenuItem item = menu.getItems().get(i);
            item.setText(text(keys[i]));
            switch (i) {
              case 0 -> item.setDisable(!input.isUndoable() || !input.isEditable());
              case 1 -> item.setDisable(!input.isRedoable() || !input.isEditable());
              case 2, 3 -> item.setDisable(
                  input instanceof PasswordField || input.getSelectedText().isEmpty());
              case 4 -> item.setDisable(!input.isEditable());
              case 5 -> item.setDisable(!input.isEditable() || input.getSelectedText().isEmpty());
              default -> item.setDisable(false);
            }
          }
        });
    input.setContextMenu(menu);
  }

  private String passwordText() {
    return passwordPlain.isVisible() ? passwordPlain.getText() : passwordField.getText();
  }

  private static Path pathOrNull(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return Path.of(value.trim());
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private static String detail(long done, long total) {
    String transferred = formatSize(done);
    return total > 0 ? transferred + " / " + formatSize(total) : transferred;
  }

  static String formatSize(long bytes) {
    double value = bytes;
    String unit = "B";
    if (bytes >= 1024) {
      value = bytes / 1024.0;
      unit = "KiB";
    }
    if (bytes >= 1024 * 1024) {
      value = bytes / (1024.0 * 1024.0);
      unit = "MiB";
    }
    if ("B".equals(unit)) return bytes + " B";
    return String.format(Locale.ROOT, "KiB".equals(unit) ? KIB : MIB, value);
  }

  /** Detiene las tareas pendientes cuando JavaFX cierra la aplicación. */
  public void stop() {
    if (running != null) running.cancel();
    worker.shutdownNow();
  }
}
