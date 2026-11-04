package com.runnersz.reto2.android.security

/**
 * Reconstruye el token Bearer en memoria a partir de tres fragmentos enmascarados.
 *
 * No hay ningún literal del token en el DEX ni en recursos. R8 renombra y compacta
 * estas clases en release. El resultado solo se usa para construir la cabecera
 * Authorization de la petición y nunca se escribe en preferencias, logs ni UI.
 */
object SecureTokenProvider {

    private const val TOKEN_LENGTH = 20

    fun getToken(): String {
        val bytes = ByteArray(TOKEN_LENGTH)
        fill(CredentialA.DATA, 0, bytes)
        fill(CredentialB.DATA, 7, bytes)
        fill(CredentialC.DATA, 14, bytes)
        val token = String(bytes, Charsets.UTF_8)
        bytes.fill(0)
        return token
    }

    private fun fill(part: IntArray, start: Int, output: ByteArray) {
        for (offset in part.indices) {
            val index = start + offset
            val key = (index * 73 + 19) and 0xFF
            output[index] = (part[offset] xor key).toByte()
        }
    }
}
