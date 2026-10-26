package com.runnersz.reto2.android.core

import java.io.File
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun caesarRoundTripsAndValidates() {
        assertEquals("Bc aA Ñá 19!", Caesar.encrypt("Ab zZ Ñá 19!", 1))
        for (shift in 1..25) {
            assertEquals("Ab zZ Ñá 19!", Caesar.decrypt(Caesar.encrypt("Ab zZ Ñá 19!", shift), shift))
        }
        assertEquals("", Caesar.decrypt("", 1))
        for (shift in listOf(-1, 0, 26, Int.MAX_VALUE)) {
            assertThrows(InvalidShiftException::class.java) { Caesar.decrypt("A", shift) }
        }
    }

    @Test fun ordersReadUtf8AndReportFailures() {
        val service = BattleOrderService()
        val input = temporary.newFile("order.txt")
        input.writeText("Dwdtxh Ñá!", Charsets.UTF_8)
        assertEquals("Ataque Ñá!", service.decryptFile(input, 3))
        assertEquals("Az", service.decryptText("Ba", 1))
        assertThrows(InvalidShiftException::class.java) { service.decryptFile(input, 0) }
        assertThrows(java.io.IOException::class.java) {
            service.decryptFile(File(temporary.root, "missing"), 1)
        }
    }

    @Test fun detectsSharedTypesAndMagic() {
        for (name in listOf("ORDEN.TXT", "a.txt.enc")) {
            assertEquals(SharedFileKind.ORDER, SharedFileKinds.detect(name, null))
        }
        for (name in listOf("A.PNG.ENC", "A.PNG", "a.enc")) {
            assertEquals(SharedFileKind.IMAGE, SharedFileKinds.detect(name, null))
        }
        assertEquals(SharedFileKind.ORDER, SharedFileKinds.detect(null, "text/plain"))
        for (mime in listOf("image/png", "image/jpeg")) {
            assertEquals(SharedFileKind.IMAGE, SharedFileKinds.detect(null, mime))
        }
        assertEquals(SharedFileKind.UNKNOWN, SharedFileKinds.detect(null, null))
        assertTrue(SharedFileKinds.isEncryptedName("A.ENC"))
        assertFalse(SharedFileKinds.isEncryptedName(null))
        assertFalse(SharedFileKinds.hasEncMagic(temporary.root))
        val input = temporary.newFile()
        assertFalse(SharedFileKinds.hasEncMagic(input))
        input.writeBytes(byteArrayOf(1, 2, 3, 4))
        assertFalse(SharedFileKinds.hasEncMagic(input))
        input.writeBytes(EncFormat.MAGIC)
        assertTrue(SharedFileKinds.hasEncMagic(input))
    }

    private fun header(): ByteArray = ByteBuffer.allocate(37)
        .put(EncFormat.MAGIC).put(1.toByte()).put(ByteArray(16))
        .putInt(600000).put(ByteArray(8)).putInt(1048576).array()

    @Test fun validatesHeaderAndBlockLayout() {
        val raw = header()
        val parsed = EncFormat.parse(raw)
        assertEquals(600000, parsed.iterations)
        assertEquals(1048576, parsed.blockSize)
        assertArrayEquals(ByteArray(16), parsed.salt)
        assertArrayEquals(ByteArray(8), parsed.noncePrefix)
        assertEquals(1 to 0, EncFormat.blockCountAndLastLength(48, 1048576))
        assertEquals(2 to 1, EncFormat.blockCountAndLastLength(1048641, 1048576))
        assertThrows(AesFormatException::class.java) { EncFormat.parse(ByteArray(0)) }
        for (offset in listOf(0, 4)) {
            val invalid = raw.copyOf(); invalid[offset] = 0
            assertThrows(AesFormatException::class.java) { EncFormat.parse(invalid) }
        }
        for (value in listOf(0, 99999, 2000001)) {
            val invalid = raw.copyOf(); ByteBuffer.wrap(invalid).putInt(21, value)
            assertThrows(AesFormatException::class.java) { EncFormat.parse(invalid) }
        }
        for (value in listOf(0, 65535, 16777217)) {
            val invalid = raw.copyOf(); ByteBuffer.wrap(invalid).putInt(33, value)
            assertThrows(AesFormatException::class.java) { EncFormat.parse(invalid) }
        }
        for (size in listOf(0L, 47L, 1048625L, Long.MAX_VALUE)) {
            assertThrows(AesFormatException::class.java) {
                EncFormat.blockCountAndLastLength(size, 1048576)
            }
        }
        assertArrayEquals(byteArrayOf(1,2,3,4,5,6,7,8,16,32,48,64),
            EncFormat.nonce(byteArrayOf(1,2,3,4,5,6,7,8), 0x10203040))
        assertArrayEquals(raw + byteArrayOf(1), EncFormat.aad(raw, true))
        assertArrayEquals(raw + byteArrayOf(0), EncFormat.aad(raw, false))
    }
}