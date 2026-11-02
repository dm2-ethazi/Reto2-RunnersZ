package com.runnersz.reto2.android.domain

/**
 * Resultado de analizar la dirección del servidor.
 *
 * DJ8: el formato es el mismo que en el escritorio (`host:puerto`, puerto 1–65535).
 */
sealed class Direccion {
    /** Dirección ya normalizada como `http://host:puerto`. */
    data class Valida(val url: String) : Direccion()

    /**
     * Dirección rechazada.
     *
     * @param aviso `true` cuando es solo un aviso (p. ej. `https://`) y no un error de formato.
     */
    data class Invalida(val motivo: String, val aviso: Boolean = false) : Direccion()
}

/**
 * Analiza y normaliza la dirección del servidor introducida por la persona usuaria (DJ8).
 *
 * Reglas:
 * - se recortan espacios al principio y al final;
 * - se acepta el prefijo `http://` y se guarda igual;
 * - `https://` se rechaza con aviso (el servidor de la red local usa HTTP);
 * - se exige `host:puerto` con puerto entre 1 y 65535;
 * - la dirección **no está fija en el código**: siempre sale de los ajustes.
 */
object AddressParser {

    const val AVISO_HTTPS = "El servidor usa HTTP, no HTTPS"
    const val FALTA_HOST = "Falta la dirección del servidor"
    const val FALTA_PUERTO = "Falta el puerto: usa host:puerto, p. ej. 192.168.1.20:8000"
    const val PUERTO_INVALIDO = "El puerto debe ser un número entre 1 y 65535"

    /** Prefijo HTTP admitido. */
    private const val PREFIJO_HTTP = "http://"
    private const val PREFIJO_HTTPS = "https://"

    fun parse(entrada: String): Direccion {
        val texto = entrada.trim()
        if (texto.isEmpty()) return Direccion.Invalida(FALTA_HOST)

        if (texto.lowercase().startsWith(PREFIJO_HTTPS)) {
            return Direccion.Invalida(AVISO_HTTPS, aviso = true)
        }
        val sinEsquema = if (texto.lowercase().startsWith(PREFIJO_HTTP)) {
            texto.substring(PREFIJO_HTTP.length)
        } else {
            texto
        }

        val corte = sinEsquema.lastIndexOf(':')
        if (corte <= 0) return Direccion.Invalida(FALTA_PUERTO)
        if (corte == sinEsquema.length - 1) return Direccion.Invalida(PUERTO_INVALIDO)

        val host = sinEsquema.substring(0, corte).trim()
        val puertoTexto = sinEsquema.substring(corte + 1).trim()
        if (host.isEmpty()) return Direccion.Invalida(FALTA_HOST)

        val puerto = puertoTexto.toIntOrNull() ?: return Direccion.Invalida(PUERTO_INVALIDO)
        if (puerto !in 1..65535) return Direccion.Invalida(PUERTO_INVALIDO)

        return Direccion.Valida("$PREFIJO_HTTP$host:$puerto")
    }
}
