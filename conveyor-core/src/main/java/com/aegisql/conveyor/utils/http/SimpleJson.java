package com.aegisql.conveyor.utils.http;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class SimpleJson {

    private SimpleJson() {
    }

    static boolean looksLikeJson(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("\"");
    }

    static String write(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(value, builder, new IdentityHashMap<>());
        return builder.toString();
    }

    static Object parse(byte[] bytes) {
        return parse(new String(bytes, StandardCharsets.UTF_8));
    }

    static Object parse(String json) {
        Objects.requireNonNull(json, "JSON source must be provided");
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isEof()) {
            throw new IllegalArgumentException("Unexpected trailing JSON content");
        }
        return value;
    }

    private static void writeValue(Object value, StringBuilder builder, IdentityHashMap<Object, Boolean> visited) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String || value instanceof Character || value instanceof Enum<?> || value instanceof TemporalAccessor) {
            writeString(value.toString(), builder);
            return;
        }
        if (value instanceof Boolean) {
            builder.append(value);
            return;
        }
        if (value instanceof Number number) {
            if (number instanceof Double d && (!Double.isFinite(d))) {
                throw new IllegalArgumentException("Non-finite doubles are not supported in JSON");
            }
            if (number instanceof Float f && (!Float.isFinite(f))) {
                throw new IllegalArgumentException("Non-finite floats are not supported in JSON");
            }
            builder.append(number);
            return;
        }
        if (value instanceof Optional<?> optional) {
            writeValue(optional.orElse(null), builder, visited);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            writeMap(map, builder, visited);
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            writeIterable(iterable, builder, visited);
            return;
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            writeArray(value, builder, visited);
            return;
        }
        if (valueClass.isRecord()) {
            writeRecord(value, builder, visited);
            return;
        }
        writeBean(value, builder, visited);
    }

    private static void writeMap(Map<?, ?> map, StringBuilder builder, IdentityHashMap<Object, Boolean> visited) {
        guardCycles(map, visited);
        builder.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            writeString(String.valueOf(entry.getKey()), builder);
            builder.append(':');
            writeValue(entry.getValue(), builder, visited);
            first = false;
        }
        builder.append('}');
        visited.remove(map);
    }

    private static void writeIterable(Iterable<?> iterable, StringBuilder builder, IdentityHashMap<Object, Boolean> visited) {
        guardCycles(iterable, visited);
        builder.append('[');
        boolean first = true;
        for (Object item : iterable) {
            if (!first) {
                builder.append(',');
            }
            writeValue(item, builder, visited);
            first = false;
        }
        builder.append(']');
        visited.remove(iterable);
    }

    private static void writeArray(Object array, StringBuilder builder, IdentityHashMap<Object, Boolean> visited) {
        guardCycles(array, visited);
        builder.append('[');
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            writeValue(Array.get(array, i), builder, visited);
        }
        builder.append(']');
        visited.remove(array);
    }

    private static void writeRecord(Object record, StringBuilder builder, IdentityHashMap<Object, Boolean> visited) {
        guardCycles(record, visited);
        builder.append('{');
        boolean first = true;
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            if (!first) {
                builder.append(',');
            }
            writeString(component.getName(), builder);
            builder.append(':');
            try {
                writeValue(component.getAccessor().invoke(record), builder, visited);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException("Failed to read record component " + component.getName(), e);
            }
            first = false;
        }
        builder.append('}');
        visited.remove(record);
    }

    private static void writeBean(Object bean, StringBuilder builder, IdentityHashMap<Object, Boolean> visited) {
        guardCycles(bean, visited);
        try {
            var descriptors = Introspector.getBeanInfo(bean.getClass(), Object.class).getPropertyDescriptors();
            builder.append('{');
            boolean first = true;
            for (var descriptor : descriptors) {
                if (descriptor.getReadMethod() == null) {
                    continue;
                }
                if (!first) {
                    builder.append(',');
                }
                writeString(descriptor.getName(), builder);
                builder.append(':');
                try {
                    writeValue(descriptor.getReadMethod().invoke(bean), builder, visited);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalArgumentException("Failed to read bean property " + descriptor.getName(), e);
                }
                first = false;
            }
            builder.append('}');
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException("Failed to introspect bean " + bean.getClass().getName(), e);
        } finally {
            visited.remove(bean);
        }
    }

    private static void guardCycles(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (visited.containsKey(value)) {
            throw new IllegalArgumentException("Cyclic JSON structures are not supported");
        }
        visited.put(value, Boolean.TRUE);
    }

    private static void writeString(String value, StringBuilder builder) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        builder.append('"');
    }

    private static final class Parser {

        private final String source;
        private int index;

        private Parser(String source) {
            this.source = source;
        }

        private Object parseValue() {
            skipWhitespace();
            if (isEof()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            char ch = source.charAt(index);
            return switch (ch) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return map;
            }
            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                map.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            ArrayList<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (!isEof()) {
                char ch = source.charAt(index++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch != '\\') {
                    builder.append(ch);
                    continue;
                }
                if (isEof()) {
                    throw new IllegalArgumentException("Unterminated escape sequence in JSON string");
                }
                char escaped = source.charAt(index++);
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
                    default -> throw new IllegalArgumentException("Unsupported JSON escape \\" + escaped);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private char parseUnicode() {
            if (index + 4 > source.length()) {
                throw new IllegalArgumentException("Incomplete unicode escape");
            }
            String hex = source.substring(index, index + 4);
            index += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        private Object parseLiteral(String literal, Object value) {
            if (!source.startsWith(literal, index)) {
                throw new IllegalArgumentException("Invalid JSON literal at position " + index);
            }
            index += literal.length();
            return value;
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (!isEof() && Character.isDigit(source.charAt(index))) {
                index++;
            }
            boolean fractional = false;
            if (!isEof() && source.charAt(index) == '.') {
                fractional = true;
                index++;
                while (!isEof() && Character.isDigit(source.charAt(index))) {
                    index++;
                }
            }
            if (!isEof() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
                fractional = true;
                index++;
                if (!isEof() && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
                    index++;
                }
                while (!isEof() && Character.isDigit(source.charAt(index))) {
                    index++;
                }
            }
            String token = source.substring(start, index);
            try {
                if (!fractional) {
                    return Long.parseLong(token);
                }
                return new BigDecimal(token);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid JSON number: " + token, ex);
            }
        }

        private void expect(char expected) {
            skipWhitespace();
            if (isEof() || source.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + index);
            }
            index++;
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return !isEof() && source.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (!isEof() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private boolean isEof() {
            return index >= source.length();
        }
    }
}
