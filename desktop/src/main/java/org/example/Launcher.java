package org.example;

import javafx.application.Application;

/** Ordinary JVM entry point so the executable JAR can bootstrap JavaFX itself. */
public final class Launcher {
    private Launcher() { }

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
