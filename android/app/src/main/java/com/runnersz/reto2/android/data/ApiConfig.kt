package com.runnersz.reto2.android.data

/**
 * Configuración inmutable del cliente. La dirección no se puede cambiar desde la app:
 * así el token solo se envía al servidor oficial por HTTPS.
  * @author Jairo
*/
object ApiConfig {
    const val BASE_URL = "https://runnersz-api.onrender.com"
    const val MAX_UPLOAD_BYTES = 100L * 1024L * 1024L
}
