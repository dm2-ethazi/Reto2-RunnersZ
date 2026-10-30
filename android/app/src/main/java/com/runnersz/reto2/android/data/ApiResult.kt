package com.runnersz.reto2.android.data

import com.runnersz.reto2.android.domain.ErrorDeLaApp

/**
 * Resultado tipado de una llamada a la API (§5.1).
 *
 * `Success(...)` cuando la petición terminó bien; `Failure(...)` con el `code` del JSON
 * `{code,message}` (que manda) o, sin JSON válido, con el mensaje genérico del estado HTTP.
 */
sealed class ApiResult<out T> {

    data class Success<out T>(val data: T, val estadoHttp: Int) : ApiResult<T>()

    data class Failure(val error: ErrorDeLaApp, val estadoHttp: Int) : ApiResult<Nothing>() {
        /** Código del servidor si venía en el JSON; `null` si fue un fallo genérico. */
        var code: String? = null
            internal set
    }

    val esExito: Boolean get() = this is Success

    /** Atajo para recorrer el resultado sin `when` completo. */
    fun <R> fold(ok: (T) -> R, mal: (Failure) -> R): R = when (this) {
        is Success -> ok(data)
        is Failure -> mal(this)
    }
}
