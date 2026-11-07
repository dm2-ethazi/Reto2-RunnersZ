package com.runnersz.reto2.android.data

import android.os.Handler
import android.os.Looper
import com.runnersz.reto2.android.domain.ErrorMessages
import com.runnersz.reto2.android.domain.ShiftValidator
import com.runnersz.reto2.android.security.SecureTokenProvider
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject

/**
 * Cliente HTTP de CipherSprint.
 *
 * El token se reconstruye en cada petición y solo vive en la cabecera Authorization.
 * No se persiste, no se muestra y no se incluye en mensajes de error ni logs.
  * @author jairoben
*/
class ApiClient(
    private val base: String = ApiConfig.BASE_URL,
    private val tokenProvider: () -> String = { SecureTokenProvider.getToken() },
    private val client: OkHttpClient = defaultClient(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) {

    fun checkAuth(callback: (ApiResult<Unit>) -> Unit) {
        execute(request("/auth/check").get().build(), callback) { _, code ->
            ApiResult.Success(Unit, code)
        }
    }

    fun caesarDecrypt(text: String, shift: Int, callback: (ApiResult<String>) -> Unit) {
        if (!ShiftValidator.esValido(shift)) {
            deliver(callback, ApiResult.Failure(com.runnersz.reto2.android.domain.ErrorMensaje(ShiftValidator.MENSAJE)))
            return
        }
        val json = JSONObject()
            .put("text", text)
            .put("shift", shift)
            .put("mode", "decrypt")
            .toString()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        execute(request("/text/caesar").post(body).build(), callback) { bytes, code ->
            val result = JSONObject(String(bytes, Charsets.UTF_8)).getString("result")
            ApiResult.Success(result, code)
        }
    }

    fun decryptFile(file: File, password: String, callback: (ApiResult<ByteArray>) -> Unit) {
        when {
            password.isEmpty() -> {
                deliver(callback, ApiResult.Failure(com.runnersz.reto2.android.domain.ErrorMensaje("Introduce la clave AES")))
                return
            }
            !file.exists() || !file.isFile -> {
                deliver(callback, ApiResult.Failure(com.runnersz.reto2.android.domain.ErrorMensaje("No se pudo leer el archivo")))
                return
            }
            file.length() > ApiConfig.MAX_UPLOAD_BYTES -> {
                deliver(callback, ApiResult.Failure(com.runnersz.reto2.android.domain.ErrorMensaje("El archivo supera el límite de 100 MiB"), 413))
                return
            }
        }

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaType()),
            )
            .addFormDataPart("password", password)
            .build()

        execute(request("/files/decrypt").post(multipart).build(), callback) { bytes, code ->
            ApiResult.Success(bytes, code)
        }
    }

    private fun request(path: String): Request.Builder =
        Request.Builder()
            .url(base.trimEnd('/') + "/api/v1" + path)
            .header("Authorization", "Bearer " + tokenProvider())
            .header("Accept", "application/json")

    private fun <T> execute(
        request: Request,
        callback: (ApiResult<T>) -> Unit,
        onSuccess: (ByteArray, Int) -> ApiResult<T>,
    ) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                deliver(callback, ApiResult.Failure(ErrorMessages.sinConexion(), 0, e))
            }

            override fun onResponse(call: Call, response: Response) {
                val result: ApiResult<T> = try {
                    response.use { resp ->
                        if (!resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            ApiResult.Failure(
                                ErrorMessages.porCodigo(parseCode(body), resp.code),
                                resp.code,
                            )
                        } else if (
                            request.url.encodedPath.endsWith("/files/decrypt") &&
                            !resp.header("X-Integrity").equals("verified", ignoreCase = true)
                        ) {
                            ApiResult.Failure(ErrorMessages.sinIntegridad(), resp.code)
                        } else {
                            onSuccess(resp.body?.bytes() ?: ByteArray(0), resp.code)
                        }
                    }
                } catch (e: JSONException) {
                    ApiResult.Failure(ErrorMessages.respuestaInvalida(), 0, e)
                } catch (e: Exception) {
                    ApiResult.Failure(ErrorMessages.sinConexion(), 0, e)
                }
                deliver(callback, result)
            }
        })
    }

    private fun <T> deliver(callback: (ApiResult<T>) -> Unit, result: ApiResult<T>) {
        mainHandler.post { callback(result) }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .callTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()

        fun parseCode(body: String?): String? = try {
            if (body.isNullOrBlank()) null
            else JSONObject(body).optString("code").ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }
}
