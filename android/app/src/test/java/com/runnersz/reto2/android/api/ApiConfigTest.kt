package com.runnersz.reto2.android.api

import org.junit.Test
import org.junit.Assert.*

class ApiConfigTest {

    @Test
    fun bearerTokenShouldBeSettable() {
        ApiConfig.bearerToken = "test-token-123"
        assertEquals("test-token-123", ApiConfig.bearerToken)
    }

    @Test
    fun baseUrlShouldBeSettable() {
        ApiConfig.baseUrl = "https://test.api.com/"
        assertEquals("https://test.api.com/", ApiConfig.baseUrl)
    }

    @Test
    fun endpointsShouldNotBeEmpty() {
        assertFalse(ApiConfig.ENDPOINT_HEALTH.isEmpty())
        assertFalse(ApiConfig.ENDPOINT_AUTH_CHECK.isEmpty())
        assertFalse(ApiConfig.ENDPOINT_CAESAR.isEmpty())
        assertFalse(ApiConfig.ENDPOINT_FILE_DECRYPT.isEmpty())
    }

    @Test
    fun timeoutShouldBe120Seconds() {
        assertEquals(120L, ApiConfig.TIMEOUT_SECONDS)
    }
}
