package com.runnersz.reto2.android.data

import android.os.Handler
import android.os.Looper
import com.runnersz.reto2.android.domain.AccionError
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Pruebas de `ApiClient.checkAuth` contra **MockWebServer** (§5.2 y DJ10):
 * 204, 401, 500, conexión rechazada y tiempo agotado.
 *
 * Los callbacks llegan al hilo principal; con Robolectric se vacía ese hilo con
 * `shadowOf(Looper.getMainLooper()).idle()` antes de asertar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApiClientCheckAuthTest {

    private lateinit var servidor: MockWebServer

    // callTimeout corto para que el caso de tiempo agotado termine rápido en pruebas.
    private val clienteDePrueba = OkHttpClient.Builder()
        .callTimeout(2, TimeUnit.SECONDS)
        .connectTimeout(1, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
    }

    @After
    fun cerrar() {
        servidor.shutdown()
    }

    private fun cliente(): ApiClient = ApiClient(
        base = servidor.url("/").toString().trimEnd('/'),
        token = "TOKEN-DE-PRUEBA",
        cliente = clienteDePrueba,
        principal = Handler(Looper.getMainLooper()),
    )

    /** Ejecuta `checkAuth` y devuelve el resultado entregado en el hilo principal. */
    private fun checkAuthEsperando(): ApiResult<Unit> {
        var obtenido: ApiResult<Unit>? = null
        val espera = CountDownLatch(1)
        cliente().checkAuth { resultado ->
            obtenido = resultado
            espera.countDown()
        }
        // Se vacía el hilo principal para que el callback postulado se ejecute.
        shadowOf(Looper.getMainLooper()).idle()
        val limite=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
        while(espera.count>0 && System.nanoTime()<limite){shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10)}
        assertEquals("El callback no llegó",0L,espera.count)
        shadowOf(Looper.getMainLooper()).idle()
        return requireNotNull(obtenido)
    }

    @Test
    fun `204 es exito`() {
        servidor.enqueue(MockResponse().setResponseCode(204))
        val r = checkAuthEsperando()
        assertTrue("Se esperaba Success, llegó $r", r is ApiResult.Success)
        assertEquals(204, (r as ApiResult.Success).estadoHttp)
    }

    @Test
    fun `envia el token como Bearer`() {
        servidor.enqueue(MockResponse().setResponseCode(204))
        checkAuthEsperando()
        val peticion = servidor.takeRequest()
        assertEquals("Bearer TOKEN-DE-PRUEBA", peticion.getHeader("Authorization"))
        assertEquals("/api/v1/auth/check", peticion.path)
        assertEquals("GET", peticion.method)
    }

    @Test
    fun `401 se traduce a token no valido con accion de ajustes`() {
        servidor.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"code":"UNAUTHORIZED","message":"no autorizado"}"""),
        )
        val r = checkAuthEsperando()
        assertTrue(r is ApiResult.Failure)
        r as ApiResult.Failure
        assertEquals("Token no válido. Revisa los ajustes", r.error.texto)
        assertEquals(AccionError.IR_A_AJUSTES, r.error.accion)
        assertEquals(401, r.estadoHttp)
    }

    @Test
    fun `401 sin cuerpo json cae al estado http`() {
        servidor.enqueue(MockResponse().setResponseCode(401).setBody("no autorizado"))
        val r = checkAuthEsperando()
        assertTrue(r is ApiResult.Failure)
        assertEquals("Token no válido. Revisa los ajustes", (r as ApiResult.Failure).error.texto)
    }

    @Test
    fun `500 ofrece reintentar`() {
        servidor.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"code":"INTERNAL_ERROR","message":"boom"}"""),
        )
        val r = checkAuthEsperando()
        assertTrue(r is ApiResult.Failure)
        r as ApiResult.Failure
        assertEquals(AccionError.REINTENTAR, r.error.accion)
        assertEquals(500, r.estadoHttp)
    }

    @Test
    fun `conexion rechazada avisa sin cerrar la app`() {
        // El servidor se cierra antes de la petición: OkHttp recibe un fallo de conexión.
        val url = servidor.url("/")
        servidor.shutdown()
        val r = ApiClient(
            base = url.toString().trimEnd('/'),
            token = "t",
            cliente = clienteDePrueba,
            principal = Handler(Looper.getMainLooper()),
        ).let { api ->
            var obtenido: ApiResult<Unit>? = null
            val espera = CountDownLatch(1)
            api.checkAuth { obtenido = it; espera.countDown() }
            shadowOf(Looper.getMainLooper()).idle()
            run { val limite=System.nanoTime()+TimeUnit.SECONDS.toNanos(5); while(espera.count>0 && System.nanoTime()<limite){shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10)} }
            shadowOf(Looper.getMainLooper()).idle()
            obtenido
        }
        assertTrue(r is ApiResult.Failure)
        r as ApiResult.Failure
        assertEquals("No se pudo conectar con el servidor", r.error.texto)
        assertEquals(AccionError.REINTENTAR, r.error.accion)
    }

    @Test
    fun `tiempo agotado se traduce a sin conexion`() {
        // El servidor acepta la conexión pero nunca responde.
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val r = checkAuthEsperando()
        assertTrue("Se esperaba Failure por timeout, llegó $r", r is ApiResult.Failure)
        assertEquals("No se pudo conectar con el servidor", (r as ApiResult.Failure).error.texto)
    }
}
