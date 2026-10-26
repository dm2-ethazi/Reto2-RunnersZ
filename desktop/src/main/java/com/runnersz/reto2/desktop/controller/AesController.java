package com.runnersz.reto2.desktop.controller;

import com.runnersz.reto2.desktop.model.AesResult;
import com.runnersz.reto2.desktop.service.BackendException;
import com.runnersz.reto2.desktop.service.BackendService;
import java.io.File;
import java.nio.file.Path;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class AesController {
    private final BackendService backend;
    private final VBox view = new VBox(16);
    private final TextField inputField = new TextField();
    private final TextField outputField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button encryptButton = new Button("Cifrar");
    private final Button decryptButton = new Button("Descifrar");
    private final Button inputBrowse = new Button("Examinar…");
    private final Button outputBrowse = new Button("Examinar…");
    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label("Selecciona un fichero y una contraseña.");
    private boolean backendReady;
    private boolean running;

    public AesController(BackendService backend) {
        this.backend = backend;
        build();
    }

    private void build() {
        Label title = new Label("Ficheros (AES)");
        title.getStyleClass().add("section-title");
        Label description =
                new Label(
                        "Cifra o recupera cualquier fichero con AES-256-GCM. "
                                + "El original nunca se modifica.");
        description.getStyleClass().add("section-description");
        description.setWrapText(true);

        inputField.setPromptText("Ruta del fichero de entrada");
        outputField.setPromptText("Ruta del fichero de salida");
        passwordField.setPromptText("Contraseña (no se almacena)");
        inputBrowse.getStyleClass().add("button-secondary");
        outputBrowse.getStyleClass().add("button-secondary");
        encryptButton.getStyleClass().add("button-primary");
        decryptButton.getStyleClass().add("button-primary");

        HBox inputRow = new HBox(8, inputField, inputBrowse);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        HBox outputRow = new HBox(8, outputField, outputBrowse);
        HBox.setHgrow(outputField, Priority.ALWAYS);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(110);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labels, fields);
        form.addRow(0, new Label("Entrada"), inputRow);
        form.addRow(1, new Label("Salida"), outputRow);
        form.addRow(2, new Label("Contraseña"), passwordField);

        HBox actions = new HBox(10, encryptButton, decryptButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(8);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        statusLabel.getStyleClass().add("status");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(14, form, actions, progressBar, statusLabel);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));

        inputBrowse.setOnAction(event -> chooseInput());
        outputBrowse.setOnAction(event -> chooseOutput());
        encryptButton.setOnAction(event -> runOperation(true));
        decryptButton.setOnAction(event -> runOperation(false));

        view.getChildren().addAll(title, description, card);
        view.setPadding(new Insets(20));
        updateControls();
    }

    public Parent getView() {
        return view;
    }

    public void setBackendReady(boolean ready) {
        backendReady = ready;
        updateControls();
    }

    private void chooseInput() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar fichero de entrada");
        File selected = chooser.showOpenDialog(window());
        if (selected == null) {
            return;
        }
        inputField.setText(selected.getAbsolutePath());
        if (outputField.getText().isBlank()) {
            outputField.setText(suggestOutput(selected.toPath(), true));
        }
    }

    private void chooseOutput() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar fichero de salida");
        if (!inputField.getText().isBlank()) {
            try {
                chooser.setInitialFileName(
                        Path.of(inputField.getText().trim()).getFileName() + ".enc");
            } catch (RuntimeException ignored) {
                chooser.setInitialFileName("salida.enc");
            }
        }
        File selected = chooser.showSaveDialog(window());
        if (selected != null) {
            outputField.setText(selected.getAbsolutePath());
        }
    }

    private void runOperation(boolean encrypt) {
        if (!backendReady) {
            UiAlerts.error(window(), "Backend no disponible", "Espera a que el backend esté listo.");
            return;
        }
        String input = inputField.getText().trim();
        String output = outputField.getText().trim();
        String password = passwordField.getText();
        if (input.isEmpty() || output.isEmpty()) {
            UiAlerts.error(window(), "Datos incompletos", "Indica el fichero de entrada y el de salida.");
            return;
        }
        if (password.isEmpty()) {
            UiAlerts.error(window(), "Contraseña vacía", "La contraseña no puede estar vacía.");
            return;
        }
        if (Path.of(input).toAbsolutePath().normalize().equals(Path.of(output).toAbsolutePath().normalize())) {
            UiAlerts.error(
                    window(),
                    "Ruta no válida",
                    "La salida debe ser distinta del fichero de entrada.");
            return;
        }

        Task<AesResult> task =
                new Task<>() {
                    @Override
                    protected AesResult call() throws Exception {
                        return encrypt
                                ? backend.encryptFile(input, output, password)
                                : backend.decryptFile(input, output, password);
                    }
                };

        running = true;
        updateControls();
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        statusLabel.setText(encrypt ? "Cifrando fichero…" : "Descifrando y verificando…");

        task.setOnSucceeded(
                event -> {
                    running = false;
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                    AesResult result = task.getValue();
                    if (encrypt) {
                        statusLabel.setText(
                                "Cifrado completado · "
                                        + result.inputBytes()
                                        + " bytes de entrada · salida: "
                                        + result.outputPath());
                    } else if (Boolean.TRUE.equals(result.integrityVerified())) {
                        statusLabel.setText(
                                "Integridad verificada · SHA-256: "
                                        + result.sha256()
                                        + " · "
                                        + result.outputBytes()
                                        + " bytes recuperados");
                    } else {
                        statusLabel.setText("Descifrado completado: " + result.outputPath());
                    }
                    updateControls();
                });
        task.setOnFailed(
                event -> {
                    running = false;
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                    Throwable failure = task.getException();
                    statusLabel.setText("Error: " + UiAlerts.messageOf(failure));
                    String message = UiAlerts.messageOf(failure);
                    if (failure instanceof BackendException backendFailure) {
                        message = backendFailure.getMessage();
                    }
                    UiAlerts.error(window(), "Operación AES", message);
                    updateControls();
                });

        Thread worker = new Thread(task, "aes-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void updateControls() {
        boolean enabled = backendReady && !running;
        encryptButton.setDisable(!enabled);
        decryptButton.setDisable(!enabled);
        inputField.setDisable(running);
        outputField.setDisable(running);
        passwordField.setDisable(running);
        inputBrowse.setDisable(running);
        outputBrowse.setDisable(running);
    }

    private String suggestOutput(Path input, boolean encrypt) {
        if (encrypt) {
            return input.toString() + ".enc";
        }
        String name = input.getFileName().toString();
        if (name.endsWith(".enc")) {
            return input.resolveSibling(name.substring(0, name.length() - 4)).toString();
        }
        return input.toString() + ".recuperado";
    }

    private Window window() {
        return view.getScene() == null ? null : view.getScene().getWindow();
    }
}
