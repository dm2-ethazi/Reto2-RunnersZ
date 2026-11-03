package com.runnersz.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Acceso a disco compartido por AES y Vigenère. La salida se prepara junto al destino: un fallo
 * previo a moverla no publica un fichero incompleto. finally elimina el temporal tanto con éxito
 * como con error.
 */
public final class FileService {
  /** Lee los bytes sin reinterpretarlos como texto. */
  public byte[] readBytes(Path source) throws IOException {
    return Files.readAllBytes(source);
  }

  /** El formato de los mensajes del contrato es UTF-8. */
  public String readText(Path source) throws IOException {
    return Files.readString(source, StandardCharsets.UTF_8);
  }

  public void writeText(Path destination, String text) throws IOException {
    writeBytes(destination, text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * El controlador debe haber confirmado previamente cualquier reemplazo. Se intenta un movimiento
   * atómico; los sistemas que no lo admitan usan un movimiento normal, conservando la limpieza del
   * temporal.
   */
  public void writeBytes(Path destination, byte[] bytes) throws IOException {
    Path target = destination.toAbsolutePath().normalize();
    Path parent = target.getParent();
    Files.createDirectories(parent);
    Path temporary = Files.createTempFile(parent, ".runnersz-", ".part");
    try {
      Files.write(temporary, bytes);
      try {
        Files.move(
            temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ex) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
