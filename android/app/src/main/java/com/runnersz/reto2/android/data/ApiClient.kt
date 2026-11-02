package com.runnersz.reto2.android.data

import android.os.Handler
import android.os.Looper
import com.runnersz.reto2.android.domain.ErrorDeLaApp
import com.runnersz.reto2.android.domain.ErrorMessages
import com.runnersz.reto2.android.domain.ShiftValidator
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
import org.json.JSONObject

/**
 * Cliente de la API RunnersZ (§5.1).
 *
 * Única librería de red: **OkHttp 4.12** (DJ4), con `callTimeout` de 120 s (§7.2) y
 * `connectTimeout` de 10 s, **sin reintentos automáticos** (reintenta la persona).
 *
 * Todas las llamadas envían `Authorization: Bearer <token>`. La contraseña **solo viaja en el
 * formulario**, nunca en URL ni cabeceras, y no se registra en ningún sitio (ni log ni
 * excepción). Los callbacks llegan ya en el hilo principal (salto con [Handler]).
 *
 * @param base dirección normalizada `http://host:puerto`.
 * @param token token Bearer.
 * @param cliente cliente HTTP, inyectable para las pruebas con `MockWebServer`.
 */
class ApiClient(
    private val base: String,
    private val token: String,
    private val cliente: OkHttpClient = clientePorDefecto(),
    private val principal: Handler = Handler(Looper.getMainLooper()),
) {

    private fun peticion(ruta: String): Request.Builder = Request.Builder()
        .url(base.trimEnd('/') + ruta)
        .header("Authorization", "Bearer $token")

    /** `GET /api/v1/health` (§3.3). */
    fun health(callback: (ApiResult<String>) -> Unit) {
        ejecutar(peticion("$API/health").get().build(), callback) { cuerpo, estado ->
            ApiResult.Success(String(cuerpo, Charsets.UTF_8), estado)
        }
    }

    /**
     * `GET /api/v1/auth/check` (§5.2, CU-17).
     *
     * **204** ⇒ `Success`; **401** ⇒ `Failure` con «Token no válido»; cualquier otro fallo
     * (500, sin respuesta, tiempo agotado) ⇒ `Failure` con su mensaje.
     */
    fun checkAuth(callback: (ApiResult<Unit>) -> Unit) {
        ejecutar(peticion("$API/auth/check").get().build(), callback) { _, estado ->
            ApiResult.Success(Unit, estado)
        }
    }

    /**
     * `POST /api/v1/files/decrypt` multipart (§3.3).
     *
     * @param fichero fichero temporal ya copiado a la caché privada (DJ5: sin `readBytes()`).
     * @param password clave AES; solo viaja en la parte `password` del formulario.
     */
    fun decryptFile(fichero: File, password: String, callback: (ApiResult<ByteArray>) -> Unit) {
        require(password.isNotEmpty()) { "Introduce la clave" }
        val cuerpo = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", fichero.name,
                fichero.asRequestBody(OCTET_STREAM.toMediaType()),
            )
            .addFormDataPart("password", password)
            .build()
        ejecutar(peticion("$API/files/decrypt").post(cuerpo).build(), callback) { respuesta, estado ->
            ApiResult.Success(respuesta, estado)
        }
    }

    /**
     * `POST /api/v1/text/caesar` con JSON `{text, shift, mode:"decrypt"}` (§6.3).
     *
     * El desplazamiento se valida en local **antes** de llamar ([ShiftValidator]).
     */
    fun caesarDecrypt(text: String, shift: Int, callback: (ApiResult<String>) -> Unit) {
        ShiftValidator.validar(shift)
        val json = JSONObject()
            .put("text", text)
            .put("shift", shift)
            .put("mode", "decrypt")
            .toString()
        ejecutar(
            peticion("$API/text/caesar").post(json.toRequestBody(JSON_MEDIA.toMediaType())).build(),
            callback,
        ) { respuesta, estado ->
            ApiResult.Success(JSONObject(String(respuesta, Charsets.UTF_8)).getString("result"), estado)
        }
    }

    /**
     * Lanza la llamada con `enqueue` (hilo propio de OkHttp) y entrega el resultado ya
     * traducido en el hilo principal. **Nunca propaga excepciones** (§7.2).
     */
    private fun <T> ejecutar(
        peticion: Request,
        callback: (ApiResult<T>) -> Unit,
        exito: (ByteArray, Int) -> ApiResult<T>,
    ) {
        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                entregar(callback, ApiResult.Failure(ErrorMessages.sinConexion(), 0))
            }

            override fun onResponse(call: Call, response: Response) {
                val resultado: ApiResult<T> = response.use { resp ->
                    try {
                        if (!resp.isSuccessful) {
                            ApiResult.Failure(errorDeRespuesta(resp), resp.code)
                        } else {
                            // DJ7: un 200 sin la cabecera no entrega el contenido.
                            if (peticion.url.encodedPath.endsWith("/files/decrypt") && resp.header("X-Integrity") != "verified") {
                                ApiResult.Failure(ErrorMessages.sinIntegridad(), resp.code)
                            } else {
                                exito(resp.body?.bytes() ?: ByteArray(0), resp.code)
                            }
                        }
                    } catch (e: Exception) {
                        ApiResult.Failure(ErrorMessages.sinConexion(), resp.code)
                    }
                }
                entregar(callback, resultado)
            }
        })
    }

    /** Traduce el JSON `{code,message}` de error (§7.2); sin JSON válido, el estado HTTP. */
    private fun errorDeRespuesta(respuesta: Response): ErrorDeLaApp {
        val cuerpo = try {
            respuesta.body?.string()
        } catch (e: Exception) {
            null
        }
        return ErrorMessages.porCodigo(parsearCode(cuerpo), respuesta.code)
    }

    private fun <T> entregar(callback: (ApiResult<T>) -> Unit, resultado: ApiResult<T>) {
        principal.post { callback(resultado) }
    }

    companion object {
        private const val API = "/api/v1"
        private const val JSON_MEDIA = "application/json; charset=utf-8"
        private const val OCTET_STREAM = "application/octet-stream"

        /** DJ4: `callTimeout` 120 s (§7.2), `connectTimeout` 10 s, sin reintentos. */
        fun clientePorDefecto(): OkHttpClient = OkHttpClient.Builder()
            .callTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()

        /** Extrae `code` del JSON de error; `null` si el cuerpo no es JSON válido. */
        fun parsearCode(cuerpo: String?): String? = try {
            if (cuerpo.isNullOrBlank()) null
            else JSONObject(cuerpo).optString("code").ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }
}
