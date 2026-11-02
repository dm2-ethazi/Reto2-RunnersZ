package com.runnersz.reto2.android.api

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object TokenManager {

    private const val PREFS_NAME = "cipher_sprint_secure_prefs"
    private const val KEY_TOKEN = "api_bearer_token"
    private const val KEY_SERVER_URL = "api_server_url"
    private const val KEY_CONFIGURED = "api_configured"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val encPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs = encPrefs
            ApiConfig.bearerToken = getToken()
            ApiConfig.baseUrl = getServerUrl()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveConfig(token: String, serverUrl: String): Boolean {
        return try {
            prefs?.edit()?.putString(KEY_TOKEN, token)?.putString(KEY_SERVER_URL, serverUrl)?.putBoolean(KEY_CONFIGURED, true)?.apply()
            ApiConfig.bearerToken = token
            ApiConfig.baseUrl = serverUrl
            ApiClient.rebuildWithBaseUrl(serverUrl)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getToken(): String {
        return prefs?.getString(KEY_TOKEN, "") ?: ""
    }

    fun getServerUrl(): String {
        return prefs?.getString(KEY_SERVER_URL, "https://runnersz-api.onrender.com/") ?: "https://runnersz-api.onrender.com/"
    }

    fun isConfigured(): Boolean {
        return prefs?.getBoolean(KEY_CONFIGURED, false) ?: false
    }

    fun clearConfig(): Boolean {
        return try {
            prefs?.edit()?.remove(KEY_TOKEN)?.remove(KEY_SERVER_URL)?.putBoolean(KEY_CONFIGURED, false)?.apply()
            ApiConfig.bearerToken = ""
            ApiConfig.baseUrl = "https://runnersz-api.onrender.com/"
            true
        } catch (e: Exception) {
            false
        }
    }

    fun applyConfigToApi() {
        ApiConfig.bearerToken = getToken()
        ApiConfig.baseUrl = getServerUrl()
        ApiClient.rebuildWithBaseUrl(ApiConfig.baseUrl)
    }
}
