package com.runnersz.utiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Pruebas de los catálogos: paridad entre idiomas, claves de FXML y errores traducidos. */
class I18nTest {
  private static final String BUNDLE = "com.runnersz.i18n.messages";

  @AfterEach
  void reset() {
    I18n.setLanguage("es");
  }

  @Test
  void losCuatroCatalogosTienenLasMismasClaves() {
    ResourceBundle.Control strict =
        ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);
    ResourceBundle base = ResourceBundle.getBundle(BUNDLE, Locale.ROOT, strict);
    for (String tag : List.of("es", "eu", "en")) {
      ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, Locale.forLanguageTag(tag), strict);
      assertEquals(base.keySet(), bundle.keySet(), "claves de " + tag);
    }
  }

  @Test
  void todasLasClavesDelFxmlExisten() throws IOException {
    ResourceBundle base =
        ResourceBundle.getBundle(
            BUNDLE,
            Locale.ROOT,
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
    String fxml;
    try (InputStream stream = I18nTest.class.getResourceAsStream("/com/runnersz/desktop.fxml")) {
      assertNotNull(stream, "desktop.fxml en el classpath");
      fxml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
    Matcher matcher = Pattern.compile("%([A-Za-z0-9_.]+)").matcher(fxml);
    Set<String> missing = new HashSet<>();
    while (matcher.find()) {
      if (!base.containsKey(matcher.group(1))) missing.add(matcher.group(1));
    }
    assertTrue(missing.isEmpty(), "claves sin traducir: " + missing);
  }

  @Test
  void traduceYFormateaEnCadaIdioma() {
    I18n.setLanguage("es");
    assertEquals("3 caracteres", I18n.text("text.counter", 3));
    I18n.setLanguage("eu");
    assertEquals("3 karaktere", I18n.text("text.counter", 3));
    I18n.setLanguage("en");
    assertEquals("3 characters", I18n.text("text.counter", 3));
  }

  @Test
  void ignoraIdiomasNoSoportados() {
    I18n.setLanguage("fr");
    assertEquals("es", I18n.locale().getLanguage());
  }

  @Test
  void elBundleCorrespondeAlIdiomaActivo() {
    I18n.setLanguage("en");
    assertEquals("Ready", I18n.bundle().getString("status.ready"));
  }

  @Test
  void traduceLosFallosConSuClave() {
    IOException failure = I18n.failure("error.emptyFile");
    assertTrue(failure.getMessage().contains("vacío"), failure.getMessage());

    IOException http = I18n.httpFailure(413, "error.FILE_TOO_LARGE");
    assertTrue(http.getMessage().contains("413"), http.getMessage());
    assertTrue(http.getMessage().contains("tamaño"), http.getMessage());

    assertEquals(I18n.text("error.original"), I18n.errorMessage(I18n.invalid("error.original")));
    assertEquals(I18n.text("error.access"), I18n.errorMessage(new AccessDeniedException("x")));
    assertEquals(I18n.text("error.generic"), I18n.errorMessage(new IllegalStateException()));
    assertEquals(
        I18n.text("error.access"),
        I18n.errorMessage(new RuntimeException(new AccessDeniedException("negado"))));
  }

  @Test
  void conservaElMensajeDeUnFalloEnvoltorio() {
    IOException failure = I18n.failure("error.emptyFile");
    RuntimeException wrapper = new RuntimeException(failure);
    assertTrue(I18n.errorMessage(wrapper).contains("vacío"));
  }

  @Test
  void lasClavesDeAccesibilidadNoEstanVacias() {
    ResourceBundle base =
        ResourceBundle.getBundle(
            BUNDLE,
            Locale.ROOT,
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
    for (String key : base.keySet()) {
      assertFalse(base.getString(key).isBlank(), "clave vacía: " + key);
    }
  }
}
