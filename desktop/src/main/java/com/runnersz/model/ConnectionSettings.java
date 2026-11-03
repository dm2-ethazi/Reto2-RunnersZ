package com.runnersz.model;

/**
 * Datos inmutables de una conexión. No representan la contraseña AES. No se imprimen ni registran:
 * contienen el token de autenticación.
 */
public record ConnectionSettings(String baseUrl, String token) {
  @Override
  public String toString() {
    return "ConnectionSettings[credenciales ocultas]";
  }
}
