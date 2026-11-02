package com.runnersz.reto2.android.security

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object StringEncryptor {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY = "CipherSprint2026!" // Clave simétrica para ofuscar strings sensibles

    fun encrypt(plainText: String): String {
        return try {
            val secretKey = SecretKeySpec(KEY.toByteArray(Charsets.UTF_8), "AES")
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(16)
            java.security.SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    fun decrypt(cipherText: String): String {
        return try {
            val combined = android.util.Base64.decode(cipherText, android.util.Base64.NO_WRAP)
            val iv = ByteArray(16)
            System.arraycopy(combined, 0, iv, 0, 16)
            val encrypted = ByteArray(combined.size - 16)
            System.arraycopy(combined, 16, encrypted, 0, encrypted.size)
            val secretKey = SecretKeySpec(KEY.toByteArray(Charsets.UTF_8), "AES")
            val cipher = Cipher.getInstance(ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            cipherText
        }
    }
}
