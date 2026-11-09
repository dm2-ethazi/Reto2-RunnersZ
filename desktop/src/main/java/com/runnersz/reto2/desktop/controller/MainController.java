package com.runnersz.reto2.desktop.controller;

import com.runnersz.reto2.desktop.service.BackendLauncher;
import com.runnersz.reto2.desktop.service.BackendService;
import com.runnersz.reto2.desktop.service.FileTextService;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class MainController {
    private final BackendLauncher launcher;
    private final BackendService backend;
    private final FileTextService fileTextService;
    private final BorderPane root = new BorderPane();
    private final Label backendStatus = new Label("Backend local: iniciando…");
    private final Button reconnectButton = new Button("Reintentar backend");
    private final AesController aesController;
    private final VigenereController vigenereController;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private Task<String> connectTask;

    public MainController() {
        this(new BackendLauncher(), new BackendService(), new FileTextService());
    }

    MainController(
            BackendLauncher launcher, BackendService backend, FileTextService fileTextService) {
        this.launcher = launcher;
        this.backend = backend;
        this.fileTextService = fileTextService;
        this.aesController = new AesController(backend);
        this.vigenereController = new VigenereController(backend, fileTextService);
        build();
        Runtime.getRuntime()
                .addShutdownHook(new Thread(this::shutdown, "reto2-desktop-shutdown"));
    }

    public Parent getView() {
        return root;
    }

    public void connectBackend() {
        if (closed.get() || (connectTask != null && connectTask.isRunning())) {
            return;
        }
        setReady(false);
        backendStatus.setText("Arrancando backend local…");
        reconnectButton.setDisable(true);
        connectTask =
                new Task<>() {
                    @Override
                    protected String call() throws Exception {
                        return launcher.start();
                    }
                };
        connectTask.setOnSucceeded(
                event -> {
                    reconnectButton.setDisable(false);
                    if (closed.get()) {
                        launcher.stop();
                        return;
                    }
                    String url = connectTask.getValue();
                    backend.setBaseUrl(url);
                    backendStatus.setText("Backend local listo · " + url);
                    reconnectButton.setText("Reiniciar backend");
                    setReady(true);
                });
        connectTask.setOnFailed(
                event -> {
                    reconnectButton.setDisable(false);
                    backend.setBaseUrl(null);
                    setReady(false);
                    reconnectButton.setText("Reintentar backend");
                    String message = UiAlerts.messageOf(connectTask.getException());
                    backendStatus.setText("Backend local no disponible");
                    if (closed.get()) {
                        return;
                    }
                    boolean retry =
                            UiAlerts.retry(
                                    window(),
                                    "Backend no disponible",
                                    message
                                            + "\n\nComprueba que Python 3.12 y las dependencias "
                                            + "estén preparados con scripts/prepare_python.py.");
                    if (retry) {
                        connectBackend();
                    } else {
                        shutdown();
                        Platform.exit();
                    }
                });
        Thread thread = new Thread(connectTask, "backend-start");
        thread.setDaemon(true);
        thread.start();
    }

    public void shutdown() {
        if (closed.compareAndSet(false, true)) {
            launcher.stop();
        }
    }

    private void build() {
        Label title = new Label("RunnersZ");
        title.getStyleClass().add("brand-title");
        Label subtitle = new Label("Cifrado de mensajes y ficheros");
        subtitle.getStyleClass().add("brand-subtitle");
        VBox brand = new VBox(2, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        backendStatus.getStyleClass().add("backend-status");
        backendStatus.setWrapText(true);
        reconnectButton.getStyleClass().add("button-secondary");
        reconnectButton.setOnAction(event -> restartBackend());
        VBox statusBox = new VBox(4, backendStatus, reconnectButton);
        statusBox.setAlignment(Pos.CENTER_RIGHT);

        HBox topBar = new HBox(18, brand, spacer, statusBox);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 22, 16, 22));
        topBar.getStyleClass().add("top-bar");

        Tab aesTab = new Tab("Ficheros (AES)", aesController.getView());
        Tab vigenereTab = new Tab("Mensajes (Vigenère)", vigenereController.getView());
        TabPane tabs = new TabPane(aesTab, vigenereTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("main-tabs");

        root.setTop(topBar);
        root.setCenter(tabs);
        root.getStyleClass().add("app-root");
        setReady(false);
    }

    private void restartBackend() {
        launcher.stop();
        backend.setBaseUrl(null);
        connectBackend();
    }

    private void setReady(boolean ready) {
        aesController.setBackendReady(ready);
        vigenereController.setBackendReady(ready);
        backendStatus.getStyleClass().removeAll("status-ok", "status-error");
        backendStatus.getStyleClass().add(ready ? "status-ok" : "status-error");
    }

    private Window window() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }
}
