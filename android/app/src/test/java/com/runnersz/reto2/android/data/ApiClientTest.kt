package com.runnersz.reto2.android.data

import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
/**
 * @author jairoben
 */
class ApiClientTest {

    private lateinit var server: MockWebServer

    private val http = OkHttpClient.Builder()
        .callTimeout(2, TimeUnit.SECONDS)
        .connectTimeout(1, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun api(token: String = "TOKEN-DE-PRUEBA"): ApiClient = ApiClient(
        base = server.url("/").toString().trimEnd('/'),
        tokenProvider = { token },
        client = http,
        mainHandler = Handler(Looper.getMainLooper()),
    )

    private fun <T> await(block: ((ApiResult<T>) -> Unit) -> Unit): ApiResult<T> {
        var result: ApiResult<T>? = null
        val latch = CountDownLatch(1)
        block { value ->
            result = value
            latch.countDown()
        }

        shadowOf(Looper.getMainLooper()).idle()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (latch.count > 0 && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("El callback no llegó", 0L, latch.count)
        return requireNotNull(result)
    }

    @Test
    fun `checkAuth envia Bearer y la ruta v1`() {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = await<Unit> { callback -> api().checkAuth(callback) }

        assertTrue(result is ApiResult.Success)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/check", request.path)
        assertEquals("Bearer TOKEN-DE-PRUEBA", request.getHeader("Authorization"))
    }

    @Test
    fun `caesar envia json de descifrado y lee result`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"result":"Ataque"}"""),
        )

        val result = await<String> { callback ->
            api().caesarDecrypt("Dwdtxh", 3, callback)
        }

        assertTrue(result is ApiResult.Success)
        assertEquals("Ataque", (result as ApiResult.Success).data)
        val request = server.takeRequest()
        assertEquals("/api/v1/text/caesar", request.path)
        assertTrue(request.body.readUtf8().contains("\"mode\":\"decrypt\""))
    }

    @Test
    fun `decryptFile rechaza respuesta sin X-Integrity`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("PNG"))
        val file = File.createTempFile("vector", ".enc").apply { writeText("datos") }

        try {
            val result = await<ByteArray> { callback ->
                api().decryptFile(file, "clave", callback)
            }

            assertTrue(result is ApiResult.Failure)
            assertEquals(
                "El servidor no confirmó la integridad del archivo",
                (result as ApiResult.Failure).error.texto,
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun `decryptFile acepta X-Integrity verified`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("X-Integrity", "verified")
                .setBody("PNG"),
        )
        val file = File.createTempFile("vector", ".enc").apply { writeText("datos") }

        try {
            val result = await<ByteArray> { callback ->
                api().decryptFile(file, "clave", callback)
            }

            assertTrue(result is ApiResult.Success)
            assertArrayEquals("PNG".toByteArray(), (result as ApiResult.Success).data)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `401 se traduce a error de autenticacion`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"code":"UNAUTHORIZED","message":"UNAUTHORIZED"}"""),
        )

        val result = await<Unit> { callback -> api().checkAuth(callback) }

        assertTrue(result is ApiResult.Failure)
        assertEquals(
            "No se pudo autenticar con el servidor",
            (result as ApiResult.Failure).error.texto,
        )
    }
}
