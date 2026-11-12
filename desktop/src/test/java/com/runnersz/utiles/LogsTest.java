package com.runnersz.utiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas del registro: elección de carpeta por sistema operativo, escritura con nivel y fecha,
 * rotación configurada, nivel por propiedad, enmascarado de credenciales y lectura para el visor.
 *
 * @author Jairo y Alain
 */
class LogsTest {
  @AfterEach
  void cleanUp() {
    System.clearProperty(Logs.DIR_PROPERTY);
    System.clearProperty(Logs.LEVEL_PROPERTY);
    System.clearProperty(Logs.CONSOLE_PROPERTY);
    Logs.stop();
  }

  @Test
  void laCarpetaSeEligeConLaPropiedad(@TempDir Path dir) {
    System.setProperty(Logs.DIR_PROPERTY, dir.toString());
    assertEquals(dir.toAbsolutePath().normalize(), Logs.directory());
  }

  @Test
  void sinPropiedadSeUsaLaCarpetaDelUsuario() {
    System.clearProperty(Logs.DIR_PROPERTY);
    Path folder = Logs.directory();
    assertTrue(folder.isAbsolute(), folder.toString());
    assertEquals("logs", folder.getFileName().toString());
  }

  @Test
  void laCarpetaPorDefectoDependeDelSistemaOperativo() {
    assertEquals(
        Path.of("C:\\Users\\ana\\AppData\\Local", "RunnersZ", "logs"),
        Logs.defaultDirectory(
            "Windows 11", Map.of("LOCALAPPDATA", "C:\\Users\\ana\\AppData\\Local"), "C:\\Users\\ana"));
    assertEquals(
        Path.of("C:\\Users\\ana", "AppData", "Local", "RunnersZ", "logs"),
        Logs.defaultDirectory("Windows 10", Map.of(), "C:\\Users\\ana"));
    assertEquals(
        Path.of("/home/ana", "Library", "Logs", "RunnersZ"),
        Logs.defaultDirectory("Mac OS X", Map.of(), "/home/ana"));
    assertEquals(
        Path.of("/home/ana", ".local", "state", "runnersz", "logs"),
        Logs.defaultDirectory("Linux", Map.of(), "/home/ana"));
    assertEquals(
        Path.of("/var/state", "runnersz", "logs"),
        Logs.defaultDirectory("Linux", Map.of("XDG_STATE_HOME", "/var/state"), "/home/ana"));
  }

  @Test
  void elRegistroEscribeNivelFechaYMensaje(@TempDir Path dir) throws IOException {
    System.setProperty(Logs.DIR_PROPERTY, dir.toString());
    System.setProperty(Logs.CONSOLE_PROPERTY, "false");
    Logs.reset();

    Path file = Logs.start();
    Log.of(LogsTest.class).info("arranque de prueba");

    assertEquals(dir.toAbsolutePath().normalize().resolve(Logs.FILE_NAME), file);
    assertEquals(file, Logs.file());
    String content = Files.readString(file, StandardCharsets.UTF_8);
    assertTrue(content.contains("INFO"), content);
    assertTrue(content.contains("arranque de prueba"), content);
    assertTrue(
        content.matches("(?s).*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*"), content);
  }

  @Test
  void elNivelSeSubeConLaPropiedad(@TempDir Path dir) throws IOException {
    System.setProperty(Logs.DIR_PROPERTY, dir.toString());
    System.setProperty(Logs.CONSOLE_PROPERTY, "false");
    System.setProperty(Logs.LEVEL_PROPERTY, "DEBUG");
    Logs.reset();

    Path file = Logs.start();
    Log.of(LogsTest.class).debug("detalle fino con token=abc123");

    String content = Files.readString(file, StandardCharsets.UTF_8);
    assertTrue(content.contains("detalle fino"), content);
    assertTrue(content.contains("token=***"), content);
    assertFalse(content.contains("abc123"), content);
  }

  @Test
  void elErrorSeRegistraConSuTraza(@TempDir Path dir) throws IOException {
    System.setProperty(Logs.DIR_PROPERTY, dir.toString());
    System.setProperty(Logs.CONSOLE_PROPERTY, "false");
    Logs.reset();

    Path file = Logs.start();
    Log.of(LogsTest.class)
        .error("fallo con password: secreta123", new IllegalStateException("motivo interno"));

    String content = Files.readString(file, StandardCharsets.UTF_8);
    assertTrue(content.contains("ERROR"), content);
    assertTrue(content.contains("motivo interno"), content);
    assertFalse(content.contains("secreta123"), content);
  }

  @Test
  void losSecretosSeEnmascaran() {
    assertEquals("token=***", Logs.redact("token=abc123"));
    assertEquals("Authorization: Bearer ***", Logs.redact("Authorization: Bearer abc.def.ghi"));
    assertFalse(Logs.redact("password: hola mundo").contains("hola mundo"));
    assertEquals("", Logs.redact(null));
  }

  @Test
  void elVisorLeeSoloLasUltimasLineas(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("runnersz.log");
    Files.write(file, lines(250));

    String tail = Logs.lastLines(file, Logs.VIEWER_LINES);
    assertTrue(tail.startsWith("linea 51"), tail.substring(0, Math.min(20, tail.length())));
    assertTrue(tail.endsWith("linea 250"), tail.substring(Math.max(0, tail.length() - 20)));

    assertEquals("", Logs.lastLines(dir.resolve("fantasma.log"), Logs.VIEWER_LINES));
    assertEquals("", Logs.lastLines(file, 0));
    assertEquals("", Logs.lastLines(null, Logs.VIEWER_LINES));
  }

  @Test
  void unaRutaImposibleSeRechazaYSeUsaUnaCarpetaTemporal(@TempDir Path dir) throws IOException {
    Path blocker = Files.writeString(dir.resolve("bloqueo"), "x");
    assertThrows(IOException.class, () -> Logs.install(blocker.resolve("logs")));

    System.setProperty(Logs.DIR_PROPERTY, blocker.resolve("logs").toString());
    System.setProperty(Logs.CONSOLE_PROPERTY, "false");
    Logs.reset();
    Path file = Logs.start();
    assertTrue(Files.exists(file), file.toString());
    assertTrue(file.toString().contains("runnersz-logs"), file.toString());
  }

  private static List<String> lines(int count) {
    List<String> result = new ArrayList<>(count);
    for (int i = 1; i <= count; i++) result.add("linea " + i);
    return result;
  }
}
