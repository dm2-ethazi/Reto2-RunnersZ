package com.runnersz.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Acceso a disco compartido por AES, Vigenère y César. La salida se prepara en un temporal junto al
 * destino: un fallo previo a moverlo no publica un fichero incompleto.
 *
 * @author Jairo y Alain
 */
public final class FileService {
  /**
   * El formato de los mensajes del contrato es UTF-8.
   *
   * @param source fichero de texto que se lee
   * @return contenido completo del fichero
   * @throws IOException si el fichero no existe o no se puede leer
   */
  public String readText(Path source) throws IOException {
    return Files.readString(source, StandardCharsets.UTF_8);
  }

  /**
   * Guarda el texto de forma atómica: se escribe en un temporal y se publica al final.
   *
   * @param destination fichero de salida
   * @param text contenido que se guarda
   * @throws IOException si no se puede escribir
   */
  public void writeText(Path destination, String text) throws IOException {
    Path temporary = prepareTemporary(destination);
    try {
      Files.writeString(temporary, text, StandardCharsets.UTF_8);
      publish(temporary, destination);
    } finally {
      deleteQuietly(temporary);
    }
  }

  /**
   * Crea el directorio de destino si falta y devuelve un temporal en la misma unidad.
   *
   * @param destination ruta final que se quiere escribir
   * @return fichero temporal que el llamador debe publicar o borrar
   * @throws IOException si no se puede crear el directorio o el temporal
   */
  public Path prepareTemporary(Path destination) throws IOException {
    Path target = destination.toAbsolutePath().normalize();
    Path parent = target.getParent();
    if (parent == null) throw new IOException("destino sin carpeta");
    Files.createDirectories(parent);
    return Files.createTempFile(parent, ".runnersz-", ".part");
  }

  /**
   * El controlador debe haber confirmado previamente cualquier reemplazo. Se intenta un movimiento
   * atómico; los sistemas que no lo admitan usan un movimiento normal.
   */
  public void publish(Path temporary, Path destination) throws IOException {
    Path target = destination.toAbsolutePath().normalize();
    try {
      Files.move(
          temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException ex) {
      Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Limpieza de temporales; nunca oculta el error original.
   *
   * @param path fichero que se intenta borrar; puede ser {@code null}
   */
  public void deleteQuietly(Path path) {
    if (path == null) return;
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // El temporal se reintenta en la siguiente limpieza del sistema.
    }
  }
}
