package com.runnersz.reto2.android.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runnersz.reto2.android.api.ApiClient
import com.runnersz.reto2.android.api.ApiError
import com.runnersz.reto2.android.api.CaesarRequest
import com.runnersz.reto2.android.api.CaesarResponse
import com.runnersz.reto2.android.api.RunnersZApi
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class OrdenViewModel : ViewModel() {

    private val api: RunnersZApi = ApiClient.api
    private val gson: Gson = Gson()

    val textoCifrado = MutableLiveData<String>("")
    val resultadoDescifrado = MutableLiveData<String>("")
    val esProcesando = MutableLiveData<Boolean>(false)
    val mensajeError = MutableLiveData<String?>(null)
    val esError = MutableLiveData<Boolean>(false)

    fun cargarTexto(texto: String) {
        textoCifrado.value = texto
    }

    fun descifrarOrden(texto: String, shift: Int) {
        textoCifrado.value = texto
        esProcesando.value = true
        mensajeError.value = null
        esError.value = false

        viewModelScope.launch {
            try {
                val response: retrofit2.Response<CaesarResponse> = api.caesar(
                    CaesarRequest(text = texto, shift = shift, mode = "decrypt")
                )

                withContext(Dispatchers.Main) {
                    esProcesando.value = false
                    if (response.isSuccessful) {
                        resultadoDescifrado.value = response.body()?.result ?: "Sin resultado"
                        mensajeError.value = null
                        esError.value = false
                    } else {
                        manejarErrorApi(response)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    esProcesando.value = false
                    mensajeError.value = "${e.message}"
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
                "Token no válido. Revisá los ajustes: ${apiError?.message ?: ""}"
            }
            400 -> {
                val apiError = parseErrorBody(response)
                when (apiError?.code) {
                    "INVALID_SHIFT" -> "El desplazamiento debe ser del 1 al 25"
                    "EMPTY_PASSWORD" -> "Contraseña vacía"
                    else -> apiError?.message ?: "Error en la solicitud"
                }
            }
            422 -> {
                val apiError = parseErrorBody(response)
                apiError?.message ?: "Error de procesamiento"
            }
            else -> "Error del servidor (HTTP ${response.code()})"
        }
        mensajeError.value = errorMessage
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

    fun limpiar() {
        textoCifrado.value = ""
        resultadoDescifrado.value = ""
        mensajeError.value = null
        esError.value = false
        esProcesando.value = false
    }
}
