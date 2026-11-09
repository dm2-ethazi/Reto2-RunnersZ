package com.runnersz.reto2.android.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Calcula el factor de muestreo necesario para que una imagen no supere el lado máximo.
 *
 * @author Jairo
 */
object ImageSampler {

    const val MAX_SIDE = 2048

/**
 * Devuelve la potencia de dos que deja el lado mayor dentro de `maxSide`.
 *
 *  * @param width ancho original
 *  * @param height alto original
 *  * @param maxSide lado máximo permitido
 *  * @return factor para [BitmapFactory.Options.inSampleSize]
 */
    fun sampleSizeFor(width: Int, height: Int, maxSide: Int = MAX_SIDE): Int {
        if (width <= 0 || height <= 0 || maxSide <= 0) return 1
        var sample = 1
        while (maxOf(width, height) / sample > maxSide) {
            sample *= 2
        }
        return sample
    }
}

/**
 * Decodifica los bytes descifrados reduciendo la resolución cuando hace falta, para
 * que una foto grande no bloquee la interfaz ni provoque un `OutOfMemoryError`.
 *
 * @author Jairo
 */
object ImageDecoder {

/**
 * Decodifica los bytes con muestreo previo para no agotar la memoria.
 *
 *  * @param bytes contenido ya descifrado
 *  * @return el [Bitmap] listo para mostrar, o `null` si no es una imagen válida
 */
    fun decode(bytes: ByteArray): Bitmap? {
        if (bytes.isEmpty()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageSampler.sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}
