package com.runnersz.reto2.android.data

import com.runnersz.reto2.android.domain.ErrorMensaje

/**
 * @author jairoben
 */
sealed class ApiResult<out T> {
    data class Success<out T>(
        val data: T,
        val estadoHttp: Int = 200,
    ) : ApiResult<T>()

    data class Failure(
        val error: ErrorMensaje,
        val estadoHttp: Int = 0,
        val cause: Throwable? = null,
    ) : ApiResult<Nothing>()
}
