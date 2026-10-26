package com.runnersz.reto2.desktop.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTextService {
    public String readUtf8(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        }
    }

    public void writeUtf8(Path path, String content) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null && !Files.isDirectory(parent)) {
            throw new IOException("La carpeta de destino no existe: " + parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }
}
