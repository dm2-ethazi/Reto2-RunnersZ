package com.runnersz.service;

import com.runnersz.model.ConnectionSettings;
import com.runnersz.utiles.I18n;
import java.net.URI;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Lee, valida y guarda los ajustes de conexión del usuario. No hay dirección ni token escritos en
 * el código: ambos se configuran desde Ajustes. La contraseña AES nunca pasa por esta clase.
 *
 * @author RunnersZ
 */
public final class SettingsStore {
  private static final String NODE = "/com/runnersz";
  private static final String LEGACY = "/org/example";

  private final Preferences preferences;
  private String baseUrl;
  private String token;

  public SettingsStore() {
    this(Preferences.userRoot().node(NODE), true);
  }

  /** Constructor de pruebas: comparte el nodo indicado y no migra nada. */
  SettingsStore(Preferences preferences, boolean migrate) {
    this.preferences = preferences;
    if (migrate) migrateLegacy();
    baseUrl = normalize(preferences.get("server", ""));
    token = preferences.get("token", "");
  }

  /** Configuración candidata para probar la conexión antes de guardar. */
  public SettingsStore(String baseUrl, String token) {
    preferences = null;
    this.baseUrl = normalize(baseUrl);
    this.token = token == null ? "" : token.trim();
    validate(this.baseUrl, this.token);
  }

  /** Instantánea de los ajustes: una operación en curso no cambia al editar Ajustes. */
  public ConnectionSettings snapshot() {
    return new ConnectionSettings(baseUrl, token);
  }

  /**
   * Dirección base configurada, sin barra final ni {@code /api/v1}.
   *
   * @return la dirección, o cadena vacía si aún no se ha configurado
   */
  public String baseUrl() {
    return baseUrl;
  }

  /**
   * Token Bearer configurado.
   *
   * @return el token, o cadena vacía si aún no se ha configurado
   */
  public String token() {
    return token;
  }

  /** Sin dirección o sin token no se puede llamar a la API. */
  public boolean isConfigured() {
    return !baseUrl.isBlank() && !token.isBlank();
  }

  /**
   * Valida y guarda los ajustes en las preferencias locales.
   *
   * @param baseUrl dirección base del servidor
   * @param token token Bearer de la API
   * @throws IllegalArgumentException si la dirección o el token no son válidos
   */
  public void update(String baseUrl, String token) {
    String normalized = normalize(baseUrl);
    String cleanToken = token == null ? "" : token.trim();
    validate(normalized, cleanToken);
    this.baseUrl = normalized;
    this.token = cleanToken;
    if (preferences != null) {
      preferences.put("server", normalized);
      preferences.put("token", cleanToken);
      flush(preferences);
    }
  }

  private void migrateLegacy() {
    try {
      if (preferences.keys().length > 0) return;
      Preferences legacy = Preferences.userRoot().node(LEGACY);
      if (legacy.keys().length == 0) return;
      for (String key : legacy.keys()) preferences.put(key, legacy.get(key, ""));
      flush(preferences);
    } catch (BackingStoreException | IllegalStateException ignored) {
      // La migración es una comodidad: si falla, el usuario vuelve a escribir los ajustes.
    }
  }

  private static void flush(Preferences preferences) {
    try {
      preferences.flush();
    } catch (BackingStoreException ignored) {
      // Las preferencias se guardan igualmente al cerrar la aplicación.
    }
  }

  private static void validate(String url, String token) {
    if (url == null || url.isBlank()) throw I18n.invalid("error.url");
    try {
      URI uri = URI.create(url);
      if (uri.getHost() == null
          || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
        throw I18n.invalid("error.url");
      }
    } catch (IllegalArgumentException ex) {
      throw I18n.invalid("error.url");
    }
    if (token == null || token.isBlank()) throw I18n.invalid("error.token");
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) return "";
    String result = value.trim();
    while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
    if (result.endsWith("/api/v1")) result = result.substring(0, result.length() - 7);
    return result;
  }
}
