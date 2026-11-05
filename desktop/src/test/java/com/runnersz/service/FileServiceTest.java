package com.runnersz.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas del acceso a disco: temporales, publicación y textos.
 *
 * @author RunnersZ
 */
class FileServiceTest {
  @TempDir Path dir;

  private final FileService files = new FileService();

  @Test
  void creaElDirectorioDeDestinoYElTemporal() throws IOException {
    Path target = dir.resolve("a/b/c/salida.bin");
    Path temporary = files.prepareTemporary(target);

    assertTrue(Files.isDirectory(target.getParent()));
    assertEquals(target.getParent(), temporary.getParent());
    assertTrue(Files.exists(temporary));
    files.deleteQuietly(temporary);
    assertFalse(Files.exists(temporary));
  }

  @Test
  void publicaConReemplazo() throws IOException {
    Path target = Files.writeString(dir.resolve("salida.txt"), "antiguo");
    Path temporary = files.prepareTemporary(target);
    Files.writeString(temporary, "nuevo");

    files.publish(temporary, target);

    assertEquals("nuevo", Files.readString(target));
    assertFalse(Files.exists(temporary));
  }

  @Test
  void escribeTextoDeFormaAtomica() throws IOException {
    Path target = dir.resolve("texto.txt");
    files.writeText(target, "áéí");

    assertEquals("áéí", files.readText(target));
    try (Stream<Path> entries = Files.list(dir)) {
      assertTrue(entries.noneMatch(path -> path.getFileName().toString().startsWith(".runnersz-")));
    }
  }

  @Test
  void borrarUnTemporalInexistenteNoFalla() {
    files.deleteQuietly(null);
    files.deleteQuietly(dir.resolve("no-existe.part"));
  }
}
