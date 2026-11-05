package com.runnersz.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Servidor HTTP local que imita la API para probar el cliente sin Docker ni red externa.
 *
 * @author RunnersZ
 */
public final class StubApi implements AutoCloseable {
  /** Petición recibida, con las cabeceras que valida el cliente. */
  public record Request(
      String path, String method, String contentType, String authorization, byte[] body) {}

  private final HttpServer server;
  private final Map<String, String> headers = new HashMap<>();
  private volatile int status = 204;
  private volatile byte[] body = new byte[0];
  private volatile Request last;

  public StubApi() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/v1",
        exchange -> {
          byte[] received = exchange.getRequestBody().readAllBytes();
          last =
              new Request(
                  exchange.getRequestURI().getPath(),
                  exchange.getRequestMethod(),
                  exchange.getRequestHeaders().getFirst("Content-Type"),
                  exchange.getRequestHeaders().getFirst("Authorization"),
                  received);
          headers.forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
          byte[] payload = body;
          if (payload.length == 0) {
            exchange.sendResponseHeaders(status, -1);
          } else {
            exchange.sendResponseHeaders(status, payload.length);
            exchange.getResponseBody().write(payload);
          }
          exchange.close();
        });
    server.start();
  }

  public String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  public void respond(int status, String payload) {
    this.status = status;
    this.body = payload.getBytes(StandardCharsets.UTF_8);
  }

  public void respond(int status, byte[] payload) {
    this.status = status;
    this.body = payload.clone();
  }

  public void header(String name, String value) {
    headers.put(name, value);
  }

  public void clearHeaders() {
    headers.clear();
  }

  public Request last() {
    return last;
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
