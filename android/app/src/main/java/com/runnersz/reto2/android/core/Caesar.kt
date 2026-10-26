package com.runnersz.reto2.android.core

class InvalidShiftException(message: String = "El desplazamiento debe ser un entero entre 1 y 25.") :
    Exception(message)

object Caesar {
    fun encrypt(text: String, shift: Int): String = transform(text, shift, decrypting = false)

    fun decrypt(text: String, shift: Int): String = transform(text, shift, decrypting = true)

    fun transform(text: String, shift: Int, decrypting: Boolean): String {
        if (shift !in 1..25) {
            throw InvalidShiftException()
        }
        val movement = if (decrypting) -shift else shift
        val result = StringBuilder(text.length)
        for (character in text) {
            when {
                character in 'A'..'Z' -> result.append(rotate(character, 'A', movement))
                character in 'a'..'z' -> result.append(rotate(character, 'a', movement))
                else -> result.append(character)
            }
        }
        return result.toString()
    }

    private fun rotate(character: Char, base: Char, movement: Int): Char {
        val normalized = (character - base + movement) % 26
        val positive = if (normalized < 0) normalized + 26 else normalized
        return (base + positive)
    }
}
