package org.example;

import java.util.prefs.Preferences;

/** Local connection preferences; cryptographic operations remain on the API server. */
public final class SettingsStore {
    private final Preferences preferences;
    private String baseUrl;
    private String token;

    public SettingsStore() {
        preferences = Preferences.userNodeForPackage(SettingsStore.class);
        baseUrl = normalize(preferences.get("server", "http://localhost:8000"));
        token = preferences.get("token", "");
    }

    public SettingsStore(String baseUrl, String token) {
        preferences = null;
        this.baseUrl = normalize(baseUrl);
        this.token = token == null ? "" : token.trim();
        validate();
    }

    public String baseUrl() { return baseUrl; }
    public String token() { return token; }

    public void update(String baseUrl, String token) {
        String normalized = normalize(baseUrl);
        String cleanToken = token == null ? "" : token.trim();
        validate(normalized, cleanToken);
        this.baseUrl = normalized;
        this.token = cleanToken;
        preferences.put("server", normalized);
        preferences.put("token", cleanToken);
    }

    private void validate() { validate(baseUrl, token); }

    private static void validate(String url, String token) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            if (uri.getHost() == null || !(uri.getScheme().equals("http") || uri.getScheme().equals("https")))
                throw new IllegalArgumentException("La dirección debe ser una URL HTTP(S), por ejemplo http://192.168.1.10:8000.");
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("La dirección del servidor no es válida. Usa http://IP:puerto.", ex);
        }
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Introduce el token de API configurado en el servidor.");
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        String result = value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        if (result.endsWith("/api/v1")) result = result.substring(0, result.length() - 7);
        return result;
    }
}
