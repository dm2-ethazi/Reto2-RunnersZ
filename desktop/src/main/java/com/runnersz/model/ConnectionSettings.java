package com.runnersz.model;

/**
 * Datos inmutables de una conexión. No representan la contraseña AES. No se imprimen ni registran:
 * contienen el token de autenticación.
 *
 * @param baseUrl dirección base del servidor, sin {@code /api/v1}
 * @param token token Bearer de la API
 * @author Jairo y Alain
 */
public record ConnectionSettings(String baseUrl, String token) {
  @Override
  public String toString() {
    return "ConnectionSettings[credenciales ocultas]";
  }
}
