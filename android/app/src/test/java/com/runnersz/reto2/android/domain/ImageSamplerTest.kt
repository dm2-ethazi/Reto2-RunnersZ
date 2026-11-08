package com.runnersz.reto2.android.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Comprueba el cálculo del factor de muestreo de [ImageSampler].
 *
 * @author Jairo
 */
class ImageSamplerTest {

    @Test
    fun `no reduce imagenes que ya caben`() {
        assertEquals(1, ImageSampler.sampleSizeFor(1080, 1920))
        assertEquals(1, ImageSampler.sampleSizeFor(2048, 2048))
    }

    @Test
    fun `reduce por potencias de dos hasta caber`() {
        assertEquals(2, ImageSampler.sampleSizeFor(3000, 2000))
        assertEquals(4, ImageSampler.sampleSizeFor(6000, 8000))
        assertEquals(16, ImageSampler.sampleSizeFor(20000, 12000))
    }

    @Test
    fun `tolera valores invalidos`() {
        assertEquals(1, ImageSampler.sampleSizeFor(0, 0))
        assertEquals(1, ImageSampler.sampleSizeFor(-10, -10))
    }
}
