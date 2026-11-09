package com.runnersz.reto2.desktop.controller;

import com.runnersz.reto2.desktop.service.BackendException;
import com.runnersz.reto2.desktop.service.BackendService;
import com.runnersz.reto2.desktop.service.FileTextService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class VigenereController {
    private final BackendService backend;
    private final FileTextService fileTextService;
    private final VBox view = new VBox(16);
    private final TextArea inputArea = new TextArea();
    private final TextArea resultArea = new TextArea();
    private final TextField keyField = new TextField();
    private final Button encryptButton = new Button("Cifrar");
    private final Button decryptButton = new Button("Descifrar");
    private final Button loadButton = new Button("Cargar .txt");
    private final Button saveButton = new Button("Guardar .txt");
    private final Label statusLabel = new Label("Escribe un mensaje o carga un .txt.");
    private boolean backendReady;
    private boolean running;

    public VigenereController(BackendService backend, FileTextService fileTextService) {
        this.backend = backend;
        this.fileTextService = fileTextService;
        build();
    }

    private void build() {
        Label title = new Label("Mensajes (Vigenère)");
        title.getStyleClass().add("section-title");
        Label description =
                new Label(
                        "Alfabeto A-Z. Se conservan mayúsculas, minúsculas, espacios, "
                                + "signos, Ñ y tildes sin avanzar la clave.");
        description.getStyleClass().add("section-description");
        description.setWrapText(true);

        inputArea.setPromptText("Texto de entrada");
        resultArea.setPromptText("Resultado");
        inputArea.setPrefRowCount(8);
        resultArea.setPrefRowCount(8);
        resultArea.setEditable(true);
        VBox.setVgrow(inputArea, Priority.ALWAYS);
        VBox.setVgrow(resultArea, Priority.ALWAYS);

        keyField.setPromptText("Clave (solo letras A-Z)");
        encryptButton.getStyleClass().add("button-primary");
        decryptButton.getStyleClass().add("button-primary");
        loadButton.getStyleClass().add("button-secondary");
        saveButton.getStyleClass().add("button-secondary");

        HBox keyRow = new HBox(10, new Label("Clave"), keyField);
        HBox.setHgrow(keyField, Priority.ALWAYS);
        keyRow.setAlignment(Pos.CENTER_LEFT);

        HBox actions = new HBox(10, encryptButton, decryptButton, loadButton, saveButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        statusLabel.getStyleClass().add("status");
        statusLabel.setWrapText(true);

        HBox columns = new HBox(14);
        VBox inputColumn = new VBox(8, new Label("Entrada"), inputArea);
        VBox resultColumn = new VBox(8, new Label("Resultado"), resultArea);
        HBox.setHgrow(inputColumn, Priority.ALWAYS);
        HBox.setHgrow(resultColumn, Priority.ALWAYS);
        columns.getChildren().addAll(inputColumn, resultColumn);

        VBox card = new VBox(14, keyRow, columns, actions, statusLabel);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));

        encryptButton.setOnAction(event -> runOperation(true));
        decryptButton.setOnAction(event -> runOperation(false));
        loadButton.setOnAction(event -> loadText());
        saveButton.setOnAction(event -> saveText());

        view.getChildren().addAll(title, description, card);
        VBox.setVgrow(card, Priority.ALWAYS);
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

    private void runOperation(boolean encrypt) {
        if (!backendReady) {
            UiAlerts.error(window(), "Backend no disponible", "Espera a que el backend esté listo.");
            return;
        }
        String key = keyField.getText().trim();
        if (key.isEmpty()) {
            UiAlerts.error(window(), "Clave vacía", "La clave debe contener letras A-Z.");
            return;
        }
        String text = inputArea.getText();
        Task<String> task =
                new Task<>() {
                    @Override
                    protected String call() throws Exception {
                        return backend.vigenere(text, key, encrypt);
                    }
                };
        running = true;
        updateControls();
        statusLabel.setText(encrypt ? "Cifrando mensaje…" : "Descifrando mensaje…");
        task.setOnSucceeded(
                event -> {
                    running = false;
                    resultArea.setText(task.getValue());
                    statusLabel.setText(
                            (encrypt ? "Mensaje cifrado" : "Mensaje descifrado")
                                    + " · "
                                    + task.getValue().length()
                                    + " caracteres");
                    updateControls();
                });
        task.setOnFailed(
                event -> {
                    running = false;
                    Throwable failure = task.getException();
                    String message =
                            failure instanceof BackendException
                                    ? failure.getMessage()
                                    : UiAlerts.messageOf(failure);
                    statusLabel.setText("Error: " + message);
                    UiAlerts.error(window(), "Operación Vigenère", message);
                    updateControls();
                });
        Thread worker = new Thread(task, "vigenere-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void loadText() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Cargar mensaje");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto", "*.txt"));
        File selected = chooser.showOpenDialog(window());
        if (selected == null) {
            return;
        }
        try {
            inputArea.setText(fileTextService.readUtf8(selected.toPath()));
            statusLabel.setText("Mensaje cargado: " + selected.getName());
        } catch (IOException exception) {
            statusLabel.setText("No se pudo leer el mensaje.");
            UiAlerts.error(window(), "Lectura de fichero", UiAlerts.messageOf(exception));
        }
    }

    private void saveText() {
        if (resultArea.getText().isEmpty()) {
            UiAlerts.error(window(), "Sin resultado", "No hay resultado que guardar.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar mensaje");
        chooser.setInitialFileName("mensaje.txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto", "*.txt"));
        File selected = chooser.showSaveDialog(window());
        if (selected == null) {
            return;
        }
        try {
            fileTextService.writeUtf8(selected.toPath(), resultArea.getText());
            statusLabel.setText("Mensaje guardado: " + selected.getAbsolutePath());
        } catch (IOException exception) {
            statusLabel.setText("No se pudo guardar el mensaje.");
            UiAlerts.error(window(), "Escritura de fichero", UiAlerts.messageOf(exception));
        }
    }

    private void updateControls() {
        boolean enabled = backendReady && !running;
        encryptButton.setDisable(!enabled);
        decryptButton.setDisable(!enabled);
        keyField.setDisable(running);
        inputArea.setDisable(running);
        resultArea.setDisable(running);
        loadButton.setDisable(running);
        saveButton.setDisable(running);
    }

    private Window window() {
        return view.getScene() == null ? null : view.getScene().getWindow();
    }
}
