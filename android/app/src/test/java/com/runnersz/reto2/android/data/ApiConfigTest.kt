package com.runnersz.reto2.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author jairoben
 */
class ApiConfigTest {

    @Test
    fun `el servidor es HTTPS y no tiene barra final`() {
        assertTrue(ApiConfig.BASE_URL.startsWith("https://"))
        assertFalse(ApiConfig.BASE_URL.endsWith("/"))
    }

    @Test
    fun `el limite de subida es de 100 MiB`() {
        assertEquals(104_857_600L, ApiConfig.MAX_UPLOAD_BYTES)
    }
}
