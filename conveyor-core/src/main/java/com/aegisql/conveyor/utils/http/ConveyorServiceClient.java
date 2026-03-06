package com.aegisql.conveyor.utils.http;

import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.cart.command.CreateCommand;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.loaders.CommandLoader;
import com.aegisql.conveyor.loaders.MultiKeyCommandLoader;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.loaders.StaticPartLoader;
import com.aegisql.conveyor.serial.SerializablePredicate;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConveyorServiceClient {

    static final String AUDIT_LOGGER_NAME = "conveyor.audit.client";

    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_TIMEOUT = "TIMEOUT_WAITING_FOR_COMPLETION";
    private static final String REDACTED = "REDACTED";
    private static final String PRESENT = "PRESENT";
    private static final Set<String> SAFE_QUERY_KEYS = Set.of(
            "ttl",
            "requestttl",
            "creationtime",
            "expirationtime",
            "priority",
            "delete",
            "watchresults",
            "watchlimit"
    );
    private static final Set<String> SENSITIVE_KEY_MARKERS = Set.of(
            "password",
            "token",
            "secret",
            "cookie",
            "session",
            "authorization",
            "auth"
    );
    private static final Logger LOG = Logger.getLogger(ConveyorServiceClient.class.getName());
    private static final Logger AUDIT_LOG = Logger.getLogger(AUDIT_LOGGER_NAME);

    private final URI baseUri;
    private final CookieManager cookieManager;
    private final HttpClient httpClient;
    private final ConveyorServiceAuthentication authentication;
    private final HttpValueCodec valueCodec;
    private final Duration requestTimeout;

    private ConveyorServiceClient(Builder builder) {
        this.baseUri = normalizeBaseUri(builder.baseUri);
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(builder.connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.authentication = builder.authentication;
        this.valueCodec = builder.valueCodec;
        this.requestTimeout = builder.requestTimeout;
        LOG.fine(() -> "Created ConveyorServiceClient baseUrl=" + this.baseUri
                + " authMode=" + authentication.auditAuthMode()
                + " userId=" + safeUserId(authentication.auditUserId()));
    }

    public static Builder builder(String baseUrl) {
        return builder(URI.create(baseUrl));
    }

    public static Builder builder(URI baseUri) {
        return new Builder(baseUri);
    }

    public <K, L> PartLoader<K, L> part(String conveyorName) {
        Objects.requireNonNull(conveyorName, "Conveyor name must be provided");
        return new PartLoader<>(loader -> placePart(conveyorName, loader));
    }

    public <L> StaticPartLoader<L> staticPart(String conveyorName) {
        Objects.requireNonNull(conveyorName, "Conveyor name must be provided");
        return new StaticPartLoader<>(loader -> placeStaticPart(conveyorName, loader));
    }

    public <K, OUT> CommandLoader<K, OUT> command(String conveyorName) {
        Objects.requireNonNull(conveyorName, "Conveyor name must be provided");
        return new CommandLoader<>(command -> executeCommand(conveyorName, command));
    }

    CompletableFuture<Void> login(String username, String password) {
        String body = formEncode(Map.of(
                "username", username,
                "password", password,
                "remember-me", "true"
        ));
        URI uri = resolveUri("/login", Map.of());
        int bodySize = body.getBytes(StandardCharsets.UTF_8).length;
        LOG.fine(() -> "Starting session login url=" + sanitizeUrl(uri, Map.of())
                + " authMode=session userId=" + safeUserId(username)
                + " bodySize=" + bodySize);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .whenComplete((response, error) -> {
                    if (error != null) {
                        audit("POST", uri, Map.of(), bodySize, -1, username);
                        LOG.warning(() -> "Session login transport failure url=" + sanitizeUrl(uri, Map.of())
                                + " authMode=session userId=" + safeUserId(username)
                                + " error=" + error.getClass().getSimpleName());
                        return;
                    }
                    audit("POST", uri, Map.of(), bodySize, response.statusCode(), username);
                    LOG.fine(() -> "Received session login response url=" + sanitizeUrl(uri, Map.of())
                            + " authMode=session userId=" + safeUserId(username)
                            + " status=" + response.statusCode());
                })
                .thenApply(response -> {
                    if (response.statusCode() != 200 && response.statusCode() != 302) {
                        LOG.warning(() -> "Session login rejected url=" + sanitizeUrl(uri, Map.of())
                                + " authMode=session userId=" + safeUserId(username)
                                + " status=" + response.statusCode());
                        throw new ConveyorServiceException(
                                "Session authentication failed with HTTP " + response.statusCode(),
                                response.statusCode(),
                                null,
                                response.body()
                        );
                    }
                    return null;
                });
    }

    private <K, L> CompletableFuture<Boolean> placePart(String conveyorName, PartLoader<K, L> loader) {
        try {
            validatePartLoader(loader);
            HttpValueCodec.EncodedBody body = valueCodec.encodeBody(loader.partValue);
            Map<String, String> query = queryFromPartLoader(loader);
            String path = loader.filter == null
                    ? "/part/" + pathToken(conveyorName) + "/" + pathToken(loader.key) + "/" + pathToken(loader.label)
                    : "/part/" + pathToken(conveyorName) + "/" + pathToken(loader.label);
            return send(path, query, body).thenApply(this::resolveBooleanResponse);
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private <L> CompletableFuture<Boolean> placeStaticPart(String conveyorName, StaticPartLoader<L> loader) {
        try {
            Objects.requireNonNull(loader.label, "Static part label must be provided");
            Map<String, String> query = queryFromStaticPartLoader(loader);
            HttpValueCodec.EncodedBody body = loader.create
                    ? valueCodec.encodeBody(loader.staticPartValue)
                    : HttpValueCodec.EncodedBody.binary(new byte[0]);
            String path = "/static-part/" + pathToken(conveyorName) + "/" + pathToken(loader.label);
            return send(path, query, body, loader.create).thenApply(this::resolveBooleanResponse);
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private <K, OUT> CompletableFuture<Boolean> executeCommand(String conveyorName, GeneralCommand<K, ?> command) {
        try {
            boolean forEach = command.getKey() == null;
            if (forEach && command.getFilter() != SerializablePredicate.ANY) {
                return CompletableFuture.failedFuture(new UnsupportedOperationException("Remote foreach commands support only the ANY filter"));
            }
            return switch (command.getLabel()) {
                case CANCEL_BUILD -> sendCommand(conveyorName, command, "cancel", null).thenApply(this::resolveBooleanResponse);
                case COMPLETE_BUILD_EXEPTIONALLY -> sendCommand(conveyorName, command, "completeExceptionally", failureBody(command)).thenApply(this::resolveBooleanResponse);
                case PROPERTIES -> sendCommand(conveyorName, command, "addProperties", null).thenApply(this::resolveBooleanResponse);
                case TIMEOUT_BUILD -> sendCommand(conveyorName, command, "timeout", null).thenApply(this::resolveBooleanResponse);
                case RESCHEDULE_BUILD -> sendCommand(conveyorName, command, "reschedule", null).thenApply(this::resolveBooleanResponse);
                case CREATE_BUILD -> executeCreate(conveyorName, command);
                case PEEK_BUILD -> executePeek(command, sendCommand(conveyorName, command, "peek", null));
                case PEEK_KEY -> executePeekId(command, sendCommand(conveyorName, command, "peekId", null));
                default -> CompletableFuture.failedFuture(new UnsupportedOperationException(
                        "Remote conveyor service does not support command " + command.getLabel()));
            };
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private <K> CompletableFuture<Boolean> executeCreate(String conveyorName, GeneralCommand<K, ?> command) {
        if (command instanceof CreateCommand<?, ?> createCommand && createCommand.getValue() != null) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("Remote create(builderSupplier) is not supported"));
        }
        return sendCommand(conveyorName, command, "create", null).thenApply(this::resolveBooleanResponse);
    }

    @SuppressWarnings("unchecked")
    private <K, OUT> CompletableFuture<Boolean> executePeek(GeneralCommand<K, ?> command, CompletableFuture<RemoteResponse> responseFuture) {
        return responseFuture.thenApply(response -> {
            ensureSuccessfulCommand(response);
            Consumer<ProductBin<K, OUT>> consumer = (Consumer<ProductBin<K, OUT>>) command.getValue();
            if (command.getKey() != null) {
                consumer.accept(syntheticProductBin(command.getKey(), (OUT) response.result()));
                return Boolean.TRUE;
            }
            if (!(response.result() instanceof Map<?, ?> resultMap)) {
                throw failure(response, "Expected JSON object result for foreach peek");
            }
            for (Map.Entry<?, ?> entry : resultMap.entrySet()) {
                consumer.accept(syntheticProductBin((K) entry.getKey(), (OUT) entry.getValue()));
            }
            return Boolean.TRUE;
        });
    }

    @SuppressWarnings("unchecked")
    private <K> CompletableFuture<Boolean> executePeekId(GeneralCommand<K, ?> command, CompletableFuture<RemoteResponse> responseFuture) {
        return responseFuture.thenApply(response -> {
            ensureSuccessfulCommand(response);
            Consumer<K> consumer = (Consumer<K>) command.getValue();
            Object result = response.result();
            if (result instanceof List<?> list) {
                for (Object value : list) {
                    consumer.accept((K) value);
                }
            } else {
                consumer.accept((K) result);
            }
            return Boolean.TRUE;
        });
    }

    private <K> CompletableFuture<RemoteResponse> sendCommand(
            String conveyorName,
            GeneralCommand<K, ?> command,
            String operation,
            HttpValueCodec.EncodedBody body
    ) {
        Map<String, String> query = queryFromCommand(command);
        String path = command.getKey() == null
                ? "/command/" + pathToken(conveyorName) + "/" + operation
                : "/command/" + pathToken(conveyorName) + "/" + pathToken(command.getKey()) + "/" + operation;
        return send(path, query, body, body != null);
    }

    private CompletableFuture<RemoteResponse> send(String path, Map<String, String> query, HttpValueCodec.EncodedBody body) {
        return send(path, query, body, true);
    }

    private CompletableFuture<RemoteResponse> send(String path, Map<String, String> query, HttpValueCodec.EncodedBody body, boolean withBody) {
        URI uri = resolveUri(path, query);
        int bodySize = withBody ? body.body().length : -1;
        return authentication.prepare(this)
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        LOG.warning(() -> "Authentication preparation failed url=" + sanitizeUrl(uri, query)
                                + " authMode=" + authentication.auditAuthMode()
                                + " userId=" + safeUserId(authentication.auditUserId())
                                + " error=" + error.getClass().getSimpleName());
                    }
                })
                .thenCompose(ignored -> {
            LOG.fine(() -> "Sending request method=POST url=" + sanitizeUrl(uri, query)
                    + " authMode=" + authentication.auditAuthMode()
                    + " userId=" + safeUserId(authentication.auditUserId())
                    + " bodySize=" + bodySize);
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(requestTimeout);
            authentication.apply(builder);
            if (withBody) {
                builder.header("Content-Type", body.contentType());
                builder.POST(HttpRequest.BodyPublishers.ofByteArray(body.body()));
            } else {
                builder.POST(HttpRequest.BodyPublishers.noBody());
            }
            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .whenComplete((response, error) -> {
                        if (error != null) {
                            audit("POST", uri, query, bodySize, -1, authentication.auditUserId());
                            LOG.warning(() -> "Request transport failure method=POST url=" + sanitizeUrl(uri, query)
                                    + " authMode=" + authentication.auditAuthMode()
                                    + " userId=" + safeUserId(authentication.auditUserId())
                                    + " error=" + error.getClass().getSimpleName());
                            return;
                        }
                        audit("POST", uri, query, bodySize, response.statusCode(), authentication.auditUserId());
                        LOG.fine(() -> "Received response method=POST url=" + sanitizeUrl(uri, query)
                                + " authMode=" + authentication.auditAuthMode()
                                + " userId=" + safeUserId(authentication.auditUserId())
                                + " status=" + response.statusCode());
                    })
                    .thenApply(response -> {
                        try {
                            RemoteResponse remoteResponse = parseResponse(response);
                            LOG.fine(() -> "Parsed response method=POST url=" + sanitizeUrl(uri, query)
                                    + " authMode=" + authentication.auditAuthMode()
                                    + " userId=" + safeUserId(authentication.auditUserId())
                                    + " httpStatus=" + response.statusCode()
                                    + " placementStatus=" + remoteResponse.placementStatus());
                            return remoteResponse;
                        } catch (RuntimeException ex) {
                            LOG.warning(() -> "Request processing failed method=POST url=" + sanitizeUrl(uri, query)
                                    + " authMode=" + authentication.auditAuthMode()
                                    + " userId=" + safeUserId(authentication.auditUserId())
                                    + " httpStatus=" + response.statusCode()
                                    + " error=" + ex.getClass().getSimpleName());
                            throw ex;
                        }
                    });
        });
    }

    private RemoteResponse parseResponse(HttpResponse<String> response) {
        String body = response.body() == null ? "" : response.body();
        if (response.statusCode() == 302) {
            throw new ConveyorServiceException("Authentication failed or interactive login is required", 302, null, body);
        }
        if (response.statusCode() >= 400) {
            throw new ConveyorServiceException("HTTP " + response.statusCode() + " from conveyor service", response.statusCode(), null, body);
        }
        if (body.isBlank()) {
            return new RemoteResponse(response.statusCode(), null, null, null, null, null, null, null, Map.of(), body);
        }
        Object parsed = SimpleJson.parse(body);
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new ConveyorServiceException("Expected JSON object response from conveyor service", response.statusCode(), null, body);
        }
        return new RemoteResponse(
                response.statusCode(),
                stringValue(map.get("status")),
                map.get("result"),
                stringValue(map.get("errorCode")),
                stringValue(map.get("errorMessage")),
                stringValue(map.get("exceptionClass")),
                stringValue(map.get("correlationId")),
                stringValue(map.get("label")),
                mapValue(map.get("properties")),
                body
        );
    }

    private boolean resolveBooleanResponse(RemoteResponse response) {
        if (STATUS_ACCEPTED.equals(response.placementStatus()) || STATUS_IN_PROGRESS.equals(response.placementStatus())) {
            return true;
        }
        if (STATUS_COMPLETED.equals(response.placementStatus())) {
            if (response.result() == null) {
                return true;
            }
            if (response.result() instanceof Boolean value) {
                return value;
            }
            throw failure(response, "Expected boolean result but received " + response.result().getClass().getSimpleName());
        }
        if (STATUS_TIMEOUT.equals(response.placementStatus())) {
            throw failure(response, "Request timed out while waiting for remote completion");
        }
        throw failure(response, "Remote request failed");
    }

    private void ensureSuccessfulCommand(RemoteResponse response) {
        if (STATUS_ACCEPTED.equals(response.placementStatus()) || STATUS_IN_PROGRESS.equals(response.placementStatus()) || STATUS_COMPLETED.equals(response.placementStatus())) {
            return;
        }
        throw failure(response, "Remote command failed");
    }

    private ConveyorServiceException failure(RemoteResponse response, String defaultMessage) {
        String message = response.errorMessage() != null && !response.errorMessage().isBlank()
                ? response.errorMessage()
                : defaultMessage + (response.placementStatus() == null ? "" : " [" + response.placementStatus() + "]");
        return new ConveyorServiceException(message, response.httpStatus(), response.placementStatus(), response.rawBody());
    }

    private <K, OUT> ProductBin<K, OUT> syntheticProductBin(K key, OUT product) {
        return new ProductBin<>(null, key, product, 0, null, Map.of(), null);
    }

    private <K, L> void validatePartLoader(PartLoader<K, L> loader) {
        Objects.requireNonNull(loader.label, "Part label must be provided");
        Objects.requireNonNull(loader.partValue, "Part value must be provided");
        if (loader.filter != null && loader.filter != SerializablePredicate.ANY) {
            throw new UnsupportedOperationException("Remote foreach part placement supports only the ANY filter");
        }
        if (loader.filter == null && loader.key == null) {
            throw new IllegalArgumentException("Part key must be provided for non-foreach placement");
        }
    }

    private <K, L> Map<String, String> queryFromPartLoader(PartLoader<K, L> loader) {
        Map<String, String> query = encodeProperties(loader.getAllProperties());
        if (loader.ttlMsec > 0) {
            query.put("ttl", Long.toString(loader.ttlMsec));
        }
        if (loader.creationTime > 0) {
            query.put("creationTime", Long.toString(loader.creationTime));
        }
        if (loader.expirationTime > 0) {
            query.put("expirationTime", Long.toString(loader.expirationTime));
        }
        if (loader.priority != 0) {
            query.put("priority", Long.toString(loader.priority));
        }
        return query;
    }

    private <L> Map<String, String> queryFromStaticPartLoader(StaticPartLoader<L> loader) {
        Objects.requireNonNull(loader.label, "Static part label must be provided");
        if (loader.create && loader.staticPartValue == null) {
            throw new IllegalArgumentException("Static part value must be provided unless delete() is used");
        }
        Map<String, String> query = encodeProperties(loader.getAllProperties());
        if (!loader.create) {
            query.put("delete", "true");
        }
        if (loader.priority != 0) {
            query.put("priority", Long.toString(loader.priority));
        }
        return query;
    }

    private <K> Map<String, String> queryFromCommand(GeneralCommand<K, ?> command) {
        Map<String, String> query = encodeProperties(command.getAllProperties());
        if (command.getCreationTime() > 0) {
            query.put("creationTime", Long.toString(command.getCreationTime()));
        }
        if (command.getExpirationTime() > 0) {
            query.put("expirationTime", Long.toString(command.getExpirationTime()));
            long ttl = command.getExpirationTime() - command.getCreationTime();
            if (ttl > 0) {
                query.put("ttl", Long.toString(ttl));
            }
        }
        return query;
    }

    private Map<String, String> encodeProperties(Map<String, Object> properties) {
        Map<String, String> encoded = new LinkedHashMap<>();
        properties.forEach((key, value) -> {
            if (value != null) {
                encoded.put(key, valueCodec.encodeQueryValue(value));
            }
        });
        return encoded;
    }

    private HttpValueCodec.EncodedBody failureBody(GeneralCommand<?, ?> command) {
        Object value = command.getValue();
        if (!(value instanceof Throwable throwable)) {
            throw new IllegalArgumentException("completeExceptionally requires a Throwable value");
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.toString();
        }
        return HttpValueCodec.EncodedBody.text(message);
    }

    private URI resolveUri(String path, Map<String, String> query) {
        StringBuilder builder = new StringBuilder(baseUri.toString());
        if (builder.charAt(builder.length() - 1) == '/' && path.startsWith("/")) {
            builder.setLength(builder.length() - 1);
        }
        builder.append(path);
        if (!query.isEmpty()) {
            builder.append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (!first) {
                    builder.append('&');
                }
                builder.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
                first = false;
            }
        }
        return URI.create(builder.toString());
    }

    private static URI normalizeBaseUri(URI baseUri) {
        Objects.requireNonNull(baseUri, "Base URI must be provided");
        String normalized = baseUri.toString();
        if (normalized.endsWith("/")) {
            return baseUri;
        }
        return URI.create(normalized + "/");
    }

    private static String formEncode(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                builder.append('&');
            }
            builder.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
            first = false;
        }
        return builder.toString();
    }

    private static String pathToken(Object value) {
        return urlEncode(String.valueOf(value));
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, entryValue) -> copy.put(String.valueOf(key), entryValue));
            return Map.copyOf(copy);
        }
        return Map.of();
    }

    private void audit(String method, URI uri, Map<String, String> query, int bodySize, int status, String userId) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", Instant.now().toString());
        entry.put("userId", userId);
        entry.put("method", method);
        entry.put("url", sanitizeUrl(uri, query));
        entry.put("parameters", auditParameters(uri.getPath(), query));
        entry.put("bodySize", bodySize);
        entry.put("status", status);
        AUDIT_LOG.info(SimpleJson.write(entry));
    }

    private Map<String, Object> auditParameters(String path, Map<String, String> query) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        List<String> segments = pathSegments(path);
        if (!segments.isEmpty()) {
            switch (segments.getFirst()) {
                case "part" -> {
                    if (segments.size() >= 3) {
                        parameters.put("conveyor", decodePathSegment(segments.get(1)));
                        if (segments.size() == 4) {
                            parameters.put("id", decodePathSegment(segments.get(2)));
                            parameters.put("label", decodePathSegment(segments.get(3)));
                        } else {
                            parameters.put("label", decodePathSegment(segments.get(2)));
                            parameters.put("foreach", true);
                        }
                    }
                }
                case "static-part" -> {
                    if (segments.size() >= 3) {
                        parameters.put("conveyor", decodePathSegment(segments.get(1)));
                        parameters.put("label", decodePathSegment(segments.get(2)));
                    }
                }
                case "command" -> {
                    if (segments.size() >= 3) {
                        parameters.put("conveyor", decodePathSegment(segments.get(1)));
                        if (segments.size() == 4) {
                            parameters.put("id", decodePathSegment(segments.get(2)));
                            parameters.put("command", decodePathSegment(segments.get(3)));
                        } else {
                            parameters.put("command", decodePathSegment(segments.get(2)));
                            parameters.put("foreach", true);
                        }
                    }
                }
                default -> {
                    // keep parameters empty for unstructured endpoints such as /login
                }
            }
        }
        if (!query.isEmpty()) {
            parameters.put("query", sanitizeQuery(query));
        }
        return parameters;
    }

    private String sanitizeUrl(URI uri, Map<String, String> query) {
        StringBuilder builder = new StringBuilder();
        builder.append(uri.getScheme()).append("://").append(uri.getAuthority()).append(uri.getPath());
        Map<String, String> safeQuery = sanitizeQuery(query);
        if (!safeQuery.isEmpty()) {
            builder.append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : safeQuery.entrySet()) {
                if (!first) {
                    builder.append('&');
                }
                builder.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
                first = false;
            }
        }
        return builder.toString();
    }

    private Map<String, String> sanitizeQuery(Map<String, String> query) {
        Map<String, String> safe = new LinkedHashMap<>();
        query.forEach((key, value) -> safe.put(key, sanitizeQueryValue(key, value)));
        return safe;
    }

    private String sanitizeQueryValue(String key, String value) {
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        if (SAFE_QUERY_KEYS.contains(normalizedKey)) {
            return value;
        }
        for (String marker : SENSITIVE_KEY_MARKERS) {
            if (normalizedKey.contains(marker)) {
                return REDACTED;
            }
        }
        return PRESENT;
    }

    private List<String> pathSegments(String path) {
        if (path == null || path.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(path.split("/"))
                .filter(segment -> !segment.isBlank())
                .toList();
    }

    private String decodePathSegment(String segment) {
        return java.net.URLDecoder.decode(segment, StandardCharsets.UTF_8);
    }

    private static String safeUserId(String userId) {
        return userId == null || userId.isBlank() ? "anonymous" : userId;
    }

    private record RemoteResponse(
            int httpStatus,
            String placementStatus,
            Object result,
            String errorCode,
            String errorMessage,
            String exceptionClass,
            String correlationId,
            String label,
            Map<String, Object> properties,
            String rawBody
    ) {
    }

    public static final class Builder {

        private final URI baseUri;
        private ConveyorServiceAuthentication authentication = ConveyorServiceAuthentication.none();
        private HttpValueCodec valueCodec = new DefaultHttpValueCodec();
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofSeconds(30);

        private Builder(URI baseUri) {
            this.baseUri = baseUri;
        }

        public Builder authentication(ConveyorServiceAuthentication authentication) {
            this.authentication = Objects.requireNonNull(authentication, "Authentication strategy must be provided");
            return this;
        }

        public Builder valueCodec(HttpValueCodec valueCodec) {
            this.valueCodec = Objects.requireNonNull(valueCodec, "Value codec must be provided");
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout, "Connect timeout must be provided");
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout, "Request timeout must be provided");
            return this;
        }

        public ConveyorServiceClient build() {
            return new ConveyorServiceClient(this);
        }
    }
}
