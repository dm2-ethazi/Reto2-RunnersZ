package com.runnersz.desktop.service;

import com.runnersz.desktop.model.ServerConfig;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** HTTP connection verification; invoke from a JavaFX Task. */
public final class ConnectionService {
    private final HttpClient client;

    public ConnectionService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public ConnectionService(HttpClient client) {
        this.client = java.util.Objects.requireNonNull(client);
    }

    public void check(ServerConfig config) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(config.endpoint("/api/v1/auth/check"))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + config.token())
                .GET()
                .build();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 401) {
            throw new IOException("Token no válido. Revisa los ajustes");
        }
        if (response.statusCode() != 204) {
            throw new IOException("No se pudo verificar la conexión (HTTP "
                    + response.statusCode() + ")");
        }
    }
}
