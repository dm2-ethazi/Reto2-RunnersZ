package com.runnersz.reto2.android.domain

import com.runnersz.reto2.android.data.ApiConfig

/**
 * Motivo por el que un archivo no se puede enviar al servidor.
 *
 * @author jairoben
 */
enum class ProblemaArchivo { VACIO, DEMASIADO_GRANDE, EXTENSION }

/**
 * Comprueba en el dispositivo lo básico antes de gastar una subida: extensión `.enc`,
 * contenido y tamaño. Evita enviar al servidor un archivo que nunca podrá descifrarse
 * y muestra un mensaje que el usuario puede entender.
 *
 * @author jairoben
 */
object FileValidator {

    const val EXTENSION = ".enc"

    fun problema(nombre: String, bytes: Long): ProblemaArchivo? = when {
        bytes <= 0L -> ProblemaArchivo.VACIO
        bytes > ApiConfig.MAX_UPLOAD_BYTES -> ProblemaArchivo.DEMASIADO_GRANDE
        !nombre.endsWith(EXTENSION, ignoreCase = true) -> ProblemaArchivo.EXTENSION
        else -> null
    }
}
