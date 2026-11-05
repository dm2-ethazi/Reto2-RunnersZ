package com.runnersz.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.prefs.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de los ajustes: valores vacíos por defecto, normalización y validación. */
class SettingsStoreTest {
  private Preferences node;

  @BeforeEach
  void clean() {
    node =
        Preferences.userRoot()
            .node("/com/runnersz/pruebas/" + System.nanoTime());
  }

  @Test
  void arrancaSinDireccionNiTokenEscritos() {
    SettingsStore store = new SettingsStore(node, false);
    assertEquals("", store.baseUrl());
    assertEquals("", store.token());
    assertFalse(store.isConfigured());
  }

  @Test
  void normalizaBarrasFinalesYLaRutaDeLaApi() {
    SettingsStore store = new SettingsStore(node, false);
    store.update("https://servidor.ejemplo:8000/api/v1/", "token");
    assertEquals("https://servidor.ejemplo:8000", store.baseUrl());
    assertTrue(store.isConfigured());
  }

  @Test
  void guardaEnLaPreferenciaIndicada() {
    SettingsStore store = new SettingsStore(node, false);
    store.update("http://192.168.1.10:8000", "secreto");
    assertEquals("http://192.168.1.10:8000", node.get("server", ""));
    assertEquals("secreto", node.get("token", ""));
  }

  @Test
  void validaElEsquemaYElHost() {
    assertThrows(
        IllegalArgumentException.class, () -> new SettingsStore("ftp://host:21", "token"));
    assertThrows(IllegalArgumentException.class, () -> new SettingsStore("sin-esquema", "token"));
    assertThrows(IllegalArgumentException.class, () -> new SettingsStore("http://", "token"));
  }

  @Test
  void exigeTokenNoVacio() {
    assertThrows(IllegalArgumentException.class, () -> new SettingsStore("http://host:8000", " "));
    assertThrows(
        IllegalArgumentException.class, () -> new SettingsStore("http://host:8000", null));
  }

  @Test
  void laInstantaneaNoCambiaAlEditarLosAjustes() {
    SettingsStore store = new SettingsStore(node, false);
    store.update("http://host:8000", "uno");
    var snapshot = store.snapshot();
    store.update("http://host:9000", "dos");
    assertEquals("http://host:8000", snapshot.baseUrl());
    assertEquals("uno", snapshot.token());
  }

  @Test
  void migraLosAjustesDelNodoHeredado() throws Exception {
    Preferences legacy = Preferences.userRoot().node("/org/example");
    legacy.put("server", "http://heredado:8000");
    legacy.put("token", "token-heredado");

    SettingsStore migrated = new SettingsStore(node, true);

    assertEquals("http://heredado:8000", migrated.baseUrl());
    assertEquals("token-heredado", migrated.token());
    legacy.removeNode();
  }

  @Test
  void noImprimeCredenciales() {
    assertEquals(
        "ConnectionSettings[credenciales ocultas]",
        new SettingsStore("http://host:8000", "token").snapshot().toString());
  }
}
