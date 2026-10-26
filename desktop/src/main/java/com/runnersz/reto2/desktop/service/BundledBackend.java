package com.runnersz.reto2.desktop.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class BundledBackend {
    private BundledBackend() {}

    static Path extract() throws IOException {
        Path root = Files.createTempDirectory("reto2-backend-");
        root.toFile().deleteOnExit();
        Path source = Files.createDirectory(root.resolve("src"));
        source.toFile().deleteOnExit();
        Path module = Files.createDirectory(source.resolve("reto2_backend"));
        module.toFile().deleteOnExit();
        List<String> resources = List.of(
                "requirements.txt", "src/reto2_backend/__init__.py",
                "src/reto2_backend/__main__.py", "src/reto2_backend/aes_file.py",
                "src/reto2_backend/api.py", "src/reto2_backend/caesar.py",
                "src/reto2_backend/enc_format.py", "src/reto2_backend/errors.py",
                "src/reto2_backend/vigenere.py");
        for (String resource : resources) {
            Path target = root.resolve(resource);
            target.toFile().deleteOnExit();
            try (InputStream stream = BundledBackend.class.getResourceAsStream("/backend/" + resource)) {
                if (stream == null) {
                    throw new IOException("Missing bundled backend resource: " + resource);
                }
                Files.copy(stream, target);
            }
        }
        return root;
    }
}
