package com.runnersz.reto2.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTextServiceTest {
    private final FileTextService service = new FileTextService();

    @Test
    void escribeYLeeUtf8(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("mensaje ñ.txt");
        service.writeUtf8(file, "Hola ñandú\nsegunda línea");
        assertEquals("Hola ñandú\nsegunda línea", service.readUtf8(file));
    }

    @Test
    void escribirEnCarpetaInexistenteFalla(@TempDir Path directory) {
        Path file = directory.resolve("no").resolve("mensaje.txt");
        IOException exception =
                assertThrows(IOException.class, () -> service.writeUtf8(file, "datos"));
        assertTrue(exception.getMessage().contains("carpeta"));
    }

    @Test
    void leerFicheroInexistenteFalla(@TempDir Path directory) {
        assertThrows(
                IOException.class, () -> service.readUtf8(directory.resolve("no-existe.txt")));
    }

    @Test
    void lecturaDetectaUtf8Invalido(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("invalido.txt");
        Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28});
        assertThrows(IOException.class, () -> service.readUtf8(file));
        assertEquals(2, Files.size(file));
    }

    @Test
    void escribeSinPadreAbsoluto(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("simple.txt");
        service.writeUtf8(file, "ok");
        assertEquals("ok", Files.readString(file, StandardCharsets.UTF_8));
    }
}
