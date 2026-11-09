package com.runnersz.reto2.android.data

import com.runnersz.reto2.android.domain.ErrorMensaje

/**
 * Resultado de una llamada a la API: los datos de éxito o un error ya redactado.
 *
 * @author Jairo
 */
sealed class ApiResult<out T> {
        /**
     * Respuesta correcta con los datos convertidos y el código HTTP recibido.
     */
    data class Success<out T>(
        val data: T,
        val estadoHttp: Int = 200,
    ) : ApiResult<T>()

        /**
     * Fallo con el mensaje ya traducido, el estado HTTP y la causa original si la hubo.
     */
    data class Failure(
        val error: ErrorMensaje,
        val estadoHttp: Int = 0,
        val cause: Throwable? = null,
    ) : ApiResult<Nothing>()
}
