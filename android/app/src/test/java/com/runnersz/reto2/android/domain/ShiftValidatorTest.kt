package com.runnersz.reto2.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del validador de desplazamiento de César (§6.3: enteros de 1 a 25).
 */
class ShiftValidatorTest {

    @Test
    fun `acepta los extremos del rango`() {
        assertTrue(ShiftValidator.esValido(1))
        assertTrue(ShiftValidator.esValido(25))
        assertEquals(1, ShiftValidator.validar(1))
        assertEquals(25, ShiftValidator.validar(25))
    }

    @Test
    fun `acepta un valor intermedio`() {
        assertTrue(ShiftValidator.esValido(13))
    }

    @Test
    fun `rechaza el cero y los negativos`() {
        assertFalse(ShiftValidator.esValido(0))
        assertFalse(ShiftValidator.esValido(-3))
    }

    @Test
    fun `rechaza 26 y valores superiores`() {
        assertFalse(ShiftValidator.esValido(26))
        assertFalse(ShiftValidator.esValido(99))
    }

    @Test
    fun `validar lanza con el mensaje en espanol`() {
        val e = runCatching { ShiftValidator.validar(26) }.exceptionOrNull()
        requireNotNull(e)
        assertEquals(ShiftValidator.MENSAJE, e.message)
    }
}
