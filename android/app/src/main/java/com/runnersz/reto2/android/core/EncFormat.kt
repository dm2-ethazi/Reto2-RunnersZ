package com.runnersz.reto2.android.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class EncHeader(
    val salt: ByteArray,
    val iterations: Int,
    val noncePrefix: ByteArray,
    val blockSize: Int,
)

internal object EncFormat {
    val MAGIC = byteArrayOf(0x48, 0x47, 0x41, 0x45)
    const val VERSION = 1
    const val HEADER_SIZE = 37
    const val TAG_SIZE = 16
    const val DIGEST_SIZE = 32
    const val MIN_ITERATIONS = 100_000
    const val MAX_ITERATIONS = 2_000_000
    const val MIN_BLOCK_SIZE = 64 * 1024
    const val MAX_BLOCK_SIZE = 16 * 1024 * 1024

    fun parse(raw: ByteArray): EncHeader {
        if (raw.size != HEADER_SIZE) {
            throw AesFormatException("La cabecera .enc está incompleta.")
        }
        val buffer = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN)
        val magic = ByteArray(4)
        buffer.get(magic)
        if (!magic.contentEquals(MAGIC)) {
            throw AesFormatException("La marca del fichero no es HGAE.")
        }
        val version = buffer.get().toInt() and 0xFF
        if (version != VERSION) {
            throw AesFormatException("Versión de formato no soportada: $version.")
        }
        val salt = ByteArray(16)
        buffer.get(salt)
        val iterations = buffer.int
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) {
            throw AesFormatException("Iteraciones PBKDF2 fuera de rango: $iterations.")
        }
        val noncePrefix = ByteArray(8)
        buffer.get(noncePrefix)
        val blockSize = buffer.int
        if (blockSize !in MIN_BLOCK_SIZE..MAX_BLOCK_SIZE) {
            throw AesFormatException("Tamaño de bloque fuera de rango: $blockSize.")
        }
        return EncHeader(salt, iterations, noncePrefix, blockSize)
    }

    fun blockCountAndLastLength(payloadSize: Long, blockSize: Int): Pair<Int, Int> {
        if (payloadSize < DIGEST_SIZE + TAG_SIZE) {
            throw AesFormatException("El fichero .enc no contiene ningún bloque completo.")
        }
        val stride = blockSize.toLong() + TAG_SIZE
        val adjusted = payloadSize - DIGEST_SIZE - TAG_SIZE
        val quotient = adjusted / stride
        val remainder = (adjusted % stride).toInt()
        if (remainder > blockSize) {
            throw AesFormatException("El tamaño de los bloques cifrados no es coherente.")
        }
        if (quotient + 1 > Int.MAX_VALUE) {
            throw AesFormatException("El fichero contiene demasiados bloques.")
        }
        return (quotient + 1).toInt() to remainder
    }

    fun nonce(prefix: ByteArray, counter: Int): ByteArray {
        val nonce = ByteArray(12)
        System.arraycopy(prefix, 0, nonce, 0, 8)
        ByteBuffer.wrap(nonce, 8, 4).order(ByteOrder.BIG_ENDIAN).putInt(counter)
        return nonce
    }

    fun aad(header: ByteArray, isLast: Boolean): ByteArray {
        val aad = ByteArray(header.size + 1)
        System.arraycopy(header, 0, aad, 0, header.size)
        aad[header.size] = if (isLast) 1 else 0
        return aad
    }
}
