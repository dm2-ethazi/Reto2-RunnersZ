package com.runnersz.reto2.android.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import java.security.MessageDigest
import java.security.cert.X509Certificate

object ApiClient {

    // SHA-256 fingerprint del certificado SSL de runnersz-api.onrender.com
    private const val API_SHA256_FINGERPRINT = "A88CF8E12B2E731A485D320C92B48EA71E0D6BF8A83868B5D110DA51D4A1A45C"

    private const val DEBUG_BUILD = true

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (DEBUG_BUILD) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header(ApiConfig.HEADER_AUTHORIZATION, "Bearer ${ApiConfig.bearerToken}")
                .header(ApiConfig.HEADER_CONTENT_TYPE, "application/json")
                .header(ApiConfig.HEADER_ACCEPT, "application/json")
            chain.proceed(builder.build())
        }
        .connectTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .sslSocketFactory(createSslSocketFactory(), createTrustManager())
        .hostnameVerifier { hostname, session ->
            val peerCertificates = session.peerCertificates
            if (peerCertificates.isNotEmpty()) {
                val cert = peerCertificates[0] as X509Certificate
                val fingerprint = sha256Fingerprint(cert.encoded)
                if (DEBUG_BUILD) {
                    true
                } else {
                    fingerprint.equals(API_SHA256_FINGERPRINT, ignoreCase = true)
                }
            } else {
                false
            }
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api: RunnersZApi = retrofit.create(RunnersZApi::class.java)

    fun rebuildWithBaseUrl(baseUrl: String) {
        ApiConfig.baseUrl = if (baseUrl.isNotBlank()) baseUrl.trimEnd { it == '/' } + "/" else "https://runnersz-api.onrender.com/"
    }

    private fun createSslSocketFactory(): javax.net.ssl.SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(createTrustManager()), java.security.SecureRandom())
        return sslContext.socketFactory
    }

    private fun createTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
    }

    private fun sha256Fingerprint(der: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(der)
        return digest.joinToString("") { "%02X".format(it) }
    }
}
