package com.runnersz.reto2.android.api

import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*

class ApiErrorTest {

    @Test
    fun apiErrorShouldParseFromJson() {
        val gson = Gson()
        val json = """{"code":"UNAUTHORIZED","message":"Token no válido"}"""
        val error = gson.fromJson(json, ApiError::class.java)

        assertEquals("UNAUTHORIZED", error.code)
        assertEquals("Token no válido", error.message)
    }

    @Test
    fun apiErrorShouldHandleNullFields() {
        val gson = Gson()
        val json = """{"code":"EMPTY_FILE"}"""
        val error = gson.fromJson(json, ApiError::class.java)

        assertEquals("EMPTY_FILE", error.code)
        assertNull(error.message)
    }

    @Test
    fun apiErrorDefaultConstructorShouldWork() {
        val error = ApiError()
        assertNull(error.code)
        assertNull(error.message)
    }
}
