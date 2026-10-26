package com.runnersz.reto2.desktop.service;

import com.runnersz.reto2.desktop.model.AesResult;
import com.runnersz.reto2.desktop.util.Json;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class BackendService {
    private final HttpClient client;
    private final Duration requestTimeout;
    private volatile String baseUrl;

    public BackendService() {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                Duration.ofMinutes(30));
    }

    BackendService(HttpClient client, Duration requestTimeout) {
        this.client = client;
        this.requestTimeout = requestTimeout;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null ? null : baseUrl.replaceAll("/+$", "");
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean health() throws BackendException {
        Map<String, Object> response = get("/health");
        return "ok".equals(response.get("estado"));
    }

    public AesResult encryptFile(String inputPath, String outputPath, String password)
            throws BackendException {
        Map<String, Object> response = aesOperation("cifrar", inputPath, outputPath, password);
        return toAesResult(response);
    }

    public AesResult decryptFile(String inputPath, String outputPath, String password)
            throws BackendException {
        Map<String, Object> response = aesOperation("descifrar", inputPath, outputPath, password);
        return toAesResult(response);
    }

    public String vigenere(String text, String key, boolean encrypt) throws BackendException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("texto", text);
        payload.put("clave", key);
        payload.put("modo", encrypt ? "cifrar" : "descifrar");
        Map<String, Object> response = post("/api/vigenere", payload);
        return requireString(response, "resultado");
    }

    private Map<String, Object> aesOperation(
            String operation, String inputPath, String outputPath, String password)
            throws BackendException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ruta_entrada", inputPath);
        payload.put("ruta_salida", outputPath);
        payload.put("contrasena", password);
        return post("/api/aes/" + operation, payload);
    }

    private AesResult toAesResult(Map<String, Object> response) throws BackendException {
        Object integrity = response.get("integridad_verificada");
        Boolean verified = integrity instanceof Boolean value ? value : null;
        return new AesResult(
                requireString(response, "ruta_salida"),
                requireLong(response, "bytes_entrada"),
                requireLong(response, "bytes_salida"),
                requireString(response, "sha256"),
                verified);
    }

    private Map<String, Object> get(String path) throws BackendException {
        HttpRequest request = requestBuilder(path).GET().build();
        return exchange(request);
    }

    private Map<String, Object> post(String path, Map<String, Object> payload)
            throws BackendException {
        HttpRequest request =
                requestBuilder(path)
                        .header("Content-Type", "application/json; charset=utf-8")
                        .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(payload), StandardCharsets.UTF_8))
                        .build();
        return exchange(request);
    }

    private HttpRequest.Builder requestBuilder(String path) throws BackendException {
        String current = baseUrl;
        if (current == null || current.isBlank()) {
            throw new BackendException(
                    "BACKEND_NO_DISPONIBLE", "El backend local no está iniciado.");
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(current + path))
                .timeout(requestTimeout)
                .header("Accept", "application/json");
    }

    private Map<String, Object> exchange(HttpRequest request) throws BackendException {
        HttpResponse<String> response;
        try {
            response =
                    client.send(
                            request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BackendException(
                    "OPERACION_INTERRUMPIDA", "La operación se interrumpió.", exception);
        } catch (IOException exception) {
            throw new BackendException(
                    "BACKEND_NO_DISPONIBLE",
                    "No se pudo conectar con el backend local: " + exception.getMessage(),
                    exception);
        }

        Map<String, Object> body;
        try {
            body = Json.parseObject(response.body());
        } catch (Json.ParseException exception) {
            throw new BackendException(
                    "RESPUESTA_INVALIDA", "El backend devolvió una respuesta no válida.", exception);
        }

        if (response.statusCode() >= 400) {
            throw errorFrom(response.statusCode(), body);
        }
        return body;
    }

    private BackendException errorFrom(int status, Map<String, Object> body) {
        Object nested = body.get("error");
        if (nested instanceof Map<?, ?> error) {
            Object code = error.get("codigo");
            Object message = error.get("mensaje");
            return new BackendException(
                    code == null ? "ERROR_BACKEND" : String.valueOf(code),
                    message == null ? "Error del backend." : String.valueOf(message));
        }
        return new BackendException(
                "ERROR_BACKEND", "El backend respondió con el código HTTP " + status + ".");
    }

    private String requireString(Map<String, Object> response, String key)
            throws BackendException {
        Object value = response.get(key);
        if (value instanceof String text) {
            return text;
        }
        throw new BackendException(
                "RESPUESTA_INVALIDA", "Falta el campo de texto '" + key + "' en la respuesta.");
    }

    private long requireLong(Map<String, Object> response, String key) throws BackendException {
        Object value = response.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new BackendException(
                "RESPUESTA_INVALIDA", "Falta el campo numérico '" + key + "' en la respuesta.");
    }
}
