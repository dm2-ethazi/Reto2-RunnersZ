package com.runnersz.reto2.android.core

import java.io.File
import java.io.IOException

class BattleOrderService {
    fun decryptFile(file: File, shift: Int): String {
        if (!file.isFile) {
            throw IOException("No existe el fichero de la orden.")
        }
        return Caesar.decrypt(file.readText(Charsets.UTF_8), shift)
    }

    fun decryptText(text: String, shift: Int): String = Caesar.decrypt(text, shift)
}
