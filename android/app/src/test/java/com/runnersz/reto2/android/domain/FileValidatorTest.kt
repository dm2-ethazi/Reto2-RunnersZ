package com.runnersz.reto2.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @author jairoben
 */
class FileValidatorTest {

    @Test
    fun `acepta un enc con contenido dentro del limite`() {
        assertNull(FileValidator.problema("image.png.enc", 2_048L))
        assertNull(FileValidator.problema("IMAGEN.ENC", 1L))
    }

    @Test
    fun `rechaza archivos vacios`() {
        assertEquals(ProblemaArchivo.VACIO, FileValidator.problema("vacio.enc", 0L))
    }

    @Test
    fun `rechaza archivos mayores de 100 MiB`() {
        assertEquals(
            ProblemaArchivo.DEMASIADO_GRANDE,
            FileValidator.problema("grande.enc", 100L * 1024L * 1024L + 1L),
        )
    }

    @Test
    fun `rechaza archivos que no son enc`() {
        assertEquals(ProblemaArchivo.EXTENSION, FileValidator.problema("orden.txt", 120L))
        assertEquals(ProblemaArchivo.EXTENSION, FileValidator.problema("foto.png", 120L))
    }
}
