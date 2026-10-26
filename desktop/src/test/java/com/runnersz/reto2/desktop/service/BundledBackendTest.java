package com.runnersz.reto2.desktop.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BundledBackendTest {
    @Test
    void extractsBackendAndPinnedDependencies() throws Exception {
        Path backend = BundledBackend.extract();
        assertTrue(Files.readString(backend.resolve("requirements.txt")).contains("cryptography=="));
        assertTrue(Files.readString(backend.resolve("src/reto2_backend/__main__.py")).contains("port"));
        assertTrue(Files.isRegularFile(backend.resolve("src/reto2_backend/aes_file.py")));
        assertTrue(Files.isRegularFile(backend.resolve("src/reto2_backend/caesar.py")));
        assertNotEquals(backend, BundledBackend.extract());
    }
}
