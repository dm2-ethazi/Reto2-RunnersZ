package com.runnersz.service;

import com.runnersz.model.ConnectionSettings;
import java.util.prefs.Preferences;

/**
 * Lee, valida y guarda los ajustes de conexión del usuario. La contraseña AES nunca pasa por esta
 * clase ni se almacena en preferencias.
 */
public final class SettingsStore {
  private final Preferences preferences;
  private String baseUrl;
  private String token;

  public SettingsStore() {
    // Conserva el nodo histórico para no perder los ajustes al cambiar de paquete.
    preferences = Preferences.userRoot().node("/org/example");
    baseUrl = normalize(preferences.get("server", "http://localhost:8000"));
    token = preferences.get("token", "");
  }

  /** Configuración candidata para probar la conexión antes de guardar. */
  public SettingsStore(String baseUrl, String token) {
    preferences = null;
    this.baseUrl = normalize(baseUrl);
    this.token = token == null ? "" : token.trim();
    validate();
  }

  /** Instantánea de los ajustes: una operación en curso no cambia al editar Ajustes. */
  public ConnectionSettings snapshot() {
    return new ConnectionSettings(baseUrl, token);
  }

  public String baseUrl() {
    return baseUrl;
  }

  public String token() {
    return token;
  }

  public void update(String baseUrl, String token) {
    String normalized = normalize(baseUrl);
    String cleanToken = token == null ? "" : token.trim();
    validate(normalized, cleanToken);
    this.baseUrl = normalized;
    this.token = cleanToken;
    if (preferences != null) {
      preferences.put("server", normalized);
      preferences.put("token", cleanToken);
    }
  }

  private void validate() {
    validate(baseUrl, token);
  }

  private static void validate(String url, String token) {
    try {
      java.net.URI uri = java.net.URI.create(url);
      if (uri.getHost() == null
          || !(uri.getScheme().equals("http") || uri.getScheme().equals("https")))
        throw new IllegalArgumentException(
            "La dirección debe ser una URL HTTP(S), por ejemplo http://192.168.1.10:8000.");
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          "La dirección del servidor no es válida. Usa http://IP:puerto.", ex);
    }
    if (token == null || token.isBlank())
      throw new IllegalArgumentException("Introduce el token de API configurado en el servidor.");
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) return "";
    String result = value.trim();
    while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
    if (result.endsWith("/api/v1")) result = result.substring(0, result.length() - 7);
    return result;
  }
}
