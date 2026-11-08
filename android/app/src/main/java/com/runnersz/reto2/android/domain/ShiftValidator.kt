package com.runnersz.reto2.android.domain

/**
 * Valida el desplazamiento del cifrado César y guarda el mensaje que se muestra al usuario.
 *
 * @author Jairo
 */
object ShiftValidator {
    const val MENSAJE = "El desplazamiento debe ser un número entero entre 1 y 25"

/**
 * Comprueba que el desplazamiento está entre 1 y 25.
 *
 *  * @param shift valor introducido por el usuario
 */
    fun esValido(shift: Int): Boolean = shift in 1..25
}
