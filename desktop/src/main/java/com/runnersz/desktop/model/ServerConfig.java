package com.runnersz.desktop.model;

import java.net.URI;

/** Connection data. Secrets are never included in diagnostic output. */
public record ServerConfig(String baseUrl, String token) {
    public ServerConfig {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Indica la dirección del servidor");
        }
        URI uri;
        try {
            uri = URI.create(baseUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Dirección del servidor no válida");
        }
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null
                || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))
                || uri.getPort() > 65535 || uri.getPort() == 0) {
            throw new IllegalArgumentException("Usa http(s)://servidor:puerto, sin rutas ni credenciales");
        }
        baseUrl = uri.toString().replaceAll("/+$", "");
        if (token == null || token.isBlank()
                || token.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Indica un token válido");
        }
    }

    public URI endpoint(String path) {
        if (path == null || !path.startsWith("/api/v1/")) {
            throw new IllegalArgumentException("Ruta de API no válida");
        }
        return URI.create(baseUrl + path);
    }

    @Override
    public String toString() {
        return "ServerConfig[baseUrl=" + baseUrl + ", token=***]";
    }
}
