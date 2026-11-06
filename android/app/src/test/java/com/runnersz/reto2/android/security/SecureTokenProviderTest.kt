package com.runnersz.reto2.android.security

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author jairoben
 */
class SecureTokenProviderTest {

    @Test
    fun `reconstruye exactamente el token esperado sin literal`() {
        val token = SecureTokenProvider.getToken()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") {
            (it.toInt() and 0xFF).toString(16).padStart(2, '0')
        }

        assertEquals(20, token.length)
        assertEquals(
            "cfb22211f476c296f63ae6d34b4f889f5fd12da1e33ba3a478e656177709d583",
            hex,
        )
    }
}
