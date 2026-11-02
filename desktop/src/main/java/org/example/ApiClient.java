package org.example;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/** Thin HTTP client for the shared API. No cipher implementation lives in the desktop client. */
public final class ApiClient {
    private final SettingsStore settings;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public ApiClient(SettingsStore settings) { this.settings = settings; }

    public void checkAuth() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/auth/check")).timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + settings.token()).GET().build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        ensureSuccess(response.statusCode(), response.body());
    }

    public String vigenere(String text, String key, String mode) throws Exception {
        String json = "{\"text\":" + quote(text) + ",\"key\":" + quote(key) + ",\"mode\":" + quote(mode) + "}";
        HttpRequest request = HttpRequest.newBuilder(uri("/text/vigenere")).timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + settings.token())
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        ensureSuccess(response.statusCode(), response.body());
        String payload = new String(response.body(), StandardCharsets.UTF_8);
        String result = jsonString(payload, "result");
        if (result == null) throw new IOException("La respuesta del servidor no contiene el resultado esperado.");
        return result;
    }

    public FileResult fileOperation(byte[] file, String fileName, String password, boolean encrypt) throws Exception {
        String boundary = "----RunnersZ" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream(file.length + 1024);
        write(body, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + safeFilename(fileName) + "\"\r\nContent-Type: application/octet-stream\r\n\r\n");
        body.write(file);
        write(body, "\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"password\"\r\n\r\n" + password + "\r\n--" + boundary + "--\r\n");
        String path = encrypt ? "/files/encrypt" : "/files/decrypt";
        HttpRequest request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofMinutes(10))
                .header("Authorization", "Bearer " + settings.token())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        ensureSuccess(response.statusCode(), response.body());
        if (!encrypt && !"verified".equalsIgnoreCase(response.headers().firstValue("X-Integrity").orElse("")))
            throw new IOException("El servidor no confirmó la integridad del fichero; no se guardó el resultado.");
        return new FileResult(response.body());
    }

    private URI uri(String endpoint) { return URI.create(settings.baseUrl() + "/api/v1" + endpoint); }

    private static void ensureSuccess(int status, byte[] body) throws IOException {
        if (status >= 200 && status < 300) return;
        String payload = new String(body, StandardCharsets.UTF_8);
        String code = jsonString(payload, "code"), message = jsonString(payload, "message");
        if (message == null || message.isBlank()) message = "El servidor respondió con HTTP " + status + ".";
        if (code != null && !code.isBlank()) message += " (" + code + ")";
        throw new IOException(message);
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) switch (c) {
            case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\"); case '\b' -> out.append("\\b");
            case '\f' -> out.append("\\f"); case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
            default -> { if (c < 0x20) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
        }
        return out.append('"').toString();
    }

    /** Extracts a JSON string property while honoring escaped quotes and backslashes. */
    private static String jsonString(String json, String property) {
        String key = "\"" + property + "\"";
        int i = json.indexOf(key); if (i < 0) return null;
        i = json.indexOf(':', i + key.length()); if (i < 0) return null; i++;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i++) != '"') return null;
        StringBuilder value = new StringBuilder(); boolean escaped = false;
        for (; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) { switch (c) { case 'n' -> value.append('\n'); case 'r' -> value.append('\r'); case 't' -> value.append('\t'); case 'b' -> value.append('\b'); case 'f' -> value.append('\f'); case '"', '\\', '/' -> value.append(c); case 'u' -> { if (i + 4 >= json.length()) return null; try { value.append((char)Integer.parseInt(json.substring(i+1, i+5), 16)); i += 4; } catch (NumberFormatException ex) { return null; } } default -> value.append(c); } escaped = false; }
            else if (c == '\\') escaped = true; else if (c == '"') return value.toString(); else value.append(c);
        }
        return null;
    }

    private static String safeFilename(String name) { return name.replace("\"", "_").replace("\r", "_").replace("\n", "_"); }
    private static void write(ByteArrayOutputStream target, String value) throws IOException { target.write(value.getBytes(StandardCharsets.UTF_8)); }
    public record FileResult(byte[] bytes) {}
}
