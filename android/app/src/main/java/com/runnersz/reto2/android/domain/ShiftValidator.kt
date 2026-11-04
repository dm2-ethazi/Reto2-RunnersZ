package com.runnersz.reto2.android.domain

object ShiftValidator {
    const val MENSAJE = "El desplazamiento debe ser un número entero entre 1 y 25"

    fun esValido(shift: Int): Boolean = shift in 1..25
}
