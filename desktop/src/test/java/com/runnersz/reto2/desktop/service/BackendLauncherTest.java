package com.runnersz.reto2.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BackendLauncherTest {
    private static Path createBackend(Path root) throws IOException {
        Path backend = root.resolve("backend");
        Files.createDirectories(backend.resolve("src/reto2_backend"));
        Files.writeString(backend.resolve("src/reto2_backend/__main__.py"), "");
        Files.writeString(backend.resolve("pyproject.toml"), "");
        return backend;
    }

    private static Process sleeper(long millis) throws IOException {
        String executable =
                Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java")
                        .toString();
        String classpath = Path.of("target", "test-classes").toAbsolutePath().toString();
        return new ProcessBuilder(
                        executable,
                        "-cp",
                        classpath,
                        SleepMain.class.getName(),
                        Long.toString(millis))
                .start();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    @Test
    void encuentraBackendEnCarpetaDeTrabajo(@TempDir Path root) throws IOException {
        Path backend = createBackend(root);
        Path working = Files.createDirectories(root.resolve("desktop"));
        Path other = Files.createDirectories(root.resolve("otro"));
        assertEquals(backend, BackendLauncher.resolveBackendHome(working, other, null));
    }

    @Test
    void encuentraBackendMedianteOverride(@TempDir Path root) throws IOException {
        Path backend = createBackend(root.resolve("instalacion"));
        assertEquals(
                backend,
                BackendLauncher.resolveBackendHome(
                        root.resolve("vacio"), root.resolve("vacio2"), backend.toString()));
    }

    @Test
    void devuelveNullSiNoHayBackend(@TempDir Path root) {
        assertNull(
                BackendLauncher.resolveBackendHome(
                        root.resolve("a"), root.resolve("b"), root.resolve("no").toString()));
    }

    @Test
    void resuelvePythonDeVenv(@TempDir Path root) throws IOException {
        Path venvPython;
        if (isWindows()) {
            venvPython = root.resolve(".venv/Scripts/python.exe");
        } else {
            venvPython = root.resolve(".venv/bin/python");
        }
        Files.createDirectories(venvPython.getParent());
        Files.writeString(venvPython, "");
        assertEquals(venvPython, BackendLauncher.resolvePython(root, null));
        assertEquals(
                Path.of("python-personalizado"),
                BackendLauncher.resolvePython(root, "python-personalizado"));
    }

    @Test
    void resuelvePythonDelSistemaSinVenv(@TempDir Path root) {
        Path python = BackendLauncher.resolvePython(root, null);
        assertNotNull(python);
        assertTrue(
                python.getFileName().toString().equals("python")
                        || python.getFileName().toString().equals("python3"));
    }

    @Test
    void encuentraPuertoLibre() throws Exception {
        int first = BackendLauncher.findFreePort();
        int second = BackendLauncher.findFreePort();
        assertTrue(first > 0 && first <= 65535);
        assertTrue(second > 0 && second <= 65535);
    }

    @Test
    void probeHttpDetectaEstadoOk() throws Exception {
        HttpServer server =
                HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(
                "/health",
                exchange -> {
                    byte[] body = "{\"estado\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, body.length);
                    try (var output = exchange.getResponseBody()) {
                        output.write(body);
                    }
                });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort();
            assertTrue(BackendLauncher.probeHttp(url));
        } finally {
            server.stop(0);
        }
        assertFalse(BackendLauncher.probeHttp("http://127.0.0.1:1"));
        assertFalse(BackendLauncher.probeHttp("no-es-una-url"));
    }

    @Test
    void arrancaYDetieneProceso(@TempDir Path root) throws Exception {
        createBackend(root);
        AtomicReference<Process> process = new AtomicReference<>();
        BackendLauncher.ProcessStarter starter =
                builder -> {
                    Process started = sleeper(30_000);
                    process.set(started);
                    return started;
                };
        BackendLauncher launcher =
                new BackendLauncher(starter, url -> true, 1_000, 20, root, root);
        try {
            assertNull(launcher.getBaseUrl());
            String url = launcher.start();
            assertTrue(url.startsWith("http://127.0.0.1:"));
            assertTrue(launcher.isRunning());
            assertTrue(process.get().isAlive());
            assertEquals(url, launcher.start());
        } finally {
            launcher.stop();
        }
        assertFalse(launcher.isRunning());
        assertNull(launcher.getBaseUrl());
        assertFalse(process.get().isAlive());
        launcher.close();
    }

    @Test
    void timeoutCuandoNoResponde(@TempDir Path root) throws Exception {
        createBackend(root);
        AtomicReference<Process> process = new AtomicReference<>();
        BackendLauncher.ProcessStarter starter =
                builder -> {
                    Process started = sleeper(30_000);
                    process.set(started);
                    return started;
                };
        BackendLauncher launcher =
                new BackendLauncher(starter, url -> false, 150, 20, root, root);
        try {
            BackendException exception = assertThrows(BackendException.class, launcher::start);
            assertEquals("BACKEND_SIN_RESPUESTA", exception.getCode());
        } finally {
            launcher.stop();
        }
        assertFalse(process.get().isAlive());
    }

    @Test
    void detectaProcesoSinRespuestaYSinVida(@TempDir Path root) throws Exception {
        createBackend(root);
        AtomicReference<Process> process = new AtomicReference<>();
        BackendLauncher.ProcessStarter starter =
                builder -> {
                    Process started = sleeper(50);
                    process.set(started);
                    return started;
                };
        BackendLauncher launcher =
                new BackendLauncher(starter, url -> false, 2_000, 10, root, root);
        try {
            BackendException exception = assertThrows(BackendException.class, launcher::start);
            assertEquals("BACKEND_FINALIZO", exception.getCode());
        } finally {
            launcher.stop();
        }
        assertFalse(process.get().isAlive());
    }

    @Test
    void errorAlEjecutarPython(@TempDir Path root) throws IOException {
        createBackend(root);
        BackendLauncher.ProcessStarter starter =
                builder -> {
                    throw new IOException("python no disponible");
                };
        BackendLauncher launcher =
                new BackendLauncher(starter, url -> true, 100, 10, root, root);
        BackendException exception = assertThrows(BackendException.class, launcher::start);
        assertEquals("BACKEND_NO_INICIADO", exception.getCode());
    }

    @Test
    void errorSiNoEncuentraBackend(@TempDir Path root) {
        BackendLauncher launcher =
                new BackendLauncher(
                        builder -> sleeper(1_000), url -> true, 100, 10, root, root);
        BackendException exception = assertThrows(BackendException.class, launcher::start);
        assertEquals("BACKEND_NO_ENCONTRADO", exception.getCode());
    }

    @Test
    void closeSinProcesoNoFalla(@TempDir Path root) {
        BackendLauncher launcher =
                new BackendLauncher(
                        builder -> sleeper(1_000), url -> true, 100, 10, root, root);
        launcher.stop();
        launcher.close();
        assertFalse(launcher.isRunning());
    }
}
