package com.runnersz.reto2.android.core

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Locale

enum class SharedFileKind {
    IMAGE,
    ORDER,
    UNKNOWN,
}

object SharedFileKinds {
    fun detect(name: String?, mimeType: String?): SharedFileKind {
        val lowerName = name?.lowercase(Locale.ROOT).orEmpty()
        val lowerMime = mimeType?.lowercase(Locale.ROOT).orEmpty()
        if (lowerName.endsWith(".txt") || lowerName.endsWith(".txt.enc")) {
            return SharedFileKind.ORDER
        }
        if (lowerName.endsWith(".png") || lowerName.endsWith(".png.enc")) {
            return SharedFileKind.IMAGE
        }
        if (lowerMime == "text/plain") {
            return SharedFileKind.ORDER
        }
        if (lowerMime == "image/png" || lowerMime.startsWith("image/")) {
            return SharedFileKind.IMAGE
        }
        if (lowerName.endsWith(".enc")) {
            return SharedFileKind.IMAGE
        }
        return SharedFileKind.UNKNOWN
    }

    fun isEncryptedName(name: String?): Boolean =
        name?.lowercase(Locale.ROOT)?.endsWith(".enc") == true

    fun hasEncMagic(file: File): Boolean {
        if (!file.isFile || file.length() < 4) {
            return false
        }
        return try {
            FileInputStream(file).use { input ->
                val magic = ByteArray(4)
                var offset = 0
                while (offset < magic.size) {
                    val read = input.read(magic, offset, magic.size - offset)
                    if (read < 0) {
                        return false
                    }
                    offset += read
                }
                magic.contentEquals(EncFormat.MAGIC)
            }
        } catch (exception: IOException) {
            false
        }
    }
}
