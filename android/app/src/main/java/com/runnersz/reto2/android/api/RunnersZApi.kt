package com.runnersz.reto2.android.api

import com.runnersz.reto2.android.api.ApiConfig.ENDPOINT_AUTH_CHECK
import com.runnersz.reto2.android.api.ApiConfig.ENDPOINT_CAESAR
import com.runnersz.reto2.android.api.ApiConfig.ENDPOINT_FILE_DECRYPT
import com.runnersz.reto2.android.api.ApiConfig.ENDPOINT_HEALTH
import com.runnersz.reto2.android.api.ApiConfig.ENDPOINT_VIGENERE
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.GET

interface RunnersZApi {

    @GET(ENDPOINT_HEALTH)
    suspend fun health(): Response<kotlin.Unit>

    @GET(ENDPOINT_AUTH_CHECK)
    suspend fun checkToken(): Response<kotlin.Unit>

    @POST(ENDPOINT_CAESAR)
    suspend fun caesar(
        @Body body: CaesarRequest
    ): Response<CaesarResponse>

    @POST(ENDPOINT_VIGENERE)
    suspend fun vigenere(
        @Body body: TextRequest
    ): Response<TextResponse>

    @Multipart
    @POST(ENDPOINT_FILE_ENCRYPT)
    suspend fun encryptFile(
        @Part file: MultipartBody.Part,
        @Part password: RequestBody
    ): Response<FileResponse>

    @Multipart
    @POST(ENDPOINT_FILE_DECRYPT)
    suspend fun decryptFile(
        @Part file: MultipartBody.Part,
        @Part password: RequestBody
    ): Response<kotlin.Unit>
}

data class CaesarRequest(
    val text: String,
    val shift: Int,
    val mode: String = "decrypt"
)

data class CaesarResponse(
    val result: String? = null,
    val code: String? = null,
    val message: String? = null
)

data class TextRequest(
    val text: String,
    val key: String? = null,
    val shift: Int? = null,
    val mode: String = "encrypt"
)

data class TextResponse(
    val result: String? = null,
    val code: String? = null,
    val message: String? = null
)

data class FileResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val fileUrl: String? = null,
    val error: String? = null
)

// Modelo de error estandarizado del servidor (Contrato Técnico 3.3)
data class ApiError(
    val code: String? = null,
    val message: String? = null
)
