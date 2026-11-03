package com.runnersz.service;

import com.runnersz.utiles.I18n;
import com.runnersz.model.FileResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Caso de uso de ficheros: lectura → API → escritura. No depende de JavaFX y no implementa
 * algoritmos criptográficos.
 */
public final class FileOperationService {
  private final ApiClient api;
  private final FileService files;

  public FileOperationService(ApiClient api, FileService files) {
    this.api = api;
    this.files = files;
  }

  /** Solo publica la salida cuando la API ha completado todas sus validaciones. */
  public void execute(Path input, Path output, String password, boolean encrypt)
      throws IOException, InterruptedException {
    Path source = input.toAbsolutePath().normalize();
    Path target = output.toAbsolutePath().normalize();
    // isSameFile cubre también enlaces y variantes de una misma ruta existente.
    if (source.equals(target) || (Files.exists(target) && Files.isSameFile(source, target))) {
      throw I18n.failure("error.original");
    }
    byte[] bytes = files.readBytes(source);
    if (bytes.length == 0) {
      throw I18n.failure("error.emptyFile");
    }
    FileResult result =
        api.fileOperation(bytes, source.getFileName().toString(), password, encrypt);
    files.writeBytes(target, result.bytes());
  }
}
