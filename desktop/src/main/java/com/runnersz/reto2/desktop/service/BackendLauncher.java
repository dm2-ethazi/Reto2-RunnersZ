package com.runnersz.reto2.desktop.service;

import com.runnersz.reto2.desktop.util.Json;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class BackendLauncher implements AutoCloseable {
    @FunctionalInterface
    interface ProcessStarter {
        Process start(ProcessBuilder builder) throws IOException;
    }

    @FunctionalInterface
    interface HealthProbe {
        boolean isHealthy(String baseUrl);
    }

    private static final String BACKEND_MARKER = "src/reto2_backend/__main__.py";
    private static final long DEFAULT_TIMEOUT_MILLIS = 30_000;
    private static final long DEFAULT_POLL_MILLIS = 200;

    private final ProcessStarter starter;
    private final HealthProbe probe;
    private final long timeoutMillis;
    private final long pollMillis;
    private final Path workingDirectory;
    private final Path codeDirectory;
    private final Deque<String> logLines = new ArrayDeque<>();

    private boolean bundledAllowed;
    private Process process;
    private Thread logThread;
    private String baseUrl;

    public BackendLauncher() {
        this(
                ProcessBuilder::start,
                BackendLauncher::probeHttp,
                DEFAULT_TIMEOUT_MILLIS,
                DEFAULT_POLL_MILLIS,
                Path.of("").toAbsolutePath(),
                codeDirectory());
        bundledAllowed = true;
    }

    BackendLauncher(
            ProcessStarter starter,
            HealthProbe probe,
            long timeoutMillis,
            long pollMillis,
            Path workingDirectory,
            Path codeDirectory) {
        this.starter = starter;
        this.probe = probe;
        this.timeoutMillis = timeoutMillis;
        this.pollMillis = pollMillis;
        this.workingDirectory = workingDirectory;
        this.codeDirectory = codeDirectory;
    }

    public synchronized String start() throws BackendException {
        if (process != null && process.isAlive() && baseUrl != null) {
            return baseUrl;
        }
        stop();

        Path backendHome =
                resolveBackendHome(
                        workingDirectory,
                        codeDirectory,
                        firstNonBlank(
                                System.getProperty("reto2.backend.home"),
                                System.getenv("RETO2_BACKEND_HOME")));
        if (backendHome == null && bundledAllowed) {
            try {
                backendHome = BundledBackend.extract();
            } catch (IOException exception) {
                throw new BackendException("BACKEND_EXTRACTION", "Cannot extract bundled backend.", exception);
            }
        }
        if (backendHome == null) {
            throw new BackendException(
                    "BACKEND_NO_ENCONTRADO",
                    "No se encontró la carpeta backend. Indica RETO2_BACKEND_HOME "
                            + "o coloca backend/ junto al cliente.");
        }

        Path python =
                resolvePython(
                        backendHome,
                        firstNonBlank(
                                System.getProperty("reto2.python"), System.getenv("RETO2_PYTHON")));
        int port = findFreePort();
        ProcessBuilder builder =
                new ProcessBuilder(
                                python.toString(),
                                "-m",
                                "reto2_backend",
                                "--port",
                                Integer.toString(port))
                        .directory(backendHome.toFile())
                        .redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        String sourcePath = backendHome.resolve("src").toString();
        String previousPythonPath = environment.get("PYTHONPATH");
        environment.put(
                "PYTHONPATH",
                previousPythonPath == null || previousPythonPath.isBlank()
                        ? sourcePath
                        : sourcePath + File.pathSeparator + previousPythonPath);
        environment.put("PYTHONIOENCODING", "utf-8");
        environment.put("PYTHONUNBUFFERED", "1");
        environment.put("PYTHONDONTWRITEBYTECODE", "1");

        try {
            process = starter.start(builder);
        } catch (IOException exception) {
            throw new BackendException(
                    "BACKEND_NO_INICIADO",
                    "No se pudo ejecutar Python: " + exception.getMessage(),
                    exception);
        }

        logLines.clear();
        Process started = process;
        logThread = new Thread(() -> drainLogs(started), "reto2-backend-log");
        logThread.setDaemon(true);
        logThread.start();

        String candidateUrl = "http://127.0.0.1:" + port;
        long deadline = System.nanoTime() + Duration.ofMillis(timeoutMillis).toNanos();
        try {
            while (true) {
                if (!process.isAlive()) {
                    throw new BackendException(
                            "BACKEND_FINALIZO",
                            "El backend terminó durante el arranque. " + logs(),
                            null);
                }
                if (probe.isHealthy(candidateUrl)) {
                    baseUrl = candidateUrl;
                    return baseUrl;
                }
                if (System.nanoTime() >= deadline) {
                    throw new BackendException(
                            "BACKEND_SIN_RESPUESTA",
                            "El backend no respondió en /health. " + logs(),
                            null);
                }
                Thread.sleep(pollMillis);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            stop();
            throw new BackendException(
                    "ARRANQUE_INTERRUMPIDO", "El arranque del backend se interrumpió.", exception);
        } catch (BackendException exception) {
            stop();
            throw exception;
        }
    }

    public synchronized boolean isRunning() {
        return process != null && process.isAlive() && baseUrl != null;
    }

    public synchronized String getBaseUrl() {
        return baseUrl;
    }

    public synchronized void stop() {
        Process current = process;
        if (current == null) {
            return;
        }
        current.descendants().forEach(ProcessHandle::destroy);
        current.destroy();
        try {
            if (!current.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                current.descendants().forEach(ProcessHandle::destroyForcibly);
                current.destroyForcibly();
                current.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            current.descendants().forEach(ProcessHandle::destroyForcibly);
            current.destroyForcibly();
        } finally {
            process = null;
            baseUrl = null;
            if (logThread != null) {
                logThread.interrupt();
                logThread = null;
            }
        }
    }

    @Override
    public void close() {
        stop();
    }

    static Path resolveBackendHome(Path workingDirectory, Path codeDirectory, String override) {
        Set<Path> candidates = new LinkedHashSet<>();
        if (override != null && !override.isBlank()) {
            candidates.add(Path.of(override));
        }
        addCandidates(candidates, workingDirectory);
        addCandidates(candidates, codeDirectory);
        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized.resolve(BACKEND_MARKER))
                    && Files.isDirectory(normalized.resolve("src"))) {
                return normalized;
            }
        }
        return null;
    }

    private static void addCandidates(Set<Path> candidates, Path base) {
        if (base == null) {
            return;
        }
        candidates.add(base.resolve("backend"));
        candidates.add(base.resolve("../backend"));
        candidates.add(base.resolve("../../backend"));
    }

    static Path resolvePython(Path backendHome, String override) {
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        Path windowsVenv = backendHome.resolve(".venv/Scripts/python.exe");
        Path unixVenv = backendHome.resolve(".venv/bin/python");
        if (Files.isRegularFile(windowsVenv)) {
            return windowsVenv;
        }
        if (Files.isRegularFile(unixVenv)) {
            return unixVenv;
        }
        return Path.of(isWindows() ? "python" : "python3");
    }

    static int findFreePort() throws BackendException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            socket.setReuseAddress(false);
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new BackendException(
                    "PUERTO_NO_DISPONIBLE", "No se pudo reservar un puerto local.", exception);
        }
    }

    static boolean probeHttp(String baseUrl) {
        try (HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build()) {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl + "/health"))
                            .timeout(Duration.ofMillis(800))
                            .GET()
                            .build();
            HttpResponse<String> response =
                    client.send(
                            request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return false;
            }
            return "ok".equals(Json.parseObject(response.body()).get("estado"));
        } catch (IOException | InterruptedException | RuntimeException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private void drainLogs(Process started) {
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(started.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (logLines) {
                    logLines.addLast(line);
                    if (logLines.size() > 100) {
                        logLines.removeFirst();
                    }
                }
            }
        } catch (IOException exception) {
            if (!Thread.currentThread().isInterrupted()) {
                synchronized (logLines) {
                    logLines.addLast("No se pudo leer la salida del backend.");
                }
            }
        }
    }

    private String logs() {
        synchronized (logLines) {
            if (logLines.isEmpty()) {
                return "Sin trazas del proceso.";
            }
            return "Últimas trazas: " + String.join(" | ", logLines);
        }
    }

    private static Path codeDirectory() {
        try {
            URI location =
                    BackendLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path path = Path.of(location);
            return Files.isDirectory(path) ? path : path.getParent();
        } catch (Exception exception) {
            return Path.of("").toAbsolutePath();
        }
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
