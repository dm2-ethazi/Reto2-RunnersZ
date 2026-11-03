package com.runnersz;

import javafx.application.Application;

/** Entrada del fat JAR: inicia JavaFX sin exigir módulos externos al lanzador JVM. */
public final class Launcher {
  private Launcher() {}

  public static void main(String[] args) {
    Application.launch(Main.class, args);
  }
}
