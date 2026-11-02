package com.runnersz.reto2.android.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runnersz.reto2.android.api.ApiClient
import com.runnersz.reto2.android.api.RunnersZApi
import com.runnersz.reto2.android.api.TokenManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.util.Patterns

class AjustesViewModel : ViewModel() {

    private val api: RunnersZApi = ApiClient.api

    val direccionServidor = MutableLiveData<String>("")
    val tokenBearer = MutableLiveData<String>("")
    val esConfigurada = MutableLiveData<Boolean>(false)
    val mensajeEstado = MutableLiveData<String>("Configuración no guardada / Config not saved")
    val esError = MutableLiveData<Boolean>(false)
    val esProcesandoConexion = MutableLiveData<Boolean>(false)

    init {
        cargarConfiguracion()
    }

    private fun cargarConfiguracion() {
        direccionServidor.value = TokenManager.getServerUrl()
        tokenBearer.value = TokenManager.getToken()
        esConfigurada.value = TokenManager.isConfigured()
        mensajeEstado.value = if (esConfigurada.value == true) {
            "Configuración guardada / Config saved"
        } else {
            "Configuración no guardada / Config not saved"
        }
    }

    fun guardarConfiguracion(direccion: String, token: String, context: Context) {
        if (direccion.isEmpty()) {
            mensajeEstado.value = "Ingresá la dirección del servidor"
            esError.value = true
            return
        }

        if (!esUrlValida(direccion)) {
            mensajeEstado.value = "URL inválida. Formato: IP:puerto o https://host"
            esError.value = true
            return
        }

        if (token.isEmpty()) {
            mensajeEstado.value = "Ingresá el token Bearer"
            esError.value = true
            return
        }

        val serverUrl = if (!direccion.endsWith("/")) direccion + "/" else direccion

        val exito = TokenManager.saveConfig(token, serverUrl)
        if (exito) {
            TokenManager.applyConfigToApi()
            direccionServidor.value = serverUrl
            tokenBearer.value = token
            esConfigurada.value = true
            mensajeEstado.value = "Configuración guardada / Config saved"
            esError.value = false
        } else {
            mensajeEstado.value = "Error guardando configuración"
            esError.value = true
        }
    }

    fun probarConexion(context: Context) {
        esProcesandoConexion.value = true
        mensajeEstado.value = "Probando conexión / Testing connection..."
        esError.value = false

        viewModelScope.launch {
            try {
                TokenManager.applyConfigToApi()
                val response = api.checkToken()

                withContext(Dispatchers.Main) {
                    esProcesandoConexion.value = false
                    when (response.code()) {
                        200, 204 -> {
                            mensajeEstado.value = "API responde correctamente / API OK"
                            esError.value = false
                        }
                        401 -> {
                            mensajeEstado.value = "Token inválido / Invalid token"
                            esError.value = true
                        }
                        else -> {
                            mensajeEstado.value = "La API no responde / API error"
                            esError.value = true
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    esProcesandoConexion.value = false
                    mensajeEstado.value = "Error: ${e.message}"
                    esError.value = true
                    e.printStackTrace()
                }
            }
        }
    }

    private fun esUrlValida(url: String): Boolean {
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            Patterns.WEB_URL.matcher(url).matches()
        } else {
            url.contains(":") && !url.contains(" ") && url.length > 3
        }
    }

    fun limpiar() {
        direccionServidor.value = ""
        tokenBearer.value = ""
        esConfigurada.value = false
        mensajeEstado.value = "Configuración no guardada / Config not saved"
        esError.value = false
    }
}
