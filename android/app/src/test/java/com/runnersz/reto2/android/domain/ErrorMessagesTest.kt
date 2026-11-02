package com.runnersz.reto2.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del mapa de errores a mensajes en español (§5.1 y §7.2).
 */
class ErrorMessagesTest {

    @Test
    fun `unauthorized se traduce a token no valido con accion de ajustes`() {
        val e = ErrorMessages.porCodigo("UNAUTHORIZED", 401)
        assertEquals("Token no válido. Revisa los ajustes", e.texto)
        assertEquals(AccionError.IR_A_AJUSTES, e.accion)
    }

    @Test
    fun `invalid_shift recuerda el rango 1 a 25`() {
        assertEquals(
            "El desplazamiento debe ser un entero entre 1 y 25",
            ErrorMessages.porCodigo("INVALID_SHIFT", 400).texto,
        )
    }

    @Test
    fun `los codigos del servidor mandan sobre el estado http`() {
        // 500 en el estado, pero el code dice EMPTY_FILE: manda el code.
        assertEquals("El archivo está vacío", ErrorMessages.porCodigo("EMPTY_FILE", 500).texto)
    }

    @Test
    fun `codigo desconocido cae al estado http`() {
        val e = ErrorMessages.porCodigo("RARO", 413)
        assertEquals("El archivo supera el tamaño máximo permitido", e.texto)
    }

    @Test
    fun `sin codigo usa el estado http`() {
        assertEquals("Petición no válida", ErrorMessages.porCodigo(null, 400).texto)
    }

    @Test
    fun `sin conexion ofrece reintentar`() {
        val e = ErrorMessages.sinConexion()
        assertEquals(ErrorMessages.SIN_CONEXION, e.texto)
        assertEquals(AccionError.REINTENTAR, e.accion)
    }

    @Test
    fun `dj7 distingue integridad no confirmada de caida de red`() {
        val e = ErrorMessages.sinIntegridad()
        assertEquals("El servidor no confirmó la integridad", e.texto)
        // No debe confundirse con una caída de red: el servidor sí respondió.
        assertNotEquals(ErrorMessages.SIN_CONEXION, e.texto)
        assertEquals(AccionError.REINTENTAR, e.accion)
    }

    @Test
    fun `estado sin mapear no se queda vacio`() {
        val e = ErrorMessages.porCodigo(null, 503)
        assertTrue(e.texto.isNotBlank())
        assertEquals(AccionError.REINTENTAR, e.accion)
    }

    @Test
    fun `decrypt_failed usa tono comprensible`() {
        assertEquals(
            "Clave incorrecta o fichero dañado",
            ErrorMessages.porCodigo("DECRYPT_FAILED", 400).texto,
        )
        assertFalse(ErrorMessages.porCodigo("DECRYPT_FAILED", 400).texto.contains("Exception"))
    }
}
