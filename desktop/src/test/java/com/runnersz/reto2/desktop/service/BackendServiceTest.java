package com.runnersz.reto2.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runnersz.reto2.desktop.model.AesResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BackendServiceTest {
    private HttpServer server;
    private BackendService service;
    private final AtomicReference<String> lastBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(
                "/health",
                exchange ->
                        respond(
                                exchange,
                                200,
                                "{\"estado\":\"ok\",\"servicio\":\"reto2-backend\","
                                        + "\"version\":\"1.0.0\"}"));
        server.createContext("/api/vigenere", this::handleVigenere);
        server.createContext("/api/aes/", this::handleAes);
        server.start();
        service = new BackendService(HttpClient.newHttpClient(), Duration.ofSeconds(5));
        service.setBaseUrl(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void handleVigenere(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        lastBody.set(body);
        if (body.contains("INVALID")) {
            respond(exchange, 200, "no-es-json");
            return;
        }
        if (body.contains("MISSING")) {
            respond(exchange, 200, "{}");
            return;
        }
        if (body.contains("SERVERERROR")) {
            respond(exchange, 500, "{\"error\":{\"codigo\":\"FALLO\",\"mensaje\":\"roto\"}}");
            return;
        }
        respond(exchange, 200, "{\"resultado\":\"Siya\"}");
    }

    private void handleAes(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        lastBody.set(body);
        if (body.contains("mala")) {
            respond(
                    exchange,
                    422,
                    "{\"error\":{\"codigo\":\"CLAVE_INCORRECTA_O_DANADO\","
                            + "\"mensaje\":\"Clave incorrecta o fichero danado.\"}}");
            return;
        }
        if (body.contains("sinerror")) {
            respond(exchange, 500, "{\"estado\":\"sin-error\"}");
            return;
        }
        if (body.contains("campo")) {
            respond(exchange, 200, "{}");
            return;
        }
        boolean decrypt = exchange.getRequestURI().getPath().endsWith("descifrar");
        String integrity = decrypt ? "true" : "null";
        respond(
                exchange,
                200,
                "{\"ruta_salida\":\"salida.enc\",\"bytes_entrada\":10,"
                        + "\"bytes_salida\":20,\"sha256\":\"abc123\","
                        + "\"integridad_verificada\":"
                        + integrity
                        + "}");
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    @Test
    void healthDevuelveTrue() throws Exception {
        assertTrue(service.health());
        assertEquals(
                "http://127.0.0.1:" + server.getAddress().getPort(), service.getBaseUrl());
    }

    @Test
    void vigenereCifrarEnviaCampos() throws Exception {
        assertEquals("Siya", service.vigenere("Hola", "LUNA", true));
        assertTrue(lastBody.get().contains("\"modo\":\"cifrar\""));
        assertTrue(lastBody.get().contains("\"clave\":\"LUNA\""));
    }

    @Test
    void vigenereDescifrarEnviaCampos() throws Exception {
        assertEquals("Siya", service.vigenere("Siya", "LUNA", false));
        assertTrue(lastBody.get().contains("\"modo\":\"descifrar\""));
    }

    @Test
    void cifrarFicheroDevuelveResultado() throws Exception {
        AesResult result = service.encryptFile("entrada.txt", "salida.enc", "clave");
        assertEquals("salida.enc", result.outputPath());
        assertEquals(10, result.inputBytes());
        assertEquals(20, result.outputBytes());
        assertEquals("abc123", result.sha256());
        assertNull(result.integrityVerified());
        assertTrue(lastBody.get().contains("\"contrasena\":\"clave\""));
        assertTrue(lastBody.get().contains("\"ruta_entrada\":\"entrada.txt\""));
    }

    @Test
    void descifrarFicheroDevuelveIntegridad() throws Exception {
        AesResult result = service.decryptFile("salida.enc", "recuperado", "clave");
        assertEquals(Boolean.TRUE, result.integrityVerified());
    }

    @Test
    void errorDelBackendSeTraduce() {
        BackendException exception =
                assertThrows(
                        BackendException.class,
                        () -> service.encryptFile("entrada", "salida", "mala"));
        assertEquals("CLAVE_INCORRECTA_O_DANADO", exception.getCode());
        assertTrue(exception.getMessage().contains("Clave"));
    }

    @Test
    void errorSinCuerpoEstructurado() {
        BackendException exception =
                assertThrows(
                        BackendException.class,
                        () -> service.encryptFile("entrada", "salida", "sinerror"));
        assertEquals("ERROR_BACKEND", exception.getCode());
        assertTrue(exception.getMessage().contains("500"));
    }

    @Test
    void respuestaNoJson() {
        BackendException exception =
                assertThrows(
                        BackendException.class, () -> service.vigenere("INVALID", "LUNA", true));
        assertEquals("RESPUESTA_INVALIDA", exception.getCode());
    }

    @Test
    void respuestaSinCampo() {
        BackendException exception =
                assertThrows(
                        BackendException.class, () -> service.vigenere("MISSING", "LUNA", true));
        assertEquals("RESPUESTA_INVALIDA", exception.getCode());
    }

    @Test
    void respuestaAesIncompleta() {
        BackendException exception =
                assertThrows(
                        BackendException.class,
                        () -> service.encryptFile("entrada", "salida", "campo"));
        assertEquals("RESPUESTA_INVALIDA", exception.getCode());
    }

    @Test
    void errorEstructuradoDelBackend() {
        BackendException exception =
                assertThrows(
                        BackendException.class,
                        () -> service.vigenere("SERVERERROR", "LUNA", true));
        assertEquals("FALLO", exception.getCode());
        assertEquals("roto", exception.getMessage());
    }

    @Test
    void sinUrl() {
        BackendService withoutUrl = new BackendService();
        BackendException exception =
                assertThrows(BackendException.class, () -> withoutUrl.health());
        assertEquals("BACKEND_NO_DISPONIBLE", exception.getCode());
        assertNull(withoutUrl.getBaseUrl());
    }

    @Test
    void conexionRechazada() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        service.setBaseUrl("http://127.0.0.1:" + port);
        BackendException exception =
                assertThrows(BackendException.class, () -> service.health());
        assertEquals("BACKEND_NO_DISPONIBLE", exception.getCode());
        assertFalse(exception.getMessage().isBlank());
    }
}
