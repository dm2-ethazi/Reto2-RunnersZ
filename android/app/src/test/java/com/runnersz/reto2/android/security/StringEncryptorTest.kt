package com.runnersz.reto2.android.security

import com.runnersz.reto2.android.BuildConfig
import org.junit.Test
import org.junit.Assert.*

class StringEncryptorTest {

    @Test
    fun encryptAndDecryptShouldReturnOriginalText() {
        val original = "Texto secreto de prueba"
        val encrypted = StringEncryptor.encrypt(original)
        val decrypted = StringEncryptor.decrypt(encrypted)

        assertNotEquals(original, encrypted, "El texto cifrado no debe ser igual al original")
        assertEquals(original, decrypted, "El texto descifrado debe ser igual al original")
    }

    @Test
    fun encryptEmptyStringShouldReturnEmptyOrSame() {
        val encrypted = StringEncryptor.encrypt("")
        val decrypted = StringEncryptor.decrypt(encrypted)
        assertEquals("", decrypted)
    }

    @Test
    fun encryptLongTextShouldWork() {
        val original = "A".repeat(1000)
        val decrypted = StringEncryptor.decrypt(StringEncryptor.encrypt(original))
        assertEquals(original, decrypted)
    }

    @Test
    fun encryptSpecialCharactersShouldWork() {
        val original = "¡Hola! ¿Cómo estás? áéíóú ñ"
        val decrypted = StringEncryptor.decrypt(StringEncryptor.encrypt(original))
        assertEquals(original, decrypted)
    }
}
