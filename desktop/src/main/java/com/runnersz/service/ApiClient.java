package com.runnersz.service;

import com.runnersz.model.ConnectionSettings;
import com.runnersz.utiles.I18n;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * Adaptador HTTP de la API: token, multipart por bloques, JSON y errores del servidor. No cifra
 * datos localmente. Recibe una instantánea de la configuración para que cambiar Ajustes no altere
 * una petición que ya está en curso.
 *
 * <p>Los ficheros se envían y se reciben en bloques de 64 KiB: el consumo de memoria no depende del
 * tamaño del archivo y el progreso refleja bytes reales transferidos.
 *
 * @author RunnersZ
 */
public final class ApiClient {
  /** Límite del contrato (§7.2): ninguna petición puede esperar más de dos minutos. */
  public static final Duration TIMEOUT = Duration.ofSeconds(120);

  private static final int CHUNK = 64 * 1024;
  private static final int MAX_ERROR_BYTES = 8 * 1024;

  /** Notificación de avance: bytes transferidos y total conocido (0 si se desconoce). */
  @FunctionalInterface
  public interface Progress {
    void advance(long done, long total);
  }

  private final ConnectionSettings settings;
  private final HttpClient client =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(10))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  /**
   * Crea el cliente con una instantánea de la configuración.
   *
   * @param settings dirección y token que se usarán en todas las peticiones
   */
  public ApiClient(ConnectionSettings settings) {
    this.settings = settings;
  }

  /** Comprueba el token mediante /auth/check sin enviar ficheros. */
  public void checkAuth() throws IOException, InterruptedException {
    HttpResponse<byte[]> response =
        sendBytes(
            HttpRequest.newBuilder(uri("/auth/check"))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + settings.token())
                .GET()
                .build());
    ensureSuccess(response.statusCode(), text(response.body()));
  }

  /** Envía texto y clave como JSON y recupera el campo result en UTF-8. */
  public String vigenere(String text, String key, String mode)
      throws IOException, InterruptedException {
    String json =
        "{\"text\":"
            + quote(text)
            + ",\"key\":"
            + quote(key)
            + ",\"mode\":"
            + quote(mode)
            + "}";
    return textResult("/text/vigenere", json);
  }

  /** Cifra o descifra con César; el desplazamiento es un entero de 1 a 25. */
  public String caesar(String text, int shift, String mode)
      throws IOException, InterruptedException {
    String json =
        "{\"text\":"
            + quote(text)
            + ",\"shift\":"
            + shift
            + ",\"mode\":"
            + quote(mode)
            + "}";
    return textResult("/text/caesar", json);
  }

  /**
   * Envía el fichero por bloques y escribe la respuesta directamente en {@code target}. Al
   * descifrar exige {@code X-Integrity: verified} para que el servicio de ficheros publique el
   * resultado.
   */
  public void transferFile(
      Path source, Path target, String password, boolean encrypt, Progress progress)
      throws IOException, InterruptedException {
    String cleanPassword = password == null ? "" : password;
    if (cleanPassword.isEmpty()) throw I18n.failure("error.EMPTY_PASSWORD");
    if (cleanPassword.indexOf('\r') >= 0 || cleanPassword.indexOf('\n') >= 0) {
      throw I18n.failure("error.passwordLine");
    }

    long size = Files.size(source);
    String boundary = "----RunnersZ" + UUID.randomUUID().toString().replace("-", "");
    String fileName = source.getFileName().toString();
    byte[] head =
        ("--"
                + boundary
                + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                + asciiName(fileName)
                + "\"; filename*=UTF-8''"
                + percentEncode(fileName)
                + "\r\nContent-Type: application/octet-stream\r\n\r\n")
            .getBytes(StandardCharsets.UTF_8);
    byte[] tail =
        ("\r\n--"
                + boundary
                + "\r\nContent-Disposition: form-data; name=\"password\"\r\n\r\n"
                + cleanPassword
                + "\r\n--"
                + boundary
                + "--\r\n")
            .getBytes(StandardCharsets.UTF_8);

    HttpRequest request =
        HttpRequest.newBuilder(uri(encrypt ? "/files/encrypt" : "/files/decrypt"))
            .timeout(TIMEOUT)
            .header("Authorization", "Bearer " + settings.token())
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(new StreamBodyPublisher(source, head, tail, size, progress))
            .build();

    ErrorCollector errors = new ErrorCollector();
    HttpResponse<Path> response =
        client.send(
            request,
            info ->
                info.statusCode() >= 200 && info.statusCode() < 300
                    ? new FileSubscriber(
                        target,
                        progress,
                        info.headers().firstValueAsLong("Content-Length").orElse(-1))
                    : errors);

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw failureFor(response.statusCode(), errors.text());
    }
    if (!encrypt
        && !"verified".equalsIgnoreCase(response.headers().firstValue("X-Integrity").orElse(""))) {
      throw I18n.failure("error.integrity");
    }
  }

  private String textResult(String endpoint, String json)
      throws IOException, InterruptedException {
    HttpResponse<byte[]> response =
        sendBytes(
            HttpRequest.newBuilder(uri(endpoint))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + settings.token())
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build());
    ensureSuccess(response.statusCode(), text(response.body()));

    String result = jsonString(text(response.body()), "result");
    if (result == null) {
      throw I18n.failure("error.apiResult");
    }
    return result;
  }

  private HttpResponse<byte[]> sendBytes(HttpRequest request)
      throws IOException, InterruptedException {
    try {
      return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    } catch (IOException ex) {
      IOException failure = I18n.failure("error.network");
      failure.initCause(ex);
      throw failure;
    }
  }

  private URI uri(String endpoint) throws IOException {
    String base = settings.baseUrl();
    if (base == null || base.isBlank()) throw I18n.failure("error.notConfigured");
    return URI.create(base + "/api/v1" + endpoint);
  }

  private static String text(byte[] body) {
    return new String(body, StandardCharsets.UTF_8);
  }

  private static void ensureSuccess(int status, String payload) throws IOException {
    if (status >= 200 && status < 300) return;
    throw failureFor(status, payload);
  }

  private static IOException failureFor(int status, String payload) {
    String trimmed = payload == null ? "" : payload.trim();
    String code = jsonString(trimmed, "code");
    // Los códigos del contrato se traducen; nunca mostramos cuerpos arbitrarios del servidor.
    String messageKey =
        switch (code == null ? "" : code) {
          case "UNAUTHORIZED", "EMPTY_FILE", "EMPTY_PASSWORD", "INVALID_KEY", "INVALID_SHIFT",
                  "FILE_TOO_LARGE", "DECRYPT_FAILED", "INTERNAL_ERROR" ->
              "error." + code;
          default ->
              switch (status) {
                case 401, 403 -> "error.UNAUTHORIZED";
                case 413 -> "error.FILE_TOO_LARGE";
                case 422 -> "error.DECRYPT_FAILED";
                case 500 -> "error.INTERNAL_ERROR";
                default -> "error.apiUnknown";
              };
        };
    // La excepción conserva la clave, de modo que el detalle se resuelve en el idioma actual.
    return I18n.httpFailure(status, messageKey);
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

  /** Nombre ASCII para la cabecera clásica; los acentos viajan en filename*. */
  private static String asciiName(String name) {
    StringBuilder out = new StringBuilder();
    for (char c : name.toCharArray()) {
      out.append(c >= 0x20 && c < 0x7f && c != '"' && c != '\\' ? c : '_');
    }
    return out.toString();
  }

  private static String percentEncode(String value) {
    StringBuilder out = new StringBuilder();
    for (byte raw : value.getBytes(StandardCharsets.UTF_8)) {
      int c = raw & 0xff;
      boolean simple =
          (c >= 'a' && c <= 'z')
              || (c >= 'A' && c <= 'Z')
              || (c >= '0' && c <= '9')
              || c == '-'
              || c == '.'
              || c == '_'
              || c == '~';
      if (simple) out.append((char) c);
      else out.append('%').append(String.format("%02X", c));
    }
    return out.toString();
  }

  /** Emisor multipart que lee el archivo por bloques y publica el avance real. */
  private static final class StreamBodyPublisher implements HttpRequest.BodyPublisher {
    private final Path source;
    private final byte[] head;
    private final byte[] tail;
    private final long fileSize;
    private final long length;
    private final Progress progress;

    StreamBodyPublisher(Path source, byte[] head, byte[] tail, long fileSize, Progress progress) {
      this.source = source;
      this.head = head;
      this.tail = tail;
      this.fileSize = fileSize;
      this.length = head.length + fileSize + tail.length;
      this.progress = progress;
    }

    @Override
    public long contentLength() {
      return length;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
      subscriber.onSubscribe(
          new Flow.Subscription() {
            private long demand;
            private long sent;
            private int stage;
            private FileChannel channel;
            private boolean stopped;

            @Override
            public void request(long n) {
              if (stopped) return;
              if (n <= 0) {
                stopped = true;
                subscriber.onError(new IllegalArgumentException("demanda no válida"));
                return;
              }
              demand += n;
              emit();
            }

            @Override
            public void cancel() {
              stopped = true;
              closeChannel();
            }

            private void emit() {
              try {
                while (demand > 0 && !stopped) {
                  ByteBuffer buffer = next();
                  if (buffer == null) {
                    stopped = true;
                    closeChannel();
                    subscriber.onComplete();
                    return;
                  }
                  demand--;
                  subscriber.onNext(buffer);
                }
              } catch (IOException | RuntimeException ex) {
                stopped = true;
                closeChannel();
                subscriber.onError(ex);
              }
            }

            private ByteBuffer next() throws IOException {
              switch (stage) {
                case 0:
                  stage = 1;
                  return ByteBuffer.wrap(head);
                case 1:
                  if (sent >= fileSize) {
                    stage = 2;
                    return next();
                  }
                  ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(CHUNK, fileSize - sent));
                  if (channel == null) {
                    channel = FileChannel.open(source, StandardOpenOption.READ);
                  }
                  int read = channel.read(buffer);
                  if (read < 0) throw I18n.failure("error.fileChanged");
                  buffer.flip();
                  sent += read;
                  if (progress != null && fileSize > 0) progress.advance(sent, fileSize);
                  return buffer;
                case 2:
                  stage = 3;
                  return ByteBuffer.wrap(tail);
                default:
                  return null;
              }
            }

            private void closeChannel() {
              if (channel != null) {
                try {
                  channel.close();
                } catch (IOException ignored) {
                  // El canal solo se cierra; no hay nada que recuperar.
                }
                channel = null;
              }
            }
          });
    }
  }

  /** Receptor que escribe la respuesta en disco bloque a bloque. */
  private static final class FileSubscriber implements HttpResponse.BodySubscriber<Path> {
    private final Path target;
    private final Progress progress;
    private final long total;
    private final CompletableFuture<Path> future = new CompletableFuture<>();
    private OutputStream out;
    private Flow.Subscription subscription;
    private long written;
    private boolean failed;

    FileSubscriber(Path target, Progress progress, long total) {
      this.target = target;
      this.progress = progress;
      this.total = total;
    }

    @Override
    public CompletionStage<Path> getBody() {
      return future;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
      this.subscription = subscription;
      try {
        out = Files.newOutputStream(target);
      } catch (IOException ex) {
        failed = true;
        future.completeExceptionally(ex);
        subscription.cancel();
        return;
      }
      subscription.request(1);
    }

    @Override
    public void onNext(List<ByteBuffer> items) {
      if (failed) return;
      try {
        for (ByteBuffer item : items) {
          byte[] chunk = new byte[item.remaining()];
          item.get(chunk);
          out.write(chunk);
          written += chunk.length;
          if (progress != null) progress.advance(written, total);
        }
      } catch (IOException ex) {
        failed = true;
        close(false);
        subscription.cancel();
        future.completeExceptionally(ex);
        return;
      }
      subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
      close(false);
      future.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
      close(true);
    }

    private void close(boolean success) {
      try {
        if (out != null) out.close();
      } catch (IOException ex) {
        if (success) {
          future.completeExceptionally(ex);
          return;
        }
      }
      if (success) future.complete(target);
    }
  }

  /** Guarda un cuerpo de error pequeño para traducir el código de la API. */
  private static final class ErrorCollector implements HttpResponse.BodySubscriber<Path> {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final CompletableFuture<Path> future = new CompletableFuture<>();

    @Override
    public CompletionStage<Path> getBody() {
      return future;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
      subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(List<ByteBuffer> items) {
      for (ByteBuffer item : items) {
        int remaining = Math.min(item.remaining(), MAX_ERROR_BYTES - bytes.size());
        if (remaining <= 0) return;
        byte[] chunk = new byte[remaining];
        item.get(chunk);
        bytes.writeBytes(chunk);
      }
    }

    @Override
    public void onError(Throwable throwable) {
      future.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
      future.complete(null);
    }

    String text() {
      return bytes.toString(StandardCharsets.UTF_8);
    }
  }
}
