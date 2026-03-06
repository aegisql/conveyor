package com.aegisql.conveyor.utils.http;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SimpleJsonTest {

    record Person(String name, int age) {
    }

    enum Status {
        READY
    }

    public static final class Bean {
        private final String value;

        public Bean(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static final class ExplodingBean {
        public String getValue() {
            throw new IllegalStateException("boom");
        }
    }

    public static final class WriteOnlyBean {
        public void setValue(String value) {
            // write-only on purpose
        }
    }

    record ExplodingRecord(String value) {
        @Override
        public String value() {
            throw new IllegalStateException("boom");
        }
    }

    @Test
    void writesAndParsesStructuredValues() {
        Map<String, Object> source = Map.of(
                "person", new Person("John", 42),
                "bean", new Bean("x"),
                "list", List.of(1, true, "z")
        );

        String json = SimpleJson.write(source);
        Object parsed = SimpleJson.parse(json);

        assertInstanceOf(Map.class, parsed);
        Map<?, ?> map = (Map<?, ?>) parsed;
        assertEquals(Map.of("name", "John", "age", 42L), map.get("person"));
        assertEquals(Map.of("value", "x"), map.get("bean"));
        assertEquals(List.of(1L, true, "z"), map.get("list"));
    }

    @Test
    void handlesEscapesAndNumbers() {
        Object parsed = SimpleJson.parse("{\"text\":\"line\\nvalue\",\"decimal\":10.5,\"flag\":true}");

        assertEquals(Map.of(
                "text", "line\nvalue",
                "decimal", new java.math.BigDecimal("10.5"),
                "flag", true
        ), parsed);
    }

    @Test
    void rejectsCycles() {
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("self", map);

        assertThrows(IllegalArgumentException.class, () -> SimpleJson.write(map));
    }

    @Test
    void codecEncodesTextJsonBinaryAndQueryValues() {
        DefaultHttpValueCodec codec = new DefaultHttpValueCodec();

        assertEquals("application/json", codec.encodeBody(null).contentType());
        HttpValueCodec.EncodedBody encodedBody = HttpValueCodec.EncodedBody.text("abc");
        assertSame(encodedBody, codec.encodeBody(encodedBody));
        assertEquals("text/plain; charset=UTF-8", codec.encodeBody("plain").contentType());
        assertEquals("text/plain; charset=UTF-8", codec.encodeBody('x').contentType());
        assertEquals("application/json", codec.encodeBody("{\"a\":1}").contentType());
        assertEquals("application/octet-stream", codec.encodeBody(new byte[]{1, 2}).contentType());
        assertEquals("application/json", codec.encodeBody(5).contentType());
        assertEquals("application/json", codec.encodeBody(true).contentType());
        assertEquals("application/json", codec.encodeBody(Status.READY).contentType());
        assertEquals("{\"a\":1}", new String(codec.encodeBody(Map.of("a", 1)).body()));
        assertEquals("x", codec.encodeQueryValue('x'));
        assertEquals("true", codec.encodeQueryValue(true));
        assertEquals("READY", codec.encodeQueryValue(Status.READY));
        assertEquals("{\"a\":1}", codec.encodeQueryValue(Map.of("a", 1)));

        HttpValueCodec.EncodedBody text = HttpValueCodec.EncodedBody.text("abc");
        HttpValueCodec.EncodedBody json = HttpValueCodec.EncodedBody.json("{}");
        HttpValueCodec.EncodedBody binary = HttpValueCodec.EncodedBody.binary(new byte[]{9});
        assertEquals("text/plain; charset=UTF-8", text.contentType());
        assertEquals("application/json", json.contentType());
        assertEquals(1, binary.body().length);
    }

    @Test
    void supportsAdditionalStructuredTypes() {
        String json = SimpleJson.write(Map.of(
                "optional", Optional.empty(),
                "character", 'x',
                "enum", Status.READY,
                "instant", Instant.parse("2026-03-05T10:15:30Z"),
                "array", new int[]{1, 2},
                "list", List.of("a", "b")
        ));

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) SimpleJson.parse(json);
        assertNull(parsed.get("optional"));
        assertEquals("x", parsed.get("character"));
        assertEquals("READY", parsed.get("enum"));
        assertEquals("2026-03-05T10:15:30Z", parsed.get("instant"));
        assertEquals(List.of(1L, 2L), parsed.get("array"));
        assertEquals(List.of("a", "b"), parsed.get("list"));

        assertTrue(SimpleJson.looksLikeJson(" {\"a\":1} "));
        assertTrue(SimpleJson.looksLikeJson("[1,2]"));
        assertTrue(SimpleJson.looksLikeJson("\"x\""));
        assertFalse(SimpleJson.looksLikeJson("plain"));
        assertFalse(SimpleJson.looksLikeJson(null));
        assertEquals(List.of(1L, 2L), SimpleJson.parse("[1,2]".getBytes()));
    }

    @Test
    void rejectsNonFiniteNumbersAndBadJsonSyntax() {
        assertThrows(IllegalArgumentException.class, () -> SimpleJson.write(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> SimpleJson.write(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> SimpleJson.parse("{\"a\":1} trailing"));
        assertThrows(IllegalArgumentException.class, () -> SimpleJson.parse("\"\\x\""));
        assertThrows(IllegalArgumentException.class, () -> SimpleJson.parse("\"\\u12\""));
        assertThrows(IllegalArgumentException.class, () -> SimpleJson.parse("tru"));
        assertThrows(IllegalArgumentException.class, () -> SimpleJson.parse("1e"));
        assertThrows(IllegalArgumentException.class, () -> SimpleJson.parse("\"unterminated"));
        assertThrows(IllegalArgumentException.class, () -> SimpleJson.parse("\"\\"));
        assertThrows(IllegalArgumentException.class, () -> SimpleJson.parse(""));
        assertEquals(Map.of("slash", "/", "unicode", "A"), SimpleJson.parse("{\"slash\":\"\\/\",\"unicode\":\"\\u0041\"}"));
    }

    @Test
    void wrapsAccessorFailures() {
        IllegalArgumentException beanError = assertThrows(IllegalArgumentException.class, () -> SimpleJson.write(new ExplodingBean()));
        assertInstanceOf(java.lang.reflect.InvocationTargetException.class, beanError.getCause());

        IllegalArgumentException recordError = assertThrows(IllegalArgumentException.class, () -> SimpleJson.write(new ExplodingRecord("x")));
        assertInstanceOf(java.lang.reflect.InvocationTargetException.class, recordError.getCause());
    }

    @Test
    void escapesStringsAndSupportsAdditionalParserTokens() {
        String json = SimpleJson.write(Map.of(
                "quoted", "\"",
                "slash", "\\",
                "backspace", "\b",
                "formfeed", "\f",
                "newline", "\n",
                "carriage", "\r",
                "tab", "\t",
                "control", "\u0001"
        ));

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) SimpleJson.parse(json);
        assertEquals("\"", parsed.get("quoted"));
        assertEquals("\\", parsed.get("slash"));
        assertEquals("\b", parsed.get("backspace"));
        assertEquals("\f", parsed.get("formfeed"));
        assertEquals("\n", parsed.get("newline"));
        assertEquals("\r", parsed.get("carriage"));
        assertEquals("\t", parsed.get("tab"));
        assertEquals("\u0001", parsed.get("control"));

        assertEquals(Map.of("flag", false), SimpleJson.parse("{\"flag\":false}"));
        assertEquals(new java.math.BigDecimal("-1.25e+2"), SimpleJson.parse("-1.25e+2"));
        assertEquals(new java.math.BigDecimal("1.25e-2"), SimpleJson.parse("1.25e-2"));
        assertEquals("{}", SimpleJson.write(new WriteOnlyBean()));
    }
}
