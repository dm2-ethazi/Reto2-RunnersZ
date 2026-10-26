package com.runnersz.reto2.android.core

import java.io.File
import java.io.IOException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AesInteropTest {
    @get:Rule val temporary = TemporaryFolder()
    private val decryptor = AesFileDecryptor()
    private val password = "RunnersZ-ñ-🔐"

    private fun resource(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/vectors/$name")).use { it.readBytes() }

    private fun encrypted(name: String): File = temporary.newFile().apply {
        writeBytes(resource("$name.enc"))
    }

    private fun destination(): File = File(temporary.root, java.util.UUID.randomUUID().toString())

    @Test fun decryptsBackendVectorsAtEveryBoundary() {
        for (name in listOf("empty", "one", "boundary", "multi", "image")) {
            val input = encrypted(name)
            val output = destination()
            decryptor.decrypt(input, output, password)
            assertArrayEquals(name, resource("$name.bin"), output.readBytes())
            assertFalse(File(output.path + ".part").exists())
            assertArrayEquals(resource("$name.enc"), input.readBytes())
        }
    }

    @Test fun rejectsWrongPasswordAndCorruptionWithoutPublishing() {
        val original = resource("one.enc")
        val cases = listOf(
            original to "incorrecta",
            original.copyOf().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() } to password,
            original.copyOf(original.size - 1) to password,
            original.copyOf(12) to password,
            original + byteArrayOf(0) to password,
        )
        for ((bytes, key) in cases) {
            val input = temporary.newFile().apply { writeBytes(bytes) }
            val output = destination()
            assertThrows(AesDecryptException::class.java) { decryptor.decrypt(input, output, key) }
            assertFalse(output.exists())
            assertFalse(File(output.path + ".part").exists())
            assertArrayEquals(bytes, input.readBytes())
        }
    }

    @Test fun verifiesDigestEvenWhenGcmTagIsValid() {
        val input = encrypted("bad-digest")
        val output = destination()
        assertThrows(AesIntegrityException::class.java) { decryptor.decrypt(input, output, password) }
        assertFalse(output.exists())
        assertFalse(File(output.path + ".part").exists())
    }

    @Test fun rejectsEmptyPasswordMissingAndSameInput() {
        val input = encrypted("one")
        assertThrows(EmptyAesPasswordException::class.java) { decryptor.decrypt(input, destination(), "") }
        assertThrows(IOException::class.java) { decryptor.decrypt(destination(), destination(), password) }
        assertThrows(IOException::class.java) { decryptor.decrypt(input, input, password) }
        val alias = File(input.parentFile, "./${input.name}")
        assertThrows(IOException::class.java) { decryptor.decrypt(input, alias, password) }
    }

    @Test fun neverOverwritesExistingDestinationOrTemporaryFile() {
        val input = encrypted("one")
        val output = temporary.newFile().apply { writeText("keep") }
        assertThrows(IOException::class.java) { decryptor.decrypt(input, output, password) }
        assertEquals("keep", output.readText())
        val absent = destination()
        val part = File(absent.path + ".part").apply { writeText("other operation") }
        assertThrows(IOException::class.java) { decryptor.decrypt(input, absent, password) }
        assertEquals("other operation", part.readText())
        assertFalse(absent.exists())
    }
}