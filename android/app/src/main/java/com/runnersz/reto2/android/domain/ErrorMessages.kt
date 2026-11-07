package com.runnersz.reto2.android.domain

/**
 * @author jairoben
 */
data class ErrorMensaje(val texto: String)

/**
 * @author jairoben
 */
object ErrorMessages {
    const val SIN_CONEXION = "No se pudo conectar con el servidor"
    const val RESPUESTA_INVALIDA = "Respuesta inesperada del servidor"

    fun sinConexion(): ErrorMensaje = ErrorMensaje(SIN_CONEXION)

    fun respuestaInvalida(): ErrorMensaje = ErrorMensaje(RESPUESTA_INVALIDA)

    fun sinIntegridad(): ErrorMensaje =
        ErrorMensaje("El servidor no confirmó la integridad del archivo")

    fun porCodigo(codigo: String?, estadoHttp: Int): ErrorMensaje {
        when (codigo?.uppercase()) {
            "UNAUTHORIZED" -> return ErrorMensaje("No se pudo autenticar con el servidor")
            "FORBIDDEN" -> return ErrorMensaje("No se pudo autenticar con el servidor")
            "INVALID_SHIFT" -> return ErrorMensaje("El desplazamiento debe ser un número entero entre 1 y 25")
            "EMPTY_FILE" -> return ErrorMensaje("El archivo está vacío")
            "FILE_TOO_LARGE" -> return ErrorMensaje("El archivo supera el límite de 100 MiB")
            "EMPTY_PASSWORD" -> return ErrorMensaje("Introduce la clave AES")
            "INVALID_FORMAT" -> return ErrorMensaje("El archivo no tiene un formato .enc válido")
            "RATE_LIMITED", "TOO_MANY_REQUESTS" ->
                return ErrorMensaje("Demasiadas peticiones. Espera un momento")
            "DECRYPT_FAILED", "INTEGRITY_FAILED" ->
                return ErrorMensaje("Clave incorrecta o archivo dañado")
            "INTERNAL_ERROR" -> return ErrorMensaje("Error del servidor. Inténtalo de nuevo")
        }

        return when (estadoHttp) {
            400 -> ErrorMensaje("La petición no es válida")
            401, 403 -> ErrorMensaje("No se pudo autenticar con el servidor")
            413 -> ErrorMensaje("El archivo supera el límite de 100 MiB")
            422 -> ErrorMensaje("Clave incorrecta o archivo dañado")
            429 -> ErrorMensaje("Demasiadas peticiones. Espera un momento")
            500 -> ErrorMensaje("Error del servidor. Inténtalo de nuevo")
            502, 503, 504 -> ErrorMensaje("Servicio no disponible temporalmente")
            else -> ErrorMensaje("Error de comunicación ($estadoHttp)")
        }
    }
}
