package com.runnersz.reto2.android.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftValidatorTest {

    @Test
    fun `acepta el rango completo de 1 a 25`() {
        for (shift in 1..25) {
            assertTrue("Debería aceptar $shift", ShiftValidator.esValido(shift))
        }
    }

    @Test
    fun `rechaza cero, negativos y mayores de 25`() {
        listOf(0, -1, 26, 100).forEach {
            assertFalse("No debería aceptar $it", ShiftValidator.esValido(it))
        }
    }
}
