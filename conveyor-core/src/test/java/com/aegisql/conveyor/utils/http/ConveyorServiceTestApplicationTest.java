package com.aegisql.conveyor.utils.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConveyorServiceTestApplicationTest {

    @Test
    void replaysPipeDelimitedFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("sample.psv");
        Files.writeString(file, """
                CONVEYOR_NAME|ID|LABEL|BODY|source
                collector|1001|USER|{"name":"John"}|bulk
                collector||CONFIG|{"enabled":true}|seed
                """);

        ConcurrentLinkedQueue<String> paths = new ConcurrentLinkedQueue<>();
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            paths.add(exchange.getRequestURI().getPath() + "?" + exchange.getRequestURI().getRawQuery());
            return new JsonResponse(200, SimpleJson.write(Map.of("status", "COMPLETED", "result", true, "properties", Map.of())));
        })) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            try {
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
                ConveyorServiceTestApplication.main(new String[]{
                        "--base-url", server.baseUrl(),
                        "--auth-mode", "none",
                        "--file", file.toString()
                });
            } finally {
                System.setOut(originalOut);
            }

            List<String> recorded = new ArrayList<>(paths);
            assertEquals(2, recorded.size());
            assertTrue(recorded.stream().anyMatch(path -> path.startsWith("/part/collector/1001/USER")));
            assertTrue(recorded.stream().anyMatch(path -> path.startsWith("/static-part/collector/CONFIG")));
            assertTrue(out.toString(StandardCharsets.UTF_8).contains("part collector 1001 USER -> true"));
            assertTrue(out.toString(StandardCharsets.UTF_8).contains("static-part collector CONFIG -> true"));
        }
    }

    @Test
    void sendsFixedSequenceWithoutFile() throws Exception {
        ConcurrentLinkedQueue<String> paths = new ConcurrentLinkedQueue<>();
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            return new JsonResponse(200, SimpleJson.write(Map.of("status", "COMPLETED", "result", true, "properties", Map.of())));
        })) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            try {
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
                ConveyorServiceTestApplication.main(new String[]{
                        "--base-url", server.baseUrl(),
                        "--auth-mode", "none",
                        "--conveyor", "collector",
                        "--id", "42"
                });
            } finally {
                System.setOut(originalOut);
            }

            assertEquals(List.of(
                    "/part/collector/42/USER",
                    "/part/collector/42/ADDRESS",
                    "/part/collector/42/DONE"
            ), new ArrayList<>(paths));
            assertTrue(out.toString(StandardCharsets.UTF_8).contains("Sent 3 part-loader messages"));
        }
    }

    @Test
    void emptyFileIsRejected(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("empty.psv");
        Files.writeString(file, "");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ConveyorServiceTestApplication.main(new String[]{
                        "--base-url", "http://localhost:8080",
                        "--auth-mode", "none",
                        "--file", file.toString()
                }));
        assertTrue(error.getMessage().contains("Input file is empty"));
    }

    @Test
    void optionsParsingAndAuthenticationAreCovered() throws Exception {
        Object positionalOptions = parseOptions("collector-x", "id-7");
        assertEquals("collector-x", invokeRecord(positionalOptions, "conveyor"));
        assertEquals("id-7", invokeRecord(positionalOptions, "id"));

        Object noneOptions = parseOptions("--base-url", "http://localhost:8080", "--auth-mode", "none", "--shuffle");
        assertEquals("http://localhost:8080", invokeRecord(noneOptions, "baseUrl"));
        assertEquals(true, invokeRecord(noneOptions, "shuffle"));
        @SuppressWarnings("unchecked")
        Map<String, Object> defaults = (Map<String, Object>) invokePrivate(noneOptions, "defaultProperties");
        assertEquals("1 SECONDS", defaults.get("ttl"));
        assertEquals("100", defaults.get("requestTTL"));
        assertInstanceOf(ConveyorServiceAuthentication.class, invokePrivate(noneOptions, "authentication"));

        Object bearerOptions = parseOptions("--auth-mode", "bearer", "--token", "token-1");
        ConveyorServiceAuthentication bearer = (ConveyorServiceAuthentication) invokePrivate(bearerOptions, "authentication");
        HttpRequest.Builder bearerBuilder = HttpRequest.newBuilder(java.net.URI.create("http://localhost"));
        bearer.apply(bearerBuilder);
        assertEquals("Bearer token-1", bearerBuilder.build().headers().firstValue("Authorization").orElseThrow());

        Object cookieOptions = parseOptions("--auth-mode", "cookie", "--cookie", "JSESSIONID=x");
        ConveyorServiceAuthentication cookie = (ConveyorServiceAuthentication) invokePrivate(cookieOptions, "authentication");
        HttpRequest.Builder cookieBuilder = HttpRequest.newBuilder(java.net.URI.create("http://localhost"));
        cookie.apply(cookieBuilder);
        assertEquals("JSESSIONID=x", cookieBuilder.build().headers().firstValue("Cookie").orElseThrow());

        Object basicOptions = parseOptions("--auth-mode", "basic", "--user", "rest", "--password", "rest");
        assertInstanceOf(ConveyorServiceAuthentication.class, invokePrivate(basicOptions, "authentication"));

        Object sessionOptions = parseOptions("--auth-mode", "session", "--user", "admin", "--password", "admin");
        assertInstanceOf(ConveyorServiceAuthentication.class, invokePrivate(sessionOptions, "authentication"));

        InvocationTargetException unsupported = assertThrows(InvocationTargetException.class,
                () -> invokePrivate(parseOptions("--auth-mode", "weird"), "authentication"));
        assertInstanceOf(IllegalArgumentException.class, unsupported.getTargetException());

        InvocationTargetException missingToken = assertThrows(InvocationTargetException.class,
                () -> invokePrivate(parseOptions("--auth-mode", "bearer"), "authentication"));
        assertInstanceOf(IllegalArgumentException.class, missingToken.getTargetException());

        InvocationTargetException missingValue = assertThrows(InvocationTargetException.class,
                () -> parseOptions("--base-url"));
        assertInstanceOf(IllegalArgumentException.class, missingValue.getTargetException());

        String usage = (String) invokePrivate(noneOptions, "usageText");
        assertTrue(usage.contains("ConveyorServiceTestApplication [options]"));
    }

    @Test
    void headerParsingCoversPartAndStaticRows() throws Exception {
        Object partHeader = parseHeader("CONVEYOR_NAME|ID|LABEL|BODY|source|delete");
        Object partRow = invokePrivate(partHeader, "parseRow", "collector|1001|USER|{\"name\":\"John\"}|bulk|");
        assertEquals("collector", invokeRecord(partRow, "conveyor"));
        assertEquals("1001", invokeRecord(partRow, "id"));
        assertEquals("USER", invokeRecord(partRow, "label"));
        assertEquals(true, invokeRecord(partRow, "partRequest"));
        @SuppressWarnings("unchecked")
        Map<String, Object> partProperties = (Map<String, Object>) invokeRecord(partRow, "properties");
        assertEquals(Map.of("source", "bulk"), partProperties);

        Object staticHeader = parseHeader("CONVEYOR_NAME|LABEL|BODY|source");
        Object staticRow = invokePrivate(staticHeader, "parseRow", "collector|CONFIG|{\"enabled\":true}|seed");
        assertEquals(false, invokeRecord(staticRow, "partRequest"));
        @SuppressWarnings("unchecked")
        Map<String, Object> staticProperties = (Map<String, Object>) invokeRecord(staticRow, "properties");
        assertEquals(Map.of("source", "seed"), staticProperties);

        InvocationTargetException badHeader = assertThrows(InvocationTargetException.class,
                () -> parseHeader("CONVEYOR_NAME|LABEL"));
        assertInstanceOf(IllegalArgumentException.class, badHeader.getTargetException());

        InvocationTargetException badRow = assertThrows(InvocationTargetException.class,
                () -> invokePrivate(staticHeader, "parseRow", "collector|CONFIG"));
        assertInstanceOf(IllegalArgumentException.class, badRow.getTargetException());
    }

    @Test
    void replayFileSupportsDeleteBlankLinesAndShuffle(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("sample.psv");
        Files.writeString(file, """
                CONVEYOR_NAME|ID|LABEL|BODY|delete

                collector||CONFIG|ignored|true
                collector|1001|USER|{"name":"John"}|
                """);

        List<String> paths = new ArrayList<>();
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            return new JsonResponse(200, SimpleJson.write(Map.of("status", "COMPLETED", "result", true, "properties", Map.of())));
        })) {
            ConveyorServiceTestApplication.main(new String[]{
                    "--base-url", server.baseUrl(),
                    "--auth-mode", "none",
                    "--file", file.toString(),
                    "--shuffle"
            });
        }

        assertEquals(2, paths.size());
        assertTrue(paths.contains("/static-part/collector/CONFIG"));
        assertTrue(paths.contains("/part/collector/1001/USER"));
    }

    private record JsonResponse(int status, String body) {
    }

    private static Object parseOptions(String... args) throws Exception {
        Class<?> optionsClass = Class.forName("com.aegisql.conveyor.utils.http.ConveyorServiceTestApplication$Options");
        Method parse = optionsClass.getDeclaredMethod("parse", String[].class);
        parse.setAccessible(true);
        return parse.invoke(null, (Object) args);
    }

    private static Object parseHeader(String line) throws Exception {
        Class<?> headerClass = Class.forName("com.aegisql.conveyor.utils.http.ConveyorServiceTestApplication$Header");
        Method parse = headerClass.getDeclaredMethod("parse", String.class);
        parse.setAccessible(true);
        return parse.invoke(null, line);
    }

    private static Object invokePrivate(Object target, String methodName, Object... args) throws Exception {
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                method.setAccessible(true);
                return method.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private static Object invokeRecord(Object target, String componentName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(componentName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static final class TestHttpServer implements AutoCloseable {
        private final HttpServer server;
        private final java.util.function.Function<HttpExchange, JsonResponse> handler;

        private TestHttpServer(java.util.function.Function<HttpExchange, JsonResponse> handler) throws IOException {
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            this.handler = handler;
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", exchange -> {
                JsonResponse response = handler.apply(exchange);
                byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(response.status(), body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            server.start();
        }

        private String baseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
