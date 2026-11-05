package com.runnersz.service;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runnersz.model.ConnectionSettings;
import com.runnersz.support.StubApi;
import com.runnersz.utiles.I18n;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Pruebas del adaptador HTTP: rutas, token, multipart por bloques, integridad y errores. */
class ApiClientTest {
  @TempDir Path dir;

  private StubApi api;

  @BeforeEach
  void start() throws IOException {
    I18n.setLanguage("es");
    api = new StubApi();
  }

  @AfterEach
  void stop() {
    api.close();
  }

  private ApiClient client() {
    return new ApiClient(new ConnectionSettings(api.baseUrl(), "token-de-prueba"));
  }

  @Test
  void compruebaElTokenSinEnviarCuerpo() throws Exception {
    api.respond(204, "");
    client().checkAuth();

    assertEquals("/api/v1/auth/check", api.last().path());
    assertEquals("GET", api.last().method());
    assertEquals("Bearer token-de-prueba", api.last().authorization());
  }

  @Test
  void traduceElErrorDeToken() {
    api.respond(401, "{\"code\":\"UNAUTHORIZED\",\"message\":\"sin token\"}");
    IOException error = assertThrows(IOException.class, () -> client().checkAuth());
    assertTrue(error.getMessage().contains("token"), error.getMessage());
  }

  @Test
  void traduceElErrorDeTamanoExcesivo() {
    api.respond(413, "{\"code\":\"FILE_TOO_LARGE\"}");
    IOException error = assertThrows(IOException.class, () -> client().checkAuth());
    assertTrue(error.getMessage().contains("supera el tamaño"), error.getMessage());
  }

  @Test
  void traduceElErrorInternoCuandoNoHayCodigo() {
    api.respond(500, "sin json");
    IOException error = assertThrows(IOException.class, () -> client().checkAuth());
    assertTrue(error.getMessage().contains("error interno"), error.getMessage());
  }

  @Test
  void rechazaUnaDireccionSinConfigurar() {
    ApiClient client = new ApiClient(new ConnectionSettings("", ""));
    IOException error = assertThrows(IOException.class, client::checkAuth);
    assertTrue(error.getMessage().contains("Configura"), error.getMessage());
  }

  @Test
  void enviaElTextoYRecuperaElResultadoConEscapes() throws Exception {
    api.respond(200, "{\"result\":\"Línea \\\"uno\\\"\\ny dos\"}");
    String result = client().vigenere("hola mundo", "clave", "encrypt");

    assertEquals("Línea \"uno\"\ny dos", result);
    assertEquals("/api/v1/text/vigenere", api.last().path());
    String sent = new String(api.last().body(), UTF_8);
    assertTrue(sent.contains("\"mode\":\"encrypt\""), sent);
    assertTrue(sent.contains("\"text\":\"hola mundo\""), sent);
  }

  @Test
  void enviaElDesplazamientoDeCesarComoNumero() throws Exception {
    api.respond(200, "{\"result\":\"KHOOR\"}");
    assertEquals("KHOOR", client().caesar("HELLO", 3, "encrypt"));

    String sent = new String(api.last().body(), UTF_8);
    assertEquals("/api/v1/text/caesar", api.last().path());
    assertTrue(sent.contains("\"shift\":3"), sent);
  }

  @Test
  void fallaSiLaRespuestaDeTextoNoTraeResultado() {
    api.respond(200, "{\"otro\":\"valor\"}");
    IOException error =
        assertThrows(IOException.class, () -> client().vigenere("a", "b", "encrypt"));
    assertTrue(error.getMessage().contains("result"), error.getMessage());
  }

  @Test
  void cifraUnFicheroPorBloquesEInformaDelProgreso() throws Exception {
    Path source = dir.resolve("datos.bin");
    Files.write(source, "0123456789".repeat(150_000).getBytes(ISO_8859_1));
    Path target = dir.resolve("datos.bin.enc");
    api.respond(200, "cifrado".getBytes(UTF_8));

    long size = Files.size(source);
    AtomicLong uploaded = new AtomicLong();
    client()
        .transferFile(
            source,
            target,
            "clave",
            true,
            (done, total) -> {
              if (total == size) uploaded.set(done);
            });

    assertEquals("cifrado", Files.readString(target));
    assertEquals(size, uploaded.get(), "el progreso de subida llega al total");
    assertTrue(api.last().contentType().startsWith("multipart/form-data; boundary="));
    String body = new String(api.last().body(), ISO_8859_1);
    assertTrue(body.contains("name=\"file\"; filename=\"datos.bin\""), "cabecera del fichero");
    assertTrue(body.contains("\r\n\r\nclave\r\n--"), "campo de contraseña");
  }

  @Test
  void aceptaNombresConAcentosEnElFichero() throws Exception {
    Path source = dir.resolve("ñandú.txt");
    Files.writeString(source, "hola");
    api.respond(200, "ok".getBytes(UTF_8));

    client().transferFile(source, dir.resolve("salida"), "clave", true, (done, total) -> {});

    String body = new String(api.last().body(), ISO_8859_1);
    assertTrue(body.contains("filename*=UTF-8''%C3%B1and%C3%BA.txt"), body.substring(0, 260));
  }

  @Test
  void exigeLaCabeceraDeIntegridadAlDescifrar() throws Exception {
    Path source = dir.resolve("secreto.enc");
    Files.write(source, new byte[] {1, 2, 3});
    api.respond(200, new byte[] {9});

    IOException error =
        assertThrows(
            IOException.class,
            () -> client().transferFile(source, dir.resolve("salida"), "clave", false, (d, t) -> {}));
    assertTrue(error.getMessage().contains("X-Integrity"), error.getMessage());
  }

  @Test
  void aceptaLaIntegridadVerificadaSinDistinguirMayusculas() throws Exception {
    Path source = dir.resolve("secreto.enc");
    Files.write(source, new byte[] {1, 2, 3});
    api.respond(200, "recuperado".getBytes(UTF_8));
    api.header("X-Integrity", "VERIFIED");
    Path target = dir.resolve("salida");

    client().transferFile(source, target, "clave", false, (done, total) -> {});

    assertEquals("recuperado", Files.readString(target));
  }

  @Test
  void rechazaContrasenasVaciasOSConSaltosDeLinea() throws Exception {
    Path source = Files.writeString(dir.resolve("a.txt"), "x");
    Path target = dir.resolve("b.txt");

    IOException empty =
        assertThrows(
            IOException.class,
            () -> client().transferFile(source, target, "", true, (d, t) -> {}));
    assertTrue(empty.getMessage().contains("vacía"), empty.getMessage());

    IOException newline =
        assertThrows(
            IOException.class,
            () -> client().transferFile(source, target, "a\nb", true, (d, t) -> {}));
    assertTrue(newline.getMessage().contains("saltos de línea"), newline.getMessage());
  }

  @Test
  void informaDeErrorDeRedSinServidor() {
    api.close();
    IOException error = assertThrows(IOException.class, () -> client().checkAuth());
    assertTrue(error.getMessage().contains("contactar"), error.getMessage());
    assertNotNull(error.getCause(), "la causa original se conserva");
  }
}
