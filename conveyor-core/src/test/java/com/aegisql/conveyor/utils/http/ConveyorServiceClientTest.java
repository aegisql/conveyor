package com.aegisql.conveyor.utils.http;

import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.serial.SerializablePredicate;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.Optional;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.*;

class ConveyorServiceClientTest {

    @Test
    void auditLogUsesFullUrlAndSanitizesSensitiveValues() throws Exception {
        try (LogCapture capture = new LogCapture(ConveyorServiceClient.AUDIT_LOGGER_NAME, Level.INFO);
             TestHttpServer server = new TestHttpServer(exchange ->
                     JsonResponse.ok(202, payload("IN_PROGRESS", null)))) {
            ConveyorServiceClient client = ConveyorServiceClient.builder(server.baseUri())
                    .authentication(ConveyorServiceAuthentication.basic("rest-user", "very-secret-password"))
                    .build();

            boolean accepted = client.part("collector")
                    .id("42")
                    .label("USER")
                    .ttl(1000, TimeUnit.MILLISECONDS)
                    .addProperty("requestTTL", 100)
                    .addProperty("apiToken", "token-123")
                    .addProperty("source", "interactive-ui")
                    .value(Map.of("payload", "top-secret-body-value"))
                    .place()
                    .get();

            assertTrue(accepted);
            String auditLine = capture.joinedMessages();
            assertTrue(auditLine.contains("\"url\":\"http://localhost:"));
            assertTrue(auditLine.contains("/part/collector/42/USER?"));
            assertTrue(auditLine.contains("\"userId\":\"rest-user\""));
            assertTrue(auditLine.contains("\"requestTTL\":\"100\""));
            assertTrue(auditLine.contains("\"apiToken\":\"REDACTED\""));
            assertTrue(auditLine.contains("\"source\":\"PRESENT\""));
            assertFalse(auditLine.contains("very-secret-password"));
            assertFalse(auditLine.contains("token-123"));
            assertFalse(auditLine.contains("interactive-ui"));
            assertFalse(auditLine.contains("top-secret-body-value"));
        }
    }

    @Test
    void debugLogsCaptureMainLifecycleWithoutLeakingSecrets() throws Exception {
        try (LogCapture capture = new LogCapture(ConveyorServiceClient.class.getName(), Level.FINE);
             TestHttpServer server = new TestHttpServer(exchange ->
                     JsonResponse.ok(202, payload("IN_PROGRESS", null)))) {
            ConveyorServiceClient client = ConveyorServiceClient.builder(server.baseUri())
                    .authentication(ConveyorServiceAuthentication.basic("debug-user", "debug-password"))
                    .build();

            boolean accepted = client.part("collector")
                    .id("7")
                    .label("DONE")
                    .addProperty("source", "client-screen")
                    .value("debug-body-value")
                    .place()
                    .get();

            assertTrue(accepted);
            String logs = capture.joinedMessages();
            assertTrue(logs.contains("Created ConveyorServiceClient"));
            assertTrue(logs.contains("Sending request method=POST"));
            assertTrue(logs.contains("Received response method=POST"));
            assertTrue(logs.contains("Parsed response method=POST"));
            assertTrue(logs.contains("authMode=basic"));
            assertTrue(logs.contains("userId=debug-user"));
            assertFalse(logs.contains("debug-password"));
            assertFalse(logs.contains("debug-body-value"));
            assertFalse(logs.contains("client-screen"));
        }
    }

