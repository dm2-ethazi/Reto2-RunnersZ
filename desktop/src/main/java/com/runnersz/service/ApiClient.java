package com.runnersz.service;

import com.runnersz.model.ConnectionSettings;
import com.runnersz.model.FileResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Adaptador HTTP de la API: token, multipart, JSON y errores del servidor. No cifra datos
 * localmente. Recibe una instantánea de la configuración para que cambiar Ajustes no altere una
 * petición que ya está en curso.
 */
public final class ApiClient {
  private final ConnectionSettings settings;
  private final HttpClient client =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(10))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  public ApiClient(ConnectionSettings settings) {
    this.settings = settings;
  }

  /** Comprueba el token mediante /auth/check sin enviar ficheros. */
  public void checkAuth() throws IOException, InterruptedException {
    HttpResponse<byte[]> response =
        send(
            HttpRequest.newBuilder(uri("/auth/check"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + settings.token())
                .GET()
                .build());
    ensureSuccess(response);
  }

  /** Envía texto y clave como JSON y recupera el campo result en UTF-8. */
  public String vigenere(String text, String key, String mode)
      throws IOException, InterruptedException {
    String json =
        "{\"text\":" + quote(text) + ",\"key\":" + quote(key) + ",\"mode\":" + quote(mode) + "}";
    HttpResponse<byte[]> response =
        send(
            HttpRequest.newBuilder(uri("/text/vigenere"))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + settings.token())
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build());
    ensureSuccess(response);

    String result = jsonString(new String(response.body(), StandardCharsets.UTF_8), "result");
    if (result == null) {
      throw new IOException("La API respondió correctamente, pero no incluyó el campo «result».");
    }
    return result;
  }

  /**
   * Envía los bytes, nunca rutas locales. Al descifrar, exige la confirmación X-Integrity antes de
   * entregar datos que el servicio de ficheros pueda guardar.
   */
  public FileResult fileOperation(byte[] file, String fileName, String password, boolean encrypt)
      throws IOException, InterruptedException {
    String boundary = "----RunnersZ" + UUID.randomUUID().toString().replace("-", "");
    ByteArrayOutputStream body = new ByteArrayOutputStream(file.length + 1024);
    write(
        body,
        "--"
            + boundary
            + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
            + safeFilename(fileName)
            + "\"\r\nContent-Type: application/octet-stream\r\n\r\n");
    body.write(file);
    write(
        body,
        "\r\n--"
            + boundary
            + "\r\nContent-Disposition: form-data; name=\"password\"\r\n\r\n"
            + password
            + "\r\n--"
            + boundary
            + "--\r\n");

    String endpoint = encrypt ? "/files/encrypt" : "/files/decrypt";
    HttpResponse<byte[]> response =
        send(
            HttpRequest.newBuilder(uri(endpoint))
                .timeout(Duration.ofMinutes(10))
                .header("Authorization", "Bearer " + settings.token())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build());
    ensureSuccess(response);

    if (!encrypt
        && !"verified".equalsIgnoreCase(response.headers().firstValue("X-Integrity").orElse(""))) {
      throw new IOException(
          "La API no confirmó la integridad del fichero (falta X-Integrity: verified). No se guardó"
              + " el resultado.");
    }
    return new FileResult(response.body());
  }

  private HttpResponse<byte[]> send(HttpRequest request) throws IOException, InterruptedException {
    try {
      return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    } catch (IOException ex) {
      String detail = ex.getMessage();
      if (detail == null || detail.isBlank()) detail = ex.getClass().getSimpleName();
      throw new IOException(
          "No se pudo completar la llamada a " + request.uri() + ": " + detail, ex);
    }
  }

  private URI uri(String endpoint) {
    return URI.create(settings.baseUrl() + "/api/v1" + endpoint);
  }

  private static void ensureSuccess(HttpResponse<byte[]> response) throws IOException {
    int status = response.statusCode();
    if (status >= 200 && status < 300) return;

    String payload = new String(response.body(), StandardCharsets.UTF_8).trim();
    String code = jsonString(payload, "code");
    String message = jsonString(payload, "message");
    if (message == null) message = jsonString(payload, "detail");
    if (message == null) message = jsonString(payload, "msg");
    if (message == null || message.isBlank()) {
      message = payload.isBlank() ? "La API no devolvió detalles." : payload;
    }
    if (message.length() > 600) message = message.substring(0, 597) + "…";
    if (code != null && !code.isBlank() && !message.contains(code)) message += " (" + code + ")";
    throw new IOException("Error HTTP " + status + ": " + message);
  }

  private static String quote(String value) {
    StringBuilder out = new StringBuilder("\"");
    for (char c : value.toCharArray()) {
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\b' -> out.append("\\b");
        case '\f' -> out.append("\\f");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
          else out.append(c);
        }
      }
    }
    return out.append('"').toString();
  }

  /** Lee una propiedad de texto del JSON respetando comillas, escapes y Unicode. */
  private static String jsonString(String json, String property) {
    String key = "\"" + property + "\"";
    int i = json.indexOf(key);
    if (i < 0) return null;
    i = json.indexOf(':', i + key.length());
    if (i < 0) return null;
    i++;
    while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
    if (i >= json.length() || json.charAt(i++) != '"') return null;

    StringBuilder value = new StringBuilder();
    boolean escaped = false;
    for (; i < json.length(); i++) {
      char c = json.charAt(i);
      if (escaped) {
        switch (c) {
          case 'n' -> value.append('\n');
          case 'r' -> value.append('\r');
          case 't' -> value.append('\t');
          case 'b' -> value.append('\b');
          case 'f' -> value.append('\f');
          case '"', '\\', '/' -> value.append(c);
          case 'u' -> {
            if (i + 4 >= json.length()) return null;
            try {
              value.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
              i += 4;
            } catch (NumberFormatException ex) {
              return null;
            }
          }
          default -> value.append(c);
        }
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
      } else if (c == '"') {
        return value.toString();
      } else {
        value.append(c);
      }
    }
    return null;
  }

  /** Evita que el nombre rompa las cabeceras del formulario multipart. */
  private static String safeFilename(String name) {
    return name.replace("\\", "_")
        .replace("/", "_")
        .replace("\"", "_")
        .replace("\r", "_")
        .replace("\n", "_");
  }

  private static void write(ByteArrayOutputStream target, String value) throws IOException {
    target.write(value.getBytes(StandardCharsets.UTF_8));
  }
}
