package org.example;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.AccessibleRole;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {
    private final SettingsStore settings = new SettingsStore();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "runnersz-network-worker"); thread.setDaemon(true); return thread;
    });
    private Stage stage;
    private Label status;
    private ProgressIndicator progress;
    private Button encryptButton, decryptButton, vigenereEncryptButton, vigenereDecryptButton;
    private TextField inputPath, outputPath;
    private PasswordField aesPassword;
    private TextArea plainText, resultText;
    private File selectedInput;

    @Override public void start(Stage stage) {
        this.stage = stage;
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter.ttf"), 16);
        Font.loadFont(getClass().getResourceAsStream("/fonts/OpenSans.ttf"), 16);
        stage.setTitle("RunnersZ · Cliente de escritorio");
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(header());
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("main-tabs");
        tabs.getTabs().addAll(new Tab("Ficheros · AES", fileView()), new Tab("Mensajes · Vigenère", vigenereView()));
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        root.setCenter(tabs);
        root.setBottom(footer());
        Scene scene = new Scene(root, 980, 720);
        scene.getStylesheets().add(getClass().getResource("/org/example/runnersz.css").toExternalForm());
        stage.setMinWidth(760); stage.setMinHeight(620); stage.setScene(scene); stage.show();
    }

    private HBox header() {
        Label brand = new Label("RUNNERSZ"); brand.getStyleClass().add("brand");
        Label subtitle = new Label("HOGWARTS  /  GESTIÓN SEGURA DE ARCHIVOS"); subtitle.getStyleClass().add("eyebrow");
        Button config = new Button("Ajustes"); accessible(config, AccessibleRole.BUTTON, "Abrir ajustes de conexión"); config.setOnAction(e -> showSettings());
        VBox identity = new VBox(4, brand, subtitle); HBox.setHgrow(identity, Priority.ALWAYS);
        HBox bar = new HBox(16, identity, config); bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(24, 32, 20, 32)); bar.getStyleClass().add("header");
        return bar;
    }

    private VBox fileView() {
        Label title = new Label("Protección de ficheros"); title.getStyleClass().add("section-title");
        Label intro = new Label("Cifra o recupera un archivo mediante el servicio seguro RunnersZ."); intro.getStyleClass().add("muted");
        inputPath = new TextField(); accessible(inputPath, AccessibleRole.TEXT_FIELD, "Fichero de entrada"); inputPath.setPromptText("Selecciona un fichero de entrada"); inputPath.setAccessibleHelp("Ruta del fichero que se enviará al servidor."); inputPath.setEditable(false);
        Button browseInput = new Button("Elegir…"); accessible(browseInput, AccessibleRole.BUTTON, "Elegir fichero de entrada"); browseInput.setOnAction(e -> chooseInput());
        HBox inputRow = new HBox(10, inputPath, browseInput); HBox.setHgrow(inputPath, Priority.ALWAYS);
        outputPath = new TextField(); accessible(outputPath, AccessibleRole.TEXT_FIELD, "Fichero de salida"); outputPath.setPromptText("Ubicación de salida"); outputPath.setAccessibleHelp("Ruta donde se guardará el resultado; el original se conserva."); outputPath.setEditable(false);
        Button browseOutput = new Button("Guardar como…"); accessible(browseOutput, AccessibleRole.BUTTON, "Elegir ubicación del fichero de salida"); browseOutput.setOnAction(e -> chooseOutput());
        HBox outputRow = new HBox(10, outputPath, browseOutput); HBox.setHgrow(outputPath, Priority.ALWAYS);
        aesPassword = new PasswordField(); accessible(aesPassword, AccessibleRole.PASSWORD_FIELD, "Contraseña AES"); aesPassword.setPromptText("Contraseña AES"); aesPassword.setAccessibleHelp("La contraseña se envía al servidor para cifrar o descifrar. No se guarda.");
        Label note = new Label("La contraseña solo se usa en esta operación. Los ficheros vacíos se rechazan de forma controlada."); note.getStyleClass().add("helper"); note.setWrapText(true);
        VBox card = new VBox(14, fieldLabel("Fichero de entrada"), inputRow, fieldLabel("Fichero de salida"), outputRow, fieldLabel("Contraseña"), aesPassword, note);
        card.getStyleClass().add("card");
        encryptButton = new Button("_Cifrar archivo"); accessible(encryptButton, AccessibleRole.BUTTON, "Cifrar archivo AES"); encryptButton.setMnemonicParsing(true); encryptButton.setOnAction(e -> runFileOperation(true)); encryptButton.getStyleClass().add("primary-button");
        decryptButton = new Button("_Descifrar archivo"); accessible(decryptButton, AccessibleRole.BUTTON, "Descifrar archivo AES"); decryptButton.setMnemonicParsing(true); decryptButton.setOnAction(e -> runFileOperation(false));
        HBox actions = new HBox(12, encryptButton, decryptButton); actions.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(18, title, intro, card, actions); content.setPadding(new Insets(28, 36, 28, 36)); content.getStyleClass().add("content");
        return content;
    }

    private VBox vigenereView() {
        Label title = new Label("Cifrado Vigenère"); title.getStyleClass().add("section-title");
        Label intro = new Label("Los mensajes se procesan en el servidor. Espacios, signos y caracteres fuera de A–Z se conservan."); intro.getStyleClass().add("muted"); intro.setWrapText(true);
        plainText = new TextArea(); accessible(plainText, AccessibleRole.TEXT_AREA, "Texto de entrada"); plainText.setPromptText("Escribe un mensaje o carga un archivo .txt…"); plainText.setWrapText(true); plainText.setPrefRowCount(8); plainText.setAccessibleHelp("Texto en UTF-8 que se cifrará o descifrará.");
        resultText = new TextArea(); accessible(resultText, AccessibleRole.TEXT_AREA, "Resultado Vigenère"); resultText.setPromptText("El resultado aparecerá aquí"); resultText.setWrapText(true); resultText.setPrefRowCount(8); resultText.setEditable(false);
        TextField key = new TextField(); accessible(key, AccessibleRole.TEXT_FIELD, "Clave Vigenère"); key.setPromptText("Clave (solo letras A–Z)"); key.setAccessibleHelp("Clave no vacía compuesta por letras de la A a la Z.");
        Button load = new Button("Abrir .txt"); accessible(load, AccessibleRole.BUTTON, "Abrir archivo de texto UTF-8"); load.setOnAction(e -> loadText());
        Button save = new Button("Guardar resultado…"); accessible(save, AccessibleRole.BUTTON, "Guardar resultado como archivo de texto"); save.setOnAction(e -> saveText());
        vigenereEncryptButton = new Button("_Cifrar"); accessible(vigenereEncryptButton, AccessibleRole.BUTTON, "Cifrar mensaje con Vigenère"); vigenereEncryptButton.setMnemonicParsing(true); vigenereEncryptButton.getStyleClass().add("primary-button"); vigenereEncryptButton.setOnAction(e -> runVigenere(key.getText(), "encrypt"));
        vigenereDecryptButton = new Button("_Descifrar"); accessible(vigenereDecryptButton, AccessibleRole.BUTTON, "Descifrar mensaje con Vigenère"); vigenereDecryptButton.setMnemonicParsing(true); vigenereDecryptButton.setOnAction(e -> runVigenere(key.getText(), "decrypt"));
        VBox card = new VBox(12, fieldLabel("Texto de entrada"), plainText, new HBox(10, load), fieldLabel("Clave"), key, new HBox(10, vigenereEncryptButton, vigenereDecryptButton), fieldLabel("Resultado"), resultText, save);
        card.getStyleClass().add("card");
        VBox content = new VBox(18, title, intro, card); content.setPadding(new Insets(28, 36, 28, 36)); content.getStyleClass().add("content");
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.getStyleClass().add("content-scroll"); return new VBox(scroll);
    }

    private HBox footer() {
        status = new Label("Listo · configura el servidor para empezar"); status.getStyleClass().add("status-text");
        progress = new ProgressIndicator(); progress.setMaxSize(20, 20); progress.setVisible(false); progress.setAccessibleRole(AccessibleRole.PROGRESS_INDICATOR); progress.setAccessibleText("Operación en curso");
        HBox bar = new HBox(10, progress, status); bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(12, 32, 16, 32)); bar.getStyleClass().add("footer"); return bar;
    }

    private Label fieldLabel(String text) { Label label = new Label(text); label.getStyleClass().add("field-label"); return label; }
    private void accessible(Control control, AccessibleRole role, String name) { control.setAccessibleRole(role); control.setAccessibleText(name); }

    private void chooseInput() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("Seleccionar fichero");
        File file = chooser.showOpenDialog(stage); if (file == null) return;
        selectedInput = file; inputPath.setText(file.getAbsolutePath());
        String name = file.getName(); String defaultName = name.endsWith(".enc") ? name.substring(0, name.length()-4) + ".recuperado" : name + ".enc";
        outputPath.setText(file.toPath().resolveSibling(defaultName).toString());
    }

    private void chooseOutput() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("Guardar resultado");
        String existing = outputPath.getText(); if (!existing.isBlank()) { Path p = Path.of(existing); if (p.getParent() != null && Files.isDirectory(p.getParent())) chooser.setInitialDirectory(p.getParent().toFile()); chooser.setInitialFileName(p.getFileName().toString()); }
        File file = chooser.showSaveDialog(stage); if (file != null) outputPath.setText(file.getAbsolutePath());
    }

    private void runFileOperation(boolean encrypt) {
        String password = aesPassword.getText(); String output = outputPath.getText().trim();
        if (selectedInput == null || !selectedInput.isFile()) { alert(Alert.AlertType.WARNING, "Falta el fichero", "Selecciona un fichero de entrada."); return; }
        if (password.isBlank()) { alert(Alert.AlertType.WARNING, "Falta la contraseña", "Introduce la contraseña AES."); aesPassword.requestFocus(); return; }
        if (output.isBlank()) { alert(Alert.AlertType.WARNING, "Falta el destino", "Indica dónde guardar el resultado."); return; }
        Path in = selectedInput.toPath().toAbsolutePath().normalize(), out = Path.of(output).toAbsolutePath().normalize();
        if (in.equals(out)) { alert(Alert.AlertType.WARNING, "Destino no válido", "El resultado no puede sobrescribir el fichero original."); return; }
        if (Files.exists(out) && !confirmOverwrite(out)) return;
        beginOperation(encrypt ? "Cifrando archivo…" : "Descifrando y verificando integridad…", () -> {
            byte[] source = Files.readAllBytes(in);
            if (source.length == 0) throw new java.io.IOException("El archivo está vacío (EMPTY_FILE).");
            ApiClient.FileResult response = new ApiClient(settings).fileOperation(source, in.getFileName().toString(), password, encrypt);
            Path parent = out.getParent(); if (parent != null) Files.createDirectories(parent);
            Path temp = Files.createTempFile(parent, ".runnersz-", ".part");
            try { Files.write(temp, response.bytes()); try { Files.move(temp, out, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING); } catch (java.nio.file.AtomicMoveNotSupportedException ex) { Files.move(temp, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING); } }
            finally { Files.deleteIfExists(temp); }
            return encrypt ? "Archivo cifrado y guardado en " + out : "Integridad verificada. Archivo recuperado en " + out;
        }, true);
    }

    private void runVigenere(String key, String mode) {
        String text = plainText.getText();
        if (text.isEmpty()) { alert(Alert.AlertType.WARNING, "Falta el texto", "Escribe un mensaje o abre un archivo .txt."); plainText.requestFocus(); return; }
        if (key == null || !key.matches("[A-Za-z]+")) { alert(Alert.AlertType.WARNING, "Clave no válida", "La clave debe contener solo letras A–Z."); return; }
        beginOperation(mode.equals("encrypt") ? "Cifrando mensaje…" : "Descifrando mensaje…", () -> new ApiClient(settings).vigenere(text, key, mode), false, value -> resultText.setText(value));
    }

    private void loadText() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("Abrir texto UTF-8"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto", "*.txt"));
        File file = chooser.showOpenDialog(stage); if (file == null) return;
        try { plainText.setText(Files.readString(file.toPath(), StandardCharsets.UTF_8)); setStatus("Texto cargado: " + file.getName()); }
        catch (Exception ex) { showError("No se pudo leer el archivo", ex); }
    }

    private void saveText() {
        if (resultText.getText().isEmpty()) { alert(Alert.AlertType.INFORMATION, "Sin resultado", "Primero cifra o descifra un mensaje."); return; }
        FileChooser chooser = new FileChooser(); chooser.setTitle("Guardar resultado UTF-8"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto", "*.txt")); chooser.setInitialFileName("resultado.txt");
        File file = chooser.showSaveDialog(stage); if (file == null) return;
        if (Files.exists(file.toPath()) && !confirmOverwrite(file.toPath())) return;
        try { Files.writeString(file.toPath(), resultText.getText(), StandardCharsets.UTF_8); setStatus("Resultado guardado: " + file.getName()); }
        catch (Exception ex) { showError("No se pudo guardar el resultado", ex); }
    }

    private void showSettings() {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.initOwner(stage); dialog.setTitle("Ajustes de conexión"); dialog.setHeaderText("Conexión con el servidor RunnersZ");
        TextField base = new TextField(settings.baseUrl()); accessible(base, AccessibleRole.TEXT_FIELD, "Dirección del servidor"); base.setPromptText("http://192.168.1.10:8000"); base.setAccessibleHelp("Dirección base, por ejemplo http://IP:8000. No incluyas /api/v1.");
        PasswordField token = new PasswordField(); accessible(token, AccessibleRole.PASSWORD_FIELD, "Token de API"); token.setText(settings.token()); token.setPromptText("Token de API"); token.setAccessibleHelp("Token Bearer configurado en el servidor. Se guarda en las preferencias locales.");
        VBox form = new VBox(10, fieldLabel("Dirección del servidor"), base, fieldLabel("Token de API"), token); form.setPadding(new Insets(12)); dialog.getDialogPane().setContent(form);
        ButtonType test = new ButtonType("Probar", ButtonBar.ButtonData.LEFT); ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(test, save, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(test).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            try {
                SettingsStore candidate = new SettingsStore(base.getText().trim(), token.getText());
                dialog.close();
                beginOperation("Comprobando conexión…", () -> { new ApiClient(candidate).checkAuth(); return "Conexión y autenticación correctas."; }, false);
            } catch (Exception ex) { alert(Alert.AlertType.ERROR, "Ajustes no válidos", ex.getMessage()); }
        });
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == save) {
            try { settings.update(base.getText().trim(), token.getText()); setStatus("Ajustes guardados localmente."); }
            catch (Exception ex) { showError("No se pudieron guardar los ajustes", ex); }
        }
    }

    @FunctionalInterface private interface Work<T> { T run() throws Exception; }
    private <T> void beginOperation(String message, Work<T> work, boolean fileOperation) { beginOperation(message, work, fileOperation, value -> {}); }
    private <T> void beginOperation(String message, Work<T> work, boolean fileOperation, java.util.function.Consumer<T> onSuccess) {
        setBusy(true, message);
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return work.run(); } };
        task.setOnSucceeded(e -> { setBusy(false, "Listo"); onSuccess.accept(task.getValue()); if (fileOperation) alert(Alert.AlertType.INFORMATION, "Operación completada", task.getValue().toString()); else if (task.getValue() instanceof String s && s.startsWith("Conexión")) alert(Alert.AlertType.INFORMATION, "Servidor disponible", s); });
        task.setOnFailed(e -> { setBusy(false, "La operación no se completó."); showError("No se pudo completar la operación", task.getException()); });
        worker.submit(task);
    }

    private boolean confirmOverwrite(Path path) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Ya existe un archivo en:\n" + path + "\n¿Quieres reemplazarlo?", ButtonType.CANCEL, ButtonType.OK);
        alert.initOwner(stage); alert.setTitle("Confirmar reemplazo"); alert.setHeaderText("El destino ya existe"); return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
    private void setBusy(boolean busy, String message) { progress.setVisible(busy); status.setText(message); encryptButton.setDisable(busy); decryptButton.setDisable(busy); vigenereEncryptButton.setDisable(busy); vigenereDecryptButton.setDisable(busy); }
    private void setStatus(String message) { status.setText(message); }
    private void alert(Alert.AlertType type, String title, String message) { Alert alert = new Alert(type, message, ButtonType.OK); alert.initOwner(stage); alert.setTitle(title); alert.setHeaderText(title); alert.showAndWait(); }
    private void showError(String title, Throwable error) {
        Throwable cause = error; while (cause != null && cause.getCause() != null) cause = cause.getCause();
        String message = cause == null ? "Error desconocido." : cause.getMessage();
        if (message == null || message.isBlank()) message = "Error de conexión o de procesamiento. Comprueba el servidor y vuelve a intentarlo.";
        alert(Alert.AlertType.ERROR, title, message);
    }
    @Override public void stop() { worker.shutdownNow(); }
    public static void main(String[] args) { launch(args); }
}
