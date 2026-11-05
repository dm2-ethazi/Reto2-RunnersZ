package com.runnersz.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runnersz.model.ConnectionSettings;
import com.runnersz.support.StubApi;
import com.runnersz.utiles.I18n;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Pruebas del caso de uso de ficheros y del acceso a disco. */
class FileOperationServiceTest {
  @TempDir Path dir;

  private StubApi api;
  private FileOperationService service;

  @BeforeEach
  void start() throws IOException {
    I18n.setLanguage("es");
    api = new StubApi();
    service =
        new FileOperationService(
            new ApiClient(new ConnectionSettings(api.baseUrl(), "token")), new FileService());
  }

  @AfterEach
  void stop() {
    api.close();
  }

  @Test
  void cifraYPublicaElResultadoSinDejarTemporales() throws Exception {
    Path input = Files.writeString(dir.resolve("nota.txt"), "contenido");
    Path output = dir.resolve("sub/nota.txt.enc");
    api.respond(200, "cifrado".getBytes(UTF_8));

    service.execute(input, output, "clave", true, (done, total) -> {});

    assertEquals("cifrado", Files.readString(output));
    assertEquals("contenido", Files.readString(input));
    try (Stream<Path> entries = Files.list(output.getParent())) {
      assertTrue(entries.noneMatch(path -> path.getFileName().toString().startsWith(".runnersz-")));
    }
  }

  @Test
  void noPublicaNadaSiFaltaLaIntegridad() throws Exception {
    Path input = Files.write(dir.resolve("secreto.enc"), new byte[] {4, 5, 6});
    Path output = dir.resolve("secreto.txt");
    api.respond(200, "datos".getBytes(UTF_8));

    assertThrows(
        IOException.class, () -> service.execute(input, output, "clave", false, (d, t) -> {}));
    assertFalse(Files.exists(output));
    try (Stream<Path> entries = Files.list(dir)) {
      assertTrue(entries.noneMatch(path -> path.getFileName().toString().startsWith(".runnersz-")));
    }
  }

  @Test
  void rechazaUnFicheroVacio() throws Exception {
    Path input = Files.createFile(dir.resolve("vacio.bin"));
    IOException error =
        assertThrows(
            IOException.class,
            () -> service.execute(input, dir.resolve("salida"), "clave", true, (d, t) -> {}));
    assertTrue(error.getMessage().contains("vacío"), error.getMessage());
  }

  @Test
  void rechazaUnaSalidaQueEsCarpeta() throws Exception {
    Path input = Files.writeString(dir.resolve("nota.txt"), "x");
    Path folder = Files.createDirectory(dir.resolve("carpeta"));
    IOException error =
        assertThrows(
            IOException.class, () -> service.execute(input, folder, "clave", true, (d, t) -> {}));
    assertTrue(error.getMessage().contains("carpeta"), error.getMessage());
  }

  @Test
  void rechazaSobrescribirElOriginal() throws Exception {
    Path input = Files.writeString(dir.resolve("nota.txt"), "x");
    IOException error =
        assertThrows(
            IOException.class, () -> service.execute(input, input, "clave", true, (d, t) -> {}));
    assertTrue(error.getMessage().contains("original"), error.getMessage());
  }

  @Test
  void rechazaUnFicheroInexistente() {
    Path missing = dir.resolve("no-existe.txt");
    IOException error =
        assertThrows(
            IOException.class,
            () -> service.execute(missing, dir.resolve("salida"), "clave", true, (d, t) -> {}));
    assertTrue(error.getMessage().contains("no existe"), error.getMessage());
  }
}
