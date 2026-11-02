package com.runnersz.reto2.android.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runnersz.reto2.android.api.ApiClient
import com.runnersz.reto2.android.api.ApiError
import com.runnersz.reto2.android.api.RunnersZApi
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ImagenViewModel : ViewModel() {

    private val api: RunnersZApi = ApiClient.api
    private val gson: Gson = Gson()

    val archivoSeleccionado = MutableLiveData<Uri?>(null)
    val nombreArchivo = MutableLiveData<String>("")
    val bitmapResultado = MutableLiveData<Bitmap?>(null)
    val esProcesando = MutableLiveData<Boolean>(false)
    val mensajeEstado = MutableLiveData<String>("Listo / Ready")
    val esError = MutableLiveData<Boolean>(false)
    val integridadVerificada = MutableLiveData<Boolean>(false)

    fun seleccionarArchivo(uri: Uri, context: android.content.Context) {
        archivoSeleccionado.value = uri
        nombreArchivo.value = uri.lastPathSegment ?: uri.toString()
        bitmapResultado.value = null
        mensajeEstado.value = "Listo / Ready"
        esError.value = false
        integridadVerificada.value = false

        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    bitmapResultado.value = bitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun descifrarImagen(uri: Uri, password: String, context: android.content.Context) {
        archivoSeleccionado.value = uri
        nombreArchivo.value = uri.lastPathSegment ?: uri.toString()
        esProcesando.value = true
        mensajeEstado.value = "Procesando / Processing..."
        esError.value = false
        integridadVerificada.value = false

        viewModelScope.launch {
            try {
                val file = uriToFile(context, uri)
                if (file == null) {
                    withContext(Dispatchers.Main) {
                        esProcesando.value = false
                        mensajeEstado.value = "No se pudo leer el archivo"
                        esError.value = true
                    }
                    return@launch
                }

                val requestFile = RequestBody.create(
                    "application/octet-stream".toMediaType(),
                    file
                )
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val passwordBody = RequestBody.create(
                    "text/plain".toMediaType(),
                    password
                )

                val response: retrofit2.Response<kotlin.Unit> = api.decryptFile(body, passwordBody)

                withContext(Dispatchers.Main) {
                    esProcesando.value = false
                    if (response.isSuccessful) {
                        val integrityHeader = response.headers().get("X-Integrity")
                        integridadVerificada.value = "verified".equals(integrityHeader, ignoreCase = true)

                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            bitmapResultado.value = bitmap
                        }

                        mensajeEstado.value = if (integridadVerificada.value == true) {
                            "Integridad verificada / Integrity verified"
                        } else {
                            "Integridad fallida / Integrity failed"
                        }
                        esError.value = false

                        borrarArchivoTemporal(file)
                    } else {
                        manejarErrorApi(response)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    esProcesando.value = false
                    mensajeEstado.value = "${e.message}"
                    esError.value = true
                    e.printStackTrace()
                }
            }
        }
    }

    private fun manejarErrorApi(response: retrofit2.Response<*>) {
        val errorMessage = when (response.code()) {
            401 -> {
                val apiError = parseErrorBody(response)
                "Token no válido: ${apiError?.message ?: ""}"
            }
            400 -> {
                val apiError = parseErrorBody(response)
                when (apiError?.code) {
                    "EMPTY_FILE" -> "El archivo está vacío"
                    "EMPTY_PASSWORD" -> "Contraseña vacía"
                    else -> apiError?.message ?: "Error en la solicitud"
                }
            }
            413 -> "Archivo demasiado grande"
            422 -> {
                val apiError = parseErrorBody(response)
                when (apiError?.code) {
                    "DECRYPT_FAILED" -> "Clave incorrecta o fichero dañado"
                    else -> apiError?.message ?: "Error de descifrado"
                }
            }
            else -> "Error del servidor (HTTP ${response.code()})"
        }
        mensajeEstado.value = errorMessage
        esError.value = true
    }

    private fun parseErrorBody(response: retrofit2.Response<*>): ApiError? {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                gson.fromJson(errorBody, ApiError::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uriToFile(context: android.content.Context, uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, System.currentTimeMillis().toString() + "_file")
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun borrarArchivoTemporal(file: File) {
        try {
            if (file.exists()) {
                val length = file.length()
                val fos = FileOutputStream(file)
                fos.write(ByteArray(length.toInt()))
                fos.close()
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            file.delete()
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun limpiar() {
        archivoSeleccionado.value = null
        nombreArchivo.value = ""
        bitmapResultado.value = null
        mensajeEstado.value = "Listo / Ready"
        esError.value = false
        integridadVerificada.value = false
        esProcesando.value = false
    }
}
