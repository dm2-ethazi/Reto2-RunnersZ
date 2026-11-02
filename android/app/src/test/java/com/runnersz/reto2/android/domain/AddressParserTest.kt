package com.runnersz.reto2.android.domain

import com.runnersz.reto2.android.domain.AddressParser.AVISO_HTTPS
import com.runnersz.reto2.android.domain.AddressParser.FALTA_PUERTO
import com.runnersz.reto2.android.domain.AddressParser.PUERTO_INVALIDO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del analizador de dirección (DJ8), según §5.2:
 * «válidas, sin puerto, puerto 0/65536, `https://`, espacios».
 */
class AddressParserTest {

    @Test
    fun `acepta host y puerto con prefijo http`() {
        val r = AddressParser.parse("http://192.168.1.20:8000")
        assertTrue(r is Direccion.Valida)
        assertEquals("http://192.168.1.20:8000", (r as Direccion.Valida).url)
    }

    @Test
    fun `acepta host y puerto sin prefijo y lo normaliza`() {
        val r = AddressParser.parse("10.0.2.2:8000")
        assertTrue(r is Direccion.Valida)
        assertEquals("http://10.0.2.2:8000", (r as Direccion.Valida).url)
    }

    @Test
    fun `recorta espacios sobrantes`() {
        val r = AddressParser.parse("   localhost:8000   ")
        assertEquals("http://localhost:8000", (r as Direccion.Valida).url)
    }

    @Test
    fun `acepta el puerto minimo y el maximo`() {
        assertTrue(AddressParser.parse("h:1") is Direccion.Valida)
        assertTrue(AddressParser.parse("h:65535") is Direccion.Valida)
    }

    @Test
    fun `rechaza el puerto 0`() {
        val r = AddressParser.parse("192.168.1.20:0")
        assertTrue(r is Direccion.Invalida)
        assertEquals(PUERTO_INVALIDO, (r as Direccion.Invalida).motivo)
    }

    @Test
    fun `rechaza el puerto 65536`() {
        val r = AddressParser.parse("192.168.1.20:65536")
        assertTrue(r is Direccion.Invalida)
        assertEquals(PUERTO_INVALIDO, (r as Direccion.Invalida).motivo)
    }

    @Test
    fun `rechaza una direccion sin puerto`() {
        val r = AddressParser.parse("192.168.1.20")
        assertTrue(r is Direccion.Invalida)
        assertEquals(FALTA_PUERTO, (r as Direccion.Invalida).motivo)
    }

    @Test
    fun `rechaza el puerto vacio tras los dos puntos`() {
        val r = AddressParser.parse("192.168.1.20:")
        assertTrue(r is Direccion.Invalida)
    }

    @Test
    fun `rechaza https con aviso y no como error de formato`() {
        val r = AddressParser.parse("https://192.168.1.20:8000")
        assertTrue(r is Direccion.Invalida)
        r as Direccion.Invalida
        assertEquals(AVISO_HTTPS, r.motivo)
        assertTrue("https debe marcarse como aviso, no como error", r.aviso)
    }

    @Test
    fun `rechaza un puerto no numerico`() {
        assertTrue(AddressParser.parse("host:ochenta") is Direccion.Invalida)
    }

    @Test
    fun `rechaza la cadena vacia`() {
        assertTrue(AddressParser.parse("   ") is Direccion.Invalida)
    }
}
