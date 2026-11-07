package com.runnersz.service;

import com.runnersz.utiles.I18n;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Caso de uso de ficheros: validar → transferir a un temporal → publicar. No depende de JavaFX y no
 * implementa algoritmos criptográficos.
 *
 * @author Jairo y Alain
 */
public final class FileOperationService {
  private final ApiClient api;
  private final FileService files;

  /**
   * Crea el caso de uso con el cliente HTTP y el acceso a disco indicados.
   *
   * @param api cliente de la API del servidor
   * @param files servicio de temporales y publicación
   */
  public FileOperationService(ApiClient api, FileService files) {
    this.api = api;
    this.files = files;
  }

  /**
   * Solo publica la salida cuando la API ha completado todas sus validaciones.
   *
   * @param input fichero que se envía al servidor
   * @param output destino del resultado
   * @param password contraseña AES de la operación
   * @param encrypt {@code true} para cifrar, {@code false} para descifrar
   * @param progress aviso de bytes transferidos
   * @throws java.io.IOException si falla la API, una validación o el disco
   * @throws InterruptedException si la operación se cancela
   */
  public void execute(
      Path input, Path output, String password, boolean encrypt, ApiClient.Progress progress)
      throws IOException, InterruptedException {
    Path source = input.toAbsolutePath().normalize();
    Path target = output.toAbsolutePath().normalize();

    if (Files.isDirectory(target)) throw I18n.failure("error.outputDirectory");
    // isSameFile cubre también enlaces y variantes de una misma ruta existente.
    if (source.equals(target) || (Files.exists(target) && Files.isSameFile(source, target))) {
      throw I18n.failure("error.original");
    }
    if (!Files.isRegularFile(source)) throw I18n.failure("error.notFound");
    if (Files.size(source) == 0) throw I18n.failure("error.emptyFile");

    Path temporary = files.prepareTemporary(target);
    try {
      api.transferFile(source, temporary, password, encrypt, progress);
      files.publish(temporary, target);
    } finally {
      files.deleteQuietly(temporary);
    }
  }
}
