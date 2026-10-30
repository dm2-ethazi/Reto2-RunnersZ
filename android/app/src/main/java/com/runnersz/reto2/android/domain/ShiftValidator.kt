package com.runnersz.reto2.android.domain

/**
 * Validación del desplazamiento de César (§6.3).
 *
 * El contrato admite **enteros de 1 a 25**; el servidor devuelve `INVALID_SHIFT` fuera de
 * ese rango, pero la app valida en local antes de llamar para no hacer la petición inútil.
 */
object ShiftValidator {

    const val RANGO_MINIMO = 1
    const val RANGO_MAXIMO = 25
    const val MENSAJE = "El desplazamiento debe ser un entero entre 1 y 25"

    fun esValido(shift: Int): Boolean = shift in RANGO_MINIMO..RANGO_MAXIMO

    /** Devuelve el desplazamiento si es válido; si no, lanza con el mensaje en español. */
    fun validar(shift: Int): Int {
        require(esValido(shift)) { MENSAJE }
        return shift
    }
}