    @Test
    void partLoaderUsesBasicAuthAndEncodesJsonBody() throws Exception {
        AtomicReference<CapturedRequest> captured = new AtomicReference<>();
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            captured.set(CapturedRequest.capture(exchange));
            return JsonResponse.ok(202, payload("IN_PROGRESS", null));
        })) {
            ConveyorServiceClient client = ConveyorServiceClient.builder(server.baseUri())
                    .authentication(ConveyorServiceAuthentication.basic("rest", "rest"))
                    .requestTimeout(Duration.ofSeconds(5))
                    .build();

            boolean accepted = client.part("collector")
                    .id("42")
                    .label("USER")
                    .ttl(1000, TimeUnit.MILLISECONDS)
                    .priority(5)
                    .addProperty("requestTTL", 100)
                    .addProperty("source", "bulk")
                    .value(Map.of("name", "John"))
                    .place()
                    .get();

            assertTrue(accepted);
            CapturedRequest request = captured.get();
            assertNotNull(request);
            assertEquals("/part/collector/42/USER", request.path());
            assertEquals("1000", request.query().get("ttl"));
            assertEquals("100", request.query().get("requestTTL"));
            assertEquals("5", request.query().get("priority"));
            assertEquals("bulk", request.query().get("source"));
            assertTrue(request.header("Content-Type").startsWith("application/json"));
            assertEquals("Basic " + Base64.getEncoder().encodeToString("rest:rest".getBytes(StandardCharsets.UTF_8)),
                    request.header("Authorization"));
            assertEquals(Map.of("name", "John"), SimpleJson.parse(request.body()));
        }
    }

    @Test
    void staticPartDeleteUsesCookieAuth() throws Exception {
        AtomicReference<CapturedRequest> captured = new AtomicReference<>();
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            captured.set(CapturedRequest.capture(exchange));
            return JsonResponse.ok(200, Map.of("status", "COMPLETED", "result", true, "properties", Map.of()));
        })) {
            ConveyorServiceClient client = ConveyorServiceClient.builder(server.baseUri())
                    .authentication(ConveyorServiceAuthentication.cookie("JSESSIONID=session-1"))
                    .build();

            boolean deleted = client.staticPart("collector")
                    .label("CONFIG")
                    .delete()
                    .place()
                    .get();

            assertTrue(deleted);
            CapturedRequest request = captured.get();
            assertNotNull(request);
            assertEquals("/static-part/collector/CONFIG", request.path());
            assertEquals("true", request.query().get("delete"));
            assertEquals("JSESSIONID=session-1", request.header("Cookie"));
            assertEquals("", request.body());
        }
    }

    @Test
    void commandLoaderSupportsBearerAuthPeekAndProperties() throws Exception {
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            CapturedRequest request = CapturedRequest.capture(exchange);
            assertEquals("Bearer token-1", request.header("Authorization"));
            if (request.path().equals("/command/collector/42/addProperties")) {
                assertEquals("A", request.query().get("a"));
                return JsonResponse.ok(200, Map.of("status", "COMPLETED", "result", true, "properties", Map.of()));
            }
            if (request.path().equals("/command/collector/42/peek")) {
                return JsonResponse.ok(200, Map.of("status", "COMPLETED", "result", Map.of("name", "John"), "properties", Map.of()));
            }
            if (request.path().equals("/command/collector/peek")) {
                return JsonResponse.ok(200, Map.of(
                        "status", "COMPLETED",
                        "result", Map.of("1", Map.of("name", "A"), "2", Map.of("name", "B")),
                        "properties", Map.of()
                ));
            }
            if (request.path().equals("/command/collector/42/peekId")) {
                return JsonResponse.ok(200, Map.of("status", "COMPLETED", "result", "42", "properties", Map.of()));
            }
            throw new AssertionError("Unexpected path " + request.path());
        })) {
            ConveyorServiceClient client = ConveyorServiceClient.builder(server.baseUri())
                    .authentication(ConveyorServiceAuthentication.bearer("token-1"))
                    .build();

            assertTrue(client.<String, Object>command("collector").id("42").addProperties(Map.of("a", "A")).get());

            ProductBin<String, Object> bin = client.<String, Object>command("collector").id("42").peek().get();
            assertEquals("42", bin.key);
            assertEquals(Map.of("name", "John"), bin.product);

            List<ProductBin<String, Object>> bins = client.<String, Object>command("collector").foreach().peek().get();
            assertEquals(2, bins.size());
            Map<Object, Object> productsByKey = new LinkedHashMap<>();
            bins.forEach(binItem -> productsByKey.put(binItem.key, binItem.product));
            assertEquals(Map.of("1", Map.of("name", "A"), "2", Map.of("name", "B")), productsByKey);

            List<String> ids = new ArrayList<>();
            assertTrue(client.<String, Object>command("collector").id("42").peekId(ids::add).get());
            assertEquals(List.of("42"), ids);
        }
    }

    @Test
    void unsupportedCommandFailsFast() {
        ConveyorServiceClient client = ConveyorServiceClient.builder(URI.create("http://localhost:8080"))
                .authentication(ConveyorServiceAuthentication.none())
                .build();

        CompletionException error = assertThrows(CompletionException.class, () -> client.command("collector").id("42").check().join());
        assertInstanceOf(UnsupportedOperationException.class, error.getCause());
    }

    @Test
    void sessionAuthenticationLogsInBeforeRequest() throws Exception {
        AtomicInteger loginCount = new AtomicInteger();
        List<CapturedRequest> captured = new ArrayList<>();
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            if (exchange.getRequestURI().getPath().equals("/login")) {
                try {
                    loginCount.incrementAndGet();
                    exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=session-2; Path=/");
                    exchange.sendResponseHeaders(302, -1);
                    exchange.close();
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
                return null;
            }
            captured.add(CapturedRequest.capture(exchange));
            return JsonResponse.ok(202, payload("ACCEPTED", null));
        })) {
            ConveyorServiceClient client = ConveyorServiceClient.builder(server.baseUri())
                    .authentication(ConveyorServiceAuthentication.session("admin", "admin"))
                    .build();

            assertTrue(client.part("collector").id("7").label("DONE").value("{}").place().get());
            assertTrue(client.part("collector").id("8").label("DONE").value("{}").place().get());
            assertEquals(1, loginCount.get());
            assertEquals(2, captured.size());
            assertEquals("JSESSIONID=session-2", captured.getFirst().header("Cookie"));
            assertEquals("JSESSIONID=session-2", captured.get(1).header("Cookie"));
        }
    }

    @Test
    void failedRemotePlacementRaisesConveyorServiceException() throws Exception {
        try (TestHttpServer server = new TestHttpServer(exchange ->
                JsonResponse.ok(200, Map.of(
                        "status", "FAILED",
                        "errorCode", "BROKEN",
                        "errorMessage", "placement failed",
                        "properties", Map.of()
                )))) {
            ConveyorServiceClient client = ConveyorServiceClient.builder(server.baseUri()).build();

            CompletionException error = assertThrows(CompletionException.class,
                    () -> client.part("collector").id("1").label("USER").value("{}").place().join());

            assertInstanceOf(ConveyorServiceException.class, error.getCause());
            ConveyorServiceException serviceException = (ConveyorServiceException) error.getCause();
            assertEquals(200, serviceException.getHttpStatus());
            assertEquals("FAILED", serviceException.getPlacementStatus());
        }
    }

    @Test
    void supportsForeachPartPathCustomCodecAndBuilderOptions() throws Exception {
        AtomicReference<CapturedRequest> captured = new AtomicReference<>();
        HttpValueCodec customCodec = new HttpValueCodec() {
            @Override
            public EncodedBody encodeBody(Object value) {
                return new EncodedBody("application/custom", ("BODY:" + value).getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public String encodeQueryValue(Object value) {
                return "Q:" + value;
            }
        };

        try (TestHttpServer server = new TestHttpServer(exchange -> {
            captured.set(CapturedRequest.capture(exchange));
            return JsonResponse.ok(200, Map.of("status", "COMPLETED", "result", true, "properties", List.of("ignored")));
        })) {
            ConveyorServiceClient client = ConveyorServiceClient.builder(server.baseUri().toString() + "/")
                    .valueCodec(customCodec)
                    .connectTimeout(Duration.ofSeconds(3))
                    .requestTimeout(Duration.ofSeconds(4))
                    .build();

            boolean accepted = client.part("collector")
                    .foreach()
                    .label("USER")
                    .creationTime(0)
                    .value("payload")
                    .addProperty("requestTTL", 100)
                    .place()
                    .get();

            assertTrue(accepted);
            CapturedRequest request = captured.get();
            assertNotNull(request);
            assertEquals("/part/collector/USER", request.path());
            assertEquals("Q:100", request.query().get("requestTTL"));
            assertEquals("application/custom", request.header("Content-Type"));
            assertEquals("BODY:payload", request.body());
            assertNull(request.query().get("creationTime"));
        }
    }

    @Test
    void rejectsInvalidPartAndStaticPartConfigurations() {
        ConveyorServiceClient client = ConveyorServiceClient.builder(URI.create("http://localhost:8080")).build();

        CompletionException missingId = assertThrows(CompletionException.class,
                () -> client.part("collector").label("USER").value("{}").place().join());
        assertInstanceOf(IllegalArgumentException.class, missingId.getCause());

        CompletionException customForEach = assertThrows(CompletionException.class,
                () -> client.<String, String>part("collector")
                        .foreach((SerializablePredicate<String>) id -> id.startsWith("A"))
                        .label("USER")
                        .value("{}")
                        .place()
                        .join());
        assertInstanceOf(UnsupportedOperationException.class, customForEach.getCause());

        CompletionException staticMissingValue = assertThrows(CompletionException.class,
                () -> client.staticPart("collector").label("CONFIG").place().join());
        assertInstanceOf(IllegalArgumentException.class, staticMissingValue.getCause());
    }

    @Test
    void staticPartSupportsPriorityAndDeleteWithoutBody() throws Exception {
        List<CapturedRequest> captured = new ArrayList<>();
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            captured.add(CapturedRequest.capture(exchange));
            return JsonResponse.ok(200, Map.of("status", "COMPLETED", "result", true, "properties", Map.of()));
        })) {
            ConveyorServiceClient client = ConveyorServiceClient.builder(server.baseUri()).build();

            assertTrue(client.staticPart("collector").label("CONFIG").priority(9).value("seed").place().get());
            assertTrue(client.staticPart("collector").label("CONFIG").delete().place().get());

            assertEquals("9", captured.getFirst().query().get("priority"));
            assertEquals("seed", captured.getFirst().body());
            assertEquals("true", captured.get(1).query().get("delete"));
            assertEquals("", captured.get(1).body());
        }
    }

    @Test
    void supportsAdditionalCommandsAndRejectsUnsupportedOnes() throws Exception {
        List<CapturedRequest> captured = new ArrayList<>();
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            captured.add(CapturedRequest.capture(exchange));
            return JsonResponse.ok(200, Map.of("status", "COMPLETED", "result", true, "properties", Map.of()));
        })) {
            ConveyorServiceClient client = ConveyorServiceClient.builder(server.baseUri()).build();

            assertTrue(client.<String, Object>command("collector").id("42").cancel().get());
            assertTrue(client.<String, Object>command("collector").id("42").timeout().get());
            assertTrue(client.<String, Object>command("collector").id("42").reschedule().get());
            assertTrue(client.<String, Object>command("collector").id("42")
                    .creationTime(100)
                    .expirationTime(450)
                    .create()
                    .get());
            assertTrue(client.<String, Object>command("collector").id("42")
                    .completeExceptionally(new IllegalStateException())
                    .get());

            assertEquals("/command/collector/42/cancel", captured.get(0).path());
            assertEquals("/command/collector/42/timeout", captured.get(1).path());
            assertEquals("/command/collector/42/reschedule", captured.get(2).path());
            assertEquals("/command/collector/42/create", captured.get(3).path());
            assertEquals("100", captured.get(3).query().get("creationTime"));
            assertEquals("450", captured.get(3).query().get("expirationTime"));
            assertEquals("350", captured.get(3).query().get("ttl"));
            assertEquals("/command/collector/42/completeExceptionally", captured.get(4).path());
            assertEquals("java.lang.IllegalStateException", captured.get(4).body());
        }

        ConveyorServiceClient client = ConveyorServiceClient.builder(URI.create("http://localhost:8080")).build();

        CompletionException unsupportedFilter = assertThrows(CompletionException.class,
                () -> client.<String, Object>command("collector")
                        .foreach(id -> id.startsWith("x"))
                        .cancel()
                        .join());
        assertInstanceOf(UnsupportedOperationException.class, unsupportedFilter.getCause());

        CompletionException unsupportedComplete = assertThrows(CompletionException.class,
                () -> client.<String, Object>command("collector").id("42").complete("done").join());
        assertInstanceOf(UnsupportedOperationException.class, unsupportedComplete.getCause());

        CompletionException unsupportedCreateSupplier = assertThrows(CompletionException.class,
                () -> client.<String, Object>command("collector")
                        .id("42")
                        .create(BuilderSupplier.of(() -> "done"))
                        .join());
        assertInstanceOf(UnsupportedOperationException.class, unsupportedCreateSupplier.getCause());
    }

    @Test
    void commandPeekAndResponseParsingFailureBranchesAreCovered() throws Exception {
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            CapturedRequest request = CapturedRequest.capture(exchange);
            return switch (request.path()) {
                case "/command/collector/peekId" -> JsonResponse.ok(200, Map.of("status", "COMPLETED", "result", List.of("1", "2"), "properties", Map.of()));
                case "/command/collector/peek" -> JsonResponse.ok(200, Map.of("status", "COMPLETED", "result", List.of("bad"), "properties", Map.of()));
                default -> throw new AssertionError("Unexpected path " + request.path());
            };
        })) {
            ConveyorServiceClient client = ConveyorServiceClient.builder(server.baseUri()).build();

            List<String> ids = new ArrayList<>();
            assertTrue(client.<String, Object>command("collector").foreach().peekId(ids::add).get());
            assertEquals(List.of("1", "2"), ids);

            CompletionException badPeek = assertThrows(CompletionException.class,
                    () -> client.<String, Object>command("collector").foreach().peek().join());
            assertInstanceOf(ConveyorServiceException.class, badPeek.getCause());
        }
    }

    @Test
    void sessionAuthenticationFailureIsReported() throws Exception {
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            if (exchange.getRequestURI().getPath().equals("/login")) {
                try {
                    exchange.sendResponseHeaders(401, -1);
                    exchange.close();
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
                return null;
            }
            return JsonResponse.ok(200, Map.of("status", "COMPLETED", "result", true, "properties", Map.of()));
        })) {
            ConveyorServiceClient client = ConveyorServiceClient.builder(server.baseUri())
                    .authentication(ConveyorServiceAuthentication.session("admin", "bad"))
                    .build();

            CompletionException error = assertThrows(CompletionException.class,
                    () -> client.part("collector").id("1").label("USER").value("{}").place().join());
            assertInstanceOf(ConveyorServiceException.class, error.getCause());
            assertEquals(401, ((ConveyorServiceException) error.getCause()).getHttpStatus());
        }
    }

    @Test
    void internalResponseHandlingBranchesAreCovered() throws Exception {
        ConveyorServiceClient client = ConveyorServiceClient.builder(URI.create("http://localhost:8080")).build();

        CompletionException redirect = assertThrows(CompletionException.class,
                () -> invokePrivate(client, "parseResponse", fakeResponse(302, "{}", URI.create("http://localhost/login"))));
        assertInstanceOf(ConveyorServiceException.class, redirect.getCause());

        CompletionException serverError = assertThrows(CompletionException.class,
                () -> invokePrivate(client, "parseResponse", fakeResponse(500, "{}", URI.create("http://localhost"))));
        assertInstanceOf(ConveyorServiceException.class, serverError.getCause());

        CompletionException invalidEnvelope = assertThrows(CompletionException.class,
                () -> invokePrivate(client, "parseResponse", fakeResponse(200, "[]", URI.create("http://localhost"))));
        assertInstanceOf(ConveyorServiceException.class, invalidEnvelope.getCause());

        Object blankResponse = invokePrivateValue(client, "parseResponse", fakeResponse(200, "", URI.create("http://localhost")));
        CompletionException blankFailure = assertThrows(CompletionException.class,
                () -> invokePrivate(client, "resolveBooleanResponse", blankResponse));
        assertInstanceOf(ConveyorServiceException.class, blankFailure.getCause());
        assertNull(((ConveyorServiceException) blankFailure.getCause()).getPlacementStatus());

        assertTrue((Boolean) invokePrivateValue(client, "resolveBooleanResponse", remoteResponse(200, "COMPLETED", null, null, null)));
        CompletionException wrongCompletedType = assertThrows(CompletionException.class,
                () -> invokePrivate(client, "resolveBooleanResponse", remoteResponse(200, "COMPLETED", Map.of("a", 1), null, null)));
        assertInstanceOf(ConveyorServiceException.class, wrongCompletedType.getCause());

        CompletionException timeout = assertThrows(CompletionException.class,
                () -> invokePrivate(client, "resolveBooleanResponse", remoteResponse(200, "TIMEOUT_WAITING_FOR_COMPLETION", null, null, null)));
        assertInstanceOf(ConveyorServiceException.class, timeout.getCause());

        CompletionException failedCommand = assertThrows(CompletionException.class,
                () -> invokePrivate(client, "ensureSuccessfulCommand", remoteResponse(200, "FAILED", null, null, "bad command")));
        assertInstanceOf(ConveyorServiceException.class, failedCommand.getCause());

        @SuppressWarnings("unchecked")
        Map<String, Object> emptyMap = (Map<String, Object>) invokePrivateValue(client, "mapValue", List.of("x"));
        assertEquals(Map.of(), emptyMap);

        CompletionException badFailureBody = assertThrows(CompletionException.class,
                () -> invokePrivate(client, "failureBody", new com.aegisql.conveyor.cart.command.GeneralCommand<>("1", "bad", com.aegisql.conveyor.CommandLabel.COMPLETE_BUILD_EXEPTIONALLY, 0, 0)));
        assertInstanceOf(IllegalArgumentException.class, badFailureBody.getCause());
    }

    @Test
    void conveyorServiceExceptionSupportsCauseConstructor() {
        IllegalStateException cause = new IllegalStateException("boom");
        ConveyorServiceException error = new ConveyorServiceException("message", cause, 409, "FAILED", "{\"x\":1}");

        assertSame(cause, error.getCause());
        assertEquals(409, error.getHttpStatus());
        assertEquals("FAILED", error.getPlacementStatus());
        assertEquals("{\"x\":1}", error.getRawBody());
    }

    private record JsonResponse(int status, String body) {
        private static JsonResponse ok(int status, Map<String, Object> payload) {
            return new JsonResponse(status, SimpleJson.write(payload));
        }
    }

    private static Map<String, Object> payload(String status, Object result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("result", result);
        payload.put("properties", Map.of());
        return payload;
    }

    private record CapturedRequest(String path, Map<String, String> query, Headers headers, String body) {

        private static CapturedRequest capture(HttpExchange exchange) {
            try {
                return new CapturedRequest(
                        exchange.getRequestURI().getPath(),
                        parseQuery(exchange.getRequestURI().getRawQuery()),
                        exchange.getRequestHeaders(),
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
                );
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }

        private String header(String name) {
            return headers.getFirst(name);
        }
    }

    private record FakeHttpResponse(int statusCode, String body, URI uri) implements HttpResponse<String> {
        @Override
        public HttpRequest request() {
            return HttpRequest.newBuilder(uri).GET().build();
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (a, b) -> true);
        }

        @Override
        public URI uri() {
            return uri;
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }
    }

    private static HttpResponse<String> fakeResponse(int statusCode, String body, URI uri) {
        return new FakeHttpResponse(statusCode, body, uri);
    }

    private static Object remoteResponse(int httpStatus, String placementStatus, Object result, String errorCode, String errorMessage) throws Exception {
        Class<?> responseClass = Class.forName("com.aegisql.conveyor.utils.http.ConveyorServiceClient$RemoteResponse");
        Constructor<?> constructor = responseClass.getDeclaredConstructor(
                int.class,
                String.class,
                Object.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                Map.class,
                String.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(httpStatus, placementStatus, result, errorCode, errorMessage, null, null, null, Map.of(), "{}");
    }

    private static void invokePrivate(Object target, String methodName, Object arg) {
        try {
            Method method = findMethod(target.getClass(), methodName, arg.getClass().getInterfaces().length > 0 ? arg.getClass().getInterfaces()[0] : arg.getClass());
            method.setAccessible(true);
            method.invoke(target, arg);
        } catch (InvocationTargetException e) {
            throw new CompletionException(e.getTargetException());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object invokePrivateValue(Object target, String methodName, Object arg) {
        try {
            Method method = findMethod(target.getClass(), methodName, arg.getClass().getInterfaces().length > 0 ? arg.getClass().getInterfaces()[0] : arg.getClass());
            method.setAccessible(true);
            return method.invoke(target, arg);
        } catch (InvocationTargetException e) {
            throw new CompletionException(e.getTargetException());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Method findMethod(Class<?> type, String methodName, Class<?> argType) throws NoSuchMethodException {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                Class<?> parameterType = method.getParameterTypes()[0];
                if (parameterType.isAssignableFrom(argType)) {
                    return method;
                }
            }
        }
        throw new NoSuchMethodException(methodName + "(" + argType.getName() + ")");
    }

    private static final class TestHttpServer implements AutoCloseable {
        private final HttpServer server;
        private final java.util.function.Function<HttpExchange, JsonResponse> handler;
        private final ConcurrentLinkedQueue<String> seenPaths = new ConcurrentLinkedQueue<>();

        private TestHttpServer(java.util.function.Function<HttpExchange, JsonResponse> handler) throws IOException {
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            this.handler = handler;
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", exchange -> {
                seenPaths.add(exchange.getRequestURI().getPath());
                JsonResponse response = handler.apply(exchange);
                if (response == null) {
                    return;
                }
                byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(response.status(), bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });
            server.start();
        }

        private URI baseUri() {
            return URI.create("http://localhost:" + server.getAddress().getPort());
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            query.put(key, value);
        }
        return query;
    }

    private static final class LogCapture extends Handler implements AutoCloseable {
        private final Logger logger;
        private final Level originalLevel;
        private final boolean originalUseParentHandlers;
        private final List<String> messages = new ArrayList<>();

        private LogCapture(String loggerName, Level level) {
            this.logger = Logger.getLogger(loggerName);
            this.originalLevel = logger.getLevel();
            this.originalUseParentHandlers = logger.getUseParentHandlers();
            setLevel(level);
            logger.setUseParentHandlers(false);
            logger.setLevel(level);
            logger.addHandler(this);
        }

        @Override
        public void publish(LogRecord record) {
            if (isLoggable(record)) {
                messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() {
            logger.removeHandler(this);
            logger.setLevel(originalLevel);
            logger.setUseParentHandlers(originalUseParentHandlers);
        }

        private String joinedMessages() {
            return String.join("\n", messages);
        }
    }
}
