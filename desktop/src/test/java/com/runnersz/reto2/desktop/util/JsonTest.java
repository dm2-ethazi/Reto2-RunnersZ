package com.runnersz.reto2.desktop.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {
    @Test
    void stringifyEscapaCaracteresEspeciales() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("texto", "l\u00ednea\n\"comillas\"\\ y \t tabulador");
        payload.put("control", "\u0001");
        String json = Json.stringify(payload);
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\\"comillas\\\""));
        assertTrue(json.contains("\\\\"));
        assertTrue(json.contains("\\t"));
        assertTrue(json.contains("\\u0001"));
        Map<String, Object> parsed = Json.parseObject(json);
        assertEquals(payload.get("texto"), parsed.get("texto"));
        assertEquals(payload.get("control"), parsed.get("control"));
    }

    @Test
    void idaYVueltaDeTipos() {
        String json =
                "{\"texto\":\"Hola\",\"numero\":42,\"decimal\":3.5,\"negativo\":-7,"
                        + "\"si\":true,\"no\":false,\"nada\":null,"
                        + "\"lista\":[1,\"dos\",null],\"objeto\":{\"clave\":\"valor\"}}";
        Map<String, Object> parsed = Json.parseObject(json);
        assertEquals("Hola", parsed.get("texto"));
        assertEquals(42L, parsed.get("numero"));
        assertEquals(3.5, parsed.get("decimal"));
        assertEquals(-7L, parsed.get("negativo"));
        assertEquals(Boolean.TRUE, parsed.get("si"));
        assertEquals(Boolean.FALSE, parsed.get("no"));
        assertTrue(parsed.containsKey("nada"));
        assertNull(parsed.get("nada"));
        assertInstanceOf(List.class, parsed.get("lista"));
        assertInstanceOf(Map.class, parsed.get("objeto"));
    }

    @Test
    void stringifySoportaListas() {
        assertEquals("[1,\"dos\",true]", Json.stringify(List.of(1, "dos", true)));
        assertEquals("[]", Json.stringify(List.of()));
        assertEquals("{}", Json.stringify(Map.of()));
    }

    @Test
    void parseAceptaEspacios() {
        Map<String, Object> parsed = Json.parseObject("  { \"a\" : 1 }  ");
        assertEquals(1L, parsed.get("a"));
    }

    @Test
    void parseRechazaEntradasInvalidas() {
        assertThrows(Json.ParseException.class, () -> Json.parseObject(null));
        assertThrows(Json.ParseException.class, () -> Json.parseObject(""));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("[]"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{}x"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":}"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\" 1}"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":1"));
    }

    @Test
    void parseRechazaCadenasInvalidas() {
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":\"sin cerrar}"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":\"\\x\"}"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":\"\\u12\"}"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":\"\\uZZZZ\"}"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":\"\u0001\"}"));
    }

    @Test
    void parseRechazaArraysYLiteralesInvalidos() {
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":[1,2}"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":[1 2]}"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":tru}"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":@}"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":1,}"));
    }

    @Test
    void parseRechazaNumerosInvalidos() {
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":-}"));
        assertThrows(Json.ParseException.class, () -> Json.parseObject("{\"a\":1e}"));
    }

    @Test
    void parseAceptaNotacionCientifica() {
        Map<String, Object> parsed = Json.parseObject("{\"a\":1.5e2,\"b\":-2E-1}");
        assertEquals(150.0, parsed.get("a"));
        assertEquals(-0.2, parsed.get("b"));
    }

    @Test
    void stringifyRechazaTiposYNumerosNoValidos() {
        assertThrows(IllegalArgumentException.class, () -> Json.stringify(new Object()));
        assertThrows(IllegalArgumentException.class, () -> Json.stringify(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Json.stringify(Double.POSITIVE_INFINITY));
    }

    @Test
    void parseAceptaEscapesUnicodeYBarra() {
        Map<String, Object> parsed = Json.parseObject("{\"a\":\"\\u00f1\\/\"}");
        assertEquals("\u00f1/", parsed.get("a"));
    }

    @Test
    void parseAceptaObjetosYArraysVacios() {
        Map<String, Object> parsed = Json.parseObject("{\"a\":{},\"b\":[]}");
        assertTrue(((Map<?, ?>) parsed.get("a")).isEmpty());
        assertTrue(((List<?>) parsed.get("b")).isEmpty());
        assertFalse(parsed.isEmpty());
    }
}

