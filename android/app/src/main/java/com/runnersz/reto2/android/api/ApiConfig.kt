package com.runnersz.reto2.android.api

object ApiConfig {
    // URL base de la API. Se configura en Ajustes (IP:puerto) y se guarda localmente.
    // Valor por defecto para pruebas rápidas; en producción se edita desde la app.
    var baseUrl: String = "https://runnersz-api.onrender.com/"

    // Bearer token. Se configura en Ajustes y se cifra en EncryptedSharedPreferences.
    var bearerToken: String = ""

    // Endpoints (todos bajo /api/v1)
    const val ENDPOINT_HEALTH = "/api/v1/health"
    const val ENDPOINT_AUTH_CHECK = "/api/v1/auth/check"
    const val ENDPOINT_CAESAR = "/api/v1/text/caesar"
    const val ENDPOINT_FILE_DECRYPT = "/api/v1/files/decrypt"

    // Headers
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val HEADER_ACCEPT = "Accept"
    const val HEADER_X_INTEGRITY = "X-Integrity"

    // Timeout máximo: 120 s (según contrato 7.2)
    const val TIMEOUT_SECONDS = 120L
}
