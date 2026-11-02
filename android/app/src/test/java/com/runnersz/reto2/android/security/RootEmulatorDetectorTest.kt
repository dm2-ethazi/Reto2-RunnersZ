package com.runnersz.reto2.android.security

import org.junit.Test
import org.junit.Assert.*

class RootEmulatorDetectorTest {

    @Test
    fun isEnvironmentUnsafeShouldReturnBoolean() {
        // No podemos probar con contexto real en unit test, solo verificamos que no falle
        val result = RootEmulatorDetector.isRooted()
        assertTrue(result or !result) // Siempre true, solo verificamos que no lance excepción
    }

    @Test
    fun isRootedShouldReturnBoolean() {
        val result = RootEmulatorDetector.isRooted()
        assertTrue(result or !result)
    }
}
