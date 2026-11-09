package com.runnersz.reto2.desktop.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public final class UiAlerts {
    private UiAlerts() {}

    public static void error(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean retry(Window owner, String title, String message) {
        ButtonType retry = new ButtonType("Reintentar");
        ButtonType close = new ButtonType("Cerrar aplicación");
        Alert alert = new Alert(Alert.AlertType.ERROR, message, retry, close);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
        return alert.getResult() == retry;
    }

    public static String messageOf(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getMessage() == null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }
}
