package com.runnersz.reto2.android.domain

/**
 * Acción sugerida junto a un mensaje de error.
 */
enum class AccionError {
    NINGUNA,
    IR_A_AJUSTES,
    REINTENTAR,
}

/**
 * Mensaje de error mostrado a la persona usuaria, en español y con su acción sugerida.
 *
 * @param texto mensaje en español (§5.1, «Mapa de errores a mensajes»).
 * @param accion acción que se ofrece junto al mensaje.
 */
data class ErrorDeLaApp(val texto: String, val accion: AccionError = AccionError.NINGUNA)

/**
 * Mapa entre los códigos `code` del JSON `{code,message}` de la API (§7.2) y los mensajes
 * de la app. El `code` del servidor **manda** sobre el `message`; si no hay JSON válido,
 * se usa el mensaje genérico asociado al estado HTTP.
 *
 * El mismo tono que el escritorio: sin tecnicismos y sin cerrar la app nunca (§7.2).
 */
object ErrorMessages {

    const val SIN_CONEXION = "No se pudo conectar con el servidor"

    /** DJ7: el servidor respondió 200 pero sin `X-Integrity: verified`. */
    const val INTEGRIDAD_NO_CONFIRMADA = "El servidor no confirmó la integridad"

    private val genericosPorEstado = mapOf(
        400 to ErrorDeLaApp("Petición no válida"),
        401 to ErrorDeLaApp("Token no válido. Revisa los ajustes", AccionError.IR_A_AJUSTES),
        413 to ErrorDeLaApp("El archivo supera el tamaño máximo permitido"),
        500 to ErrorDeLaApp("Error del servidor. Inténtalo de nuevo", AccionError.REINTENTAR),
    )

    private val porCodigo = mapOf(
        "UNAUTHORIZED" to ErrorDeLaApp("Token no válido. Revisa los ajustes", AccionError.IR_A_AJUSTES),
        "EMPTY_FILE" to ErrorDeLaApp("El archivo está vacío"),
        "EMPTY_PASSWORD" to ErrorDeLaApp("Introduce la clave"),
        "INVALID_KEY" to ErrorDeLaApp("La clave no es válida"),
        "INVALID_SHIFT" to ErrorDeLaApp("El desplazamiento debe ser un entero entre 1 y 25"),
        "FILE_TOO_LARGE" to ErrorDeLaApp("El archivo supera el tamaño máximo permitido"),
        "DECRYPT_FAILED" to ErrorDeLaApp("Clave incorrecta o fichero dañado"),
        "BAD_REQUEST" to ErrorDeLaApp("Petición no válida"),
        "INTERNAL_ERROR" to ErrorDeLaApp("Error del servidor. Inténtalo de nuevo", AccionError.REINTENTAR),
        "UNREADABLE_FILE" to ErrorDeLaApp("No se pudo leer el archivo"),
    )

    /** Error por código del servidor; si el código es desconocido, cae al estado HTTP. */
    fun porCodigo(code: String?, estadoHttp: Int): ErrorDeLaApp =
        porCodigo[code] ?: porEstado(estadoHttp)

    /** Error genérico por estado HTTP, para respuestas sin JSON válido. */
    fun porEstado(estadoHttp: Int): ErrorDeLaApp =
        genericosPorEstado[estadoHttp]
            ?: ErrorDeLaApp("Error del servidor ($estadoHttp)", AccionError.REINTENTAR)

    /** Fallo de red (sin respuesta o tiempo agotado). */
    fun sinConexion(): ErrorDeLaApp =
        ErrorDeLaApp(SIN_CONEXION, AccionError.REINTENTAR)

    /** DJ7: un 200 sin cabecera `X-Integrity: verified` no entrega el contenido. */
    fun sinIntegridad(): ErrorDeLaApp =
        ErrorDeLaApp(INTEGRIDAD_NO_CONFIRMADA, AccionError.REINTENTAR)
}
