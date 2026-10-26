package com.runnersz.reto2.desktop.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {}

    public static final class ParseException extends IllegalArgumentException {
        public ParseException(String message) {
            super(message);
        }
    }

    public static String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        appendValue(builder, value);
        return builder.toString();
    }

    private static void appendValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String text) {
            appendString(builder, text);
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            builder.append(value);
        } else if (value instanceof Number number) {
            double asDouble = number.doubleValue();
            if (Double.isNaN(asDouble) || Double.isInfinite(asDouble)) {
                throw new IllegalArgumentException("NÃºmero no representable en JSON");
            }
            builder.append(number);
        } else if (value instanceof Map<?, ?> map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                appendString(builder, String.valueOf(entry.getKey()));
                builder.append(':');
                appendValue(builder, entry.getValue());
            }
            builder.append('}');
        } else if (value instanceof Iterable<?> iterable) {
            builder.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                appendValue(builder, item);
            }
            builder.append(']');
        } else {
            throw new IllegalArgumentException("Tipo no soportado en JSON: " + value.getClass());
        }
    }

    private static void appendString(StringBuilder builder, String text) {
        builder.append('"');
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }
        builder.append('"');
    }

    public static Map<String, Object> parseObject(String json) {
        if (json == null) {
            throw new ParseException("JSON vacÃ­o");
        }
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new ParseException("Contenido sobrante tras el JSON");
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new ParseException("Se esperaba un objeto JSON");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static final class Parser {
        private final String json;
        private int index;

        private Parser(String json) {
            this.json = json;
        }

        private boolean atEnd() {
            return index >= json.length();
        }

        private void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }

        private Object parseValue() {
            skipWhitespace();
            if (atEnd()) {
                throw new ParseException("JSON incompleto");
            }
            char character = json.charAt(index);
            return switch (character) {
                case '{' -> parseObjectValue();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (character == '-' || Character.isDigit(character)) {
                        yield parseNumber();
                    }
                    throw new ParseException("Valor JSON inesperado en posiciÃ³n " + index);
                }
            };
        }

        private Map<String, Object> parseObjectValue() {
            Map<String, Object> map = new LinkedHashMap<>();
            index++;
            skipWhitespace();
            if (!atEnd() && json.charAt(index) == '}') {
                index++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (atEnd() || json.charAt(index) != '"') {
                    throw new ParseException("Se esperaba una clave de objeto");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (atEnd()) {
                    throw new ParseException("Objeto JSON sin cerrar");
                }
                char character = json.charAt(index++);
                if (character == '}') {
                    return map;
                }
                if (character != ',') {
                    throw new ParseException("Se esperaba ',' o '}'");
                }
            }
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            index++;
            skipWhitespace();
            if (!atEnd() && json.charAt(index) == ']') {
                index++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (atEnd()) {
                    throw new ParseException("Array JSON sin cerrar");
                }
                char character = json.charAt(index++);
                if (character == ']') {
                    return list;
                }
                if (character != ',') {
                    throw new ParseException("Se esperaba ',' o ']'");
                }
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (!atEnd()) {
                char character = json.charAt(index++);
                if (character == '"') {
                    return builder.toString();
                }
                if (character == '\\') {
                    if (atEnd()) {
                        throw new ParseException("Escape incompleto");
                    }
                    char escaped = json.charAt(index++);
                    switch (escaped) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case '/' -> builder.append('/');
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> builder.append(parseUnicode());
                        default -> throw new ParseException("Escape no vÃ¡lido: \\" + escaped);
                    }
                } else if (character < 0x20) {
                    throw new ParseException("CarÃ¡cter de control sin escapar");
                } else {
                    builder.append(character);
                }
            }
            throw new ParseException("Cadena JSON sin cerrar");
        }

        private char parseUnicode() {
            if (index + 4 > json.length()) {
                throw new ParseException("Secuencia unicode incompleta");
            }
            String hex = json.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw new ParseException("Secuencia unicode no vÃ¡lida: " + hex);
            }
        }

        private Object parseNumber() {
            int start = index;
            if (json.charAt(index) == '-') {
                index++;
            }
            while (!atEnd() && Character.isDigit(json.charAt(index))) {
                index++;
            }
            boolean decimal = false;
            if (!atEnd() && json.charAt(index) == '.') {
                decimal = true;
                index++;
                while (!atEnd() && Character.isDigit(json.charAt(index))) {
                    index++;
                }
            }
            if (!atEnd() && (json.charAt(index) == 'e' || json.charAt(index) == 'E')) {
                decimal = true;
                index++;
                if (!atEnd() && (json.charAt(index) == '+' || json.charAt(index) == '-')) {
                    index++;
                }
                while (!atEnd() && Character.isDigit(json.charAt(index))) {
                    index++;
                }
            }
            String token = json.substring(start, index);
            try {
                if (decimal) {
                    return Double.parseDouble(token);
                }
                return Long.parseLong(token);
            } catch (NumberFormatException exception) {
                throw new ParseException("NÃºmero JSON no vÃ¡lido: " + token);
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!json.startsWith(literal, index)) {
                throw new ParseException("Literal JSON no vÃ¡lido en posiciÃ³n " + index);
            }
            index += literal.length();
            return value;
        }

        private void expect(char expected) {
            if (atEnd() || json.charAt(index) != expected) {
                throw new ParseException("Se esperaba '" + expected + "' en posiciÃ³n " + index);
            }
            index++;
        }
    }
}
