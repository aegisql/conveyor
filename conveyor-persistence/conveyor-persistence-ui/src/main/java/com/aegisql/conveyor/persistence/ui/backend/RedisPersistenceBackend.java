package com.aegisql.conveyor.persistence.ui.backend;

import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.redis.RedisPersistenceBuilder;
import com.aegisql.conveyor.persistence.ui.model.ConnectionStatus;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;
import com.aegisql.conveyor.persistence.ui.model.PersistenceSnapshot;
import com.aegisql.conveyor.persistence.ui.model.SummaryEntry;
import com.aegisql.conveyor.persistence.ui.model.TableData;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class RedisPersistenceBackend implements PersistenceBackend {

    private final ConverterAdviser<Object> plainConverterAdviser = new ConverterAdviser<>();
    private final ConverterAdviser<Object> payloadConverterAdviser = new ConverterAdviser<>();

    @Override
    public ConnectionStatus connectionStatus(PersistenceProfile profile) {
        PersistenceProfile normalized = profile.normalized();
        try (JedisPooled jedis = openClient(normalized)) {
            if (!"PONG".equalsIgnoreCase(jedis.ping())) {
                return ConnectionStatus.FAILED;
            }
            return jedis.exists(namespace(normalized) + ":meta")
                    ? ConnectionStatus.READY
                    : ConnectionStatus.CONNECTED_UNINITIALIZED;
        } catch (Exception e) {
            return ConnectionStatus.FAILED;
        }
    }

    @Override
    public PersistenceSnapshot inspect(PersistenceProfile profile, int rowLimit, int pageIndex) {
        PersistenceProfile normalized = profile.normalized();
        int normalizedPageIndex = Math.max(0, pageIndex);
        try (JedisPooled jedis = openClient(normalized)) {
            jedis.ping();
            String namespace = namespace(normalized);
            String metaKey = namespace + ":meta";
            boolean initialized = jedis.exists(metaKey);
            List<SummaryEntry> summary = new ArrayList<>();
            summary.add(new SummaryEntry("Engine", normalized.kind().displayName()));
            summary.add(new SummaryEntry("Redis URI", sanitizeRedisUri(normalized.redisUri())));
            summary.add(new SummaryEntry("Namespace", namespace));
            summary.add(new SummaryEntry("Preview Page", Integer.toString(normalizedPageIndex + 1)));

            if (!initialized) {
                List<TableData> infoTables = availablePersistencesTables(jedis);
                return new PersistenceSnapshot(
                        ConnectionStatus.CONNECTED_UNINITIALIZED,
                        "Connected, persistence not initialized",
                        "Redis is reachable, but this persistence namespace has not been initialized yet. "
                                + "Use the Redis persistence name that created keys like conv:{name}:meta; this is not the runtime conveyor name.",
                        summary,
                        List.of(),
                        infoTables,
                        normalizedPageIndex,
                        normalizedPageIndex > 0,
                        false,
                        true,
                        false,
                        false,
                        false
                );
            }

            long activeCount = jedis.zcard(namespace + ":parts:active");
            long staticCount = jedis.zcard(namespace + ":parts:static");
            long expiringCount = jedis.zcard(namespace + ":parts:expires");
            long completedCount = jedis.scard(namespace + ":completed");
            summary.add(new SummaryEntry("Active Parts", Long.toString(activeCount)));
            summary.add(new SummaryEntry("Static Parts", Long.toString(staticCount)));
            summary.add(new SummaryEntry("Expiring Parts", Long.toString(expiringCount)));
            summary.add(new SummaryEntry("Completed Keys", Long.toString(completedCount)));
            summary.add(new SummaryEntry("Preview Limit", Integer.toString(rowLimit)));
            TablePage activePreview = partsTable(jedis, namespace, "Active Parts", namespace + ":parts:active", activeCount, rowLimit, normalizedPageIndex);
            TablePage staticPreview = partsTable(jedis, namespace, "Static Parts", namespace + ":parts:static", staticCount, rowLimit, normalizedPageIndex);
            TablePage completedPreview = completedKeysTable(jedis, namespace, completedCount, rowLimit, normalizedPageIndex);

            return new PersistenceSnapshot(
                    ConnectionStatus.READY,
                    "Connected and initialized",
                    "Showing Redis namespace metadata and a simple raw preview of stored items.",
                    summary,
                    List.of(
                            activePreview.table(),
                            staticPreview.table(),
                            completedPreview.table()
                    ),
                    List.of(namespaceMetaTable(jedis, namespace)),
                    normalizedPageIndex,
                    normalizedPageIndex > 0,
                    activePreview.hasNext() || staticPreview.hasNext() || completedPreview.hasNext(),
                    false,
                    false,
                    true,
                    true
            );
        } catch (Exception e) {
            return new PersistenceSnapshot(
                    ConnectionStatus.FAILED,
                    "Connection failed",
                    e.getMessage(),
                    List.of(
                            new SummaryEntry("Engine", normalized.kind().displayName()),
                            new SummaryEntry("Redis URI", sanitizeRedisUri(normalized.redisUri()))
                    ),
                    List.of(),
                    List.of(),
                    normalizedPageIndex,
                    normalizedPageIndex > 0,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }
    }

    @Override
    public List<String> lookupDatabases(PersistenceProfile profile) {
        return List.of();
    }

    @Override
    public List<String> lookupSchemas(PersistenceProfile profile) {
        return List.of();
    }

    @Override
    public List<String> lookupPersistenceNames(PersistenceProfile profile) {
        PersistenceProfile normalized = profile.normalized();
        try (JedisPooled jedis = openClient(normalized)) {
            jedis.ping();
            return discoverPersistences(jedis).stream()
                    .map(DiscoveredPersistence::name)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to lookup Redis persistence namespaces", e);
        }
    }

    @Override
    public String initializationScript(PersistenceProfile profile) {
        throw new UnsupportedOperationException("Redis initialization does not use SQL scripts");
    }

    @Override
    public void initialize(PersistenceProfile profile) {
        try (Persistence<Object> ignored = buildPersistence(profile, true)) {
            // autoInit on build creates namespace metadata.
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Redis persistence namespace", e);
        }
    }

    @Override
    public void archiveExpired(PersistenceProfile profile) {
        try (Persistence<Object> persistence = buildPersistence(profile, false)) {
            persistence.archiveExpiredParts();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to archive expired Redis persistence data", e);
        }
    }

    @Override
    public void archiveAll(PersistenceProfile profile) {
        try (Persistence<Object> persistence = buildPersistence(profile, false)) {
            persistence.archiveAll();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to archive all Redis persistence data", e);
        }
    }

    private Persistence<Object> buildPersistence(PersistenceProfile profile, boolean autoInit) {
        PersistenceProfile normalized = profile.normalized();
        try {
            RedisPersistenceBuilder<Object> builder = new RedisPersistenceBuilder<>(normalized.persistenceName())
                    .autoInit(autoInit)
                    .redisUri(normalized.redisUri());
            if (normalized.user() != null) {
                builder = builder.user(normalized.user());
            }
            if (normalized.password() != null) {
                builder = builder.password(normalized.password());
            }
            @SuppressWarnings("unchecked")
            Persistence<Object> persistence = (Persistence<Object>) builder.build();
            return persistence;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build Redis persistence", e);
        }
    }

    private JedisPooled openClient(PersistenceProfile profile) {
        URI uri = URI.create(profile.redisUri());
        HostAndPort hostAndPort = new HostAndPort(uri.getHost(), uri.getPort() > 0 ? uri.getPort() : 6379);
        DefaultJedisClientConfig.Builder clientConfig = DefaultJedisClientConfig.builder(uri);
        if (profile.user() != null) {
            clientConfig.user(profile.user());
        }
        if (profile.password() != null) {
            clientConfig.password(profile.password());
        }
        return new JedisPooled(new GenericObjectPoolConfig<>(), hostAndPort, clientConfig.build());
    }

    private TableData namespaceMetaTable(JedisPooled jedis, String namespace) {
        Map<String, String> meta = jedis.hgetAll(namespace + ":meta");
        List<List<String>> rows = meta.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> List.of(entry.getKey(), truncate(entry.getValue())))
                .toList();
        return new TableData("Namespace Meta", List.of("Field", "Value"), rows);
    }

    private TablePage partsTable(JedisPooled jedis, String namespace, String title, String indexKey, long totalCount, int rowLimit, int pageIndex) {
        int offset = Math.max(0, pageIndex) * Math.max(1, rowLimit);
        List<String> ids = jedis.zrange(indexKey, offset, offset + Math.max(0, rowLimit - 1));
        boolean staticParts = "Static Parts".equals(title);
        List<List<String>> rows = new ArrayList<>();
        for (String id : ids) {
            Map<String, String> meta = jedis.hgetAll(namespace + ":part:" + id + ":meta");
            String payload = jedis.get(namespace + ":part:" + id + ":payload");
            Set<String> additionalFields = meta.keySet().stream()
                    .filter(key -> key.startsWith("field:") && key.endsWith(":hint"))
                    .map(key -> key.substring("field:".length(), key.length() - ":hint".length()))
                    .collect(Collectors.toCollection(java.util.TreeSet::new));
            Object key = decodePlainField(meta.get("keyHint"), meta.get("keyData"));
            Object label = decodePlainField(meta.get("labelHint"), meta.get("labelData"));
            Object value = decodePayloadField(label, meta.get("valueHint"), payload, meta.get("valueData"));
            if (staticParts) {
                rows.add(List.of(
                        id,
                        displayValue(label),
                        valueOf(meta.get("labelHint")),
                        displayValue(value),
                        valueOf(meta.get("valueHint")),
                        valueOf(meta.get("creationTime")),
                        valueOf(meta.get("expirationTime")),
                        valueOf(meta.get("priority")),
                        jedis.exists(namespace + ":part:" + id + ":payload") ? "yes" : "no",
                        additionalFields.isEmpty() ? "" : String.join(", ", additionalFields)
                ));
            } else {
                rows.add(List.of(
                        id,
                        valueOf(meta.get("loadType")),
                        displayValue(key),
                        valueOf(meta.get("keyHint")),
                        displayValue(label),
                        valueOf(meta.get("labelHint")),
                        displayValue(value),
                        valueOf(meta.get("valueHint")),
                        valueOf(meta.get("creationTime")),
                        valueOf(meta.get("expirationTime")),
                        valueOf(meta.get("priority")),
                        jedis.exists(namespace + ":part:" + id + ":payload") ? "yes" : "no",
                        additionalFields.isEmpty() ? "" : String.join(", ", additionalFields)
                ));
            }
        }
        List<String> columns = staticParts
                ? List.of("ID", "Label", "Label Hint", "Value", "Value Hint", "Created", "Expires", "Priority", "Payload", "Additional Fields")
                : List.of("ID", "Load Type", "Key", "Key Hint", "Label", "Label Hint", "Value", "Value Hint", "Created", "Expires", "Priority", "Payload", "Additional Fields");
        return new TablePage(new TableData(title, columns, rows), totalCount > offset + rows.size());
    }

    private TablePage completedKeysTable(JedisPooled jedis, String namespace, long totalCount, int rowLimit, int pageIndex) {
        int offset = Math.max(0, pageIndex) * Math.max(1, rowLimit);
        List<List<String>> rows = jedis.smembers(namespace + ":completed").stream()
                .sorted(Comparator.naturalOrder())
                .skip(offset)
                .limit(Math.max(1, rowLimit))
                .map(key -> List.of(truncate(key)))
                .toList();
        return new TablePage(new TableData("Completed Keys", List.of("Key"), rows), totalCount > offset + rows.size());
    }

    private List<TableData> availablePersistencesTables(JedisPooled jedis) {
        List<List<String>> rows = discoverPersistences(jedis).stream()
                .map(persistence -> List.of(
                        persistence.name(),
                        persistence.namespace(),
                        valueOf(persistence.backend()),
                        valueOf(persistence.version())
                ))
                .toList();
        if (rows.isEmpty()) {
            return List.of();
        }
        return List.of(new TableData(
                "Available Persistences",
                List.of("Persistence Name", "Namespace", "Backend", "Version"),
                rows
        ));
    }

    private List<DiscoveredPersistence> discoverPersistences(JedisPooled jedis) {
        List<DiscoveredPersistence> discovered = new ArrayList<>();
        ScanParams scanParams = new ScanParams().match("conv:{*}:meta").count(500);
        String cursor = ScanParams.SCAN_POINTER_START;
        do {
            ScanResult<String> scan = jedis.scan(cursor, scanParams);
            cursor = scan.getCursor();
            for (String metaKey : scan.getResult()) {
                if (!metaKey.startsWith("conv:{") || !metaKey.endsWith("}:meta")) {
                    continue;
                }
                String namespace = metaKey.substring(0, metaKey.length() - ":meta".length());
                Map<String, String> meta = jedis.hgetAll(metaKey);
                String name = valueOf(meta.get("name"));
                if (name.isBlank()) {
                    name = namespaceName(namespace);
                }
                discovered.add(new DiscoveredPersistence(name, namespace, meta.get("backend"), meta.get("version")));
            }
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
        return discovered.stream()
                .sorted(Comparator.comparing(DiscoveredPersistence::name).thenComparing(DiscoveredPersistence::namespace))
                .toList();
    }

    private String namespaceName(String namespace) {
        if (namespace.startsWith("conv:{") && namespace.endsWith("}")) {
            return namespace.substring("conv:{".length(), namespace.length() - 1);
        }
        return namespace;
    }

    private String namespace(PersistenceProfile profile) {
        return "conv:{" + profile.persistenceName() + '}';
    }

    private String sanitizeRedisUri(String redisUri) {
        URI uri = URI.create(redisUri);
        String scheme = uri.getScheme() == null ? "redis" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "localhost" : uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 6379;
        String path = uri.getPath() == null ? "" : uri.getPath();
        return scheme + "://" + host + ':' + port + path;
    }

    private String valueOf(String value) {
        return value == null ? "" : truncate(value);
    }

    private String displayValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof byte[] bytes) {
            return "<" + bytes.length + " bytes>";
        }
        return truncate(String.valueOf(value));
    }

    private Object decodePayloadField(Object label, String hint, String encodedPayload, String legacyEncodedValue) {
        String encodedValue = encodedPayload != null ? encodedPayload : legacyEncodedValue;
        if (hint == null && encodedValue == null) {
            return null;
        }
        try {
            ObjectConverter<Object, byte[]> converter = payloadConverterAdviser.getConverter(label, hint);
            return converter.fromPersistence(decodeBytes(encodedValue));
        } catch (Exception e) {
            return "<unreadable payload>";
        }
    }

    private Object decodePlainField(String hint, String encodedValue) {
        if (hint == null && encodedValue == null) {
            return null;
        }
        try {
            ObjectConverter<Object, byte[]> converter = plainConverterAdviser.getConverter(null, hint);
            return converter.fromPersistence(decodeBytes(encodedValue));
        } catch (Exception e) {
            String fallback = bestEffortReadablePlainValue(hint, decodeBytes(encodedValue));
            return fallback == null ? "<unreadable>" : fallback;
        }
    }

    private byte[] decodeBytes(String encodedValue) {
        return encodedValue == null ? null : Base64.getUrlDecoder().decode(encodedValue);
    }

    private String bestEffortReadablePlainValue(String hint, byte[] bytes) {
        if (bytes == null || hint == null) {
            return null;
        }
        if (looksLikeEnumHint(hint) && bytes.length >= Integer.BYTES) {
            return enumFallbackValue(hint, bytes);
        }
        if (!"Serializable:byte[]".equals(hint)) {
            return null;
        }
        List<String> tokens = extractPrintableTokens(bytes);
        if (tokens.isEmpty()) {
            return "<serializable>";
        }
        String className = tokens.stream()
                .filter(token -> token.contains(".") || token.contains("$"))
                .findFirst()
                .orElse(null);
        String enumConstant = null;
        for (int i = tokens.size() - 1; i >= 0; i--) {
            String token = tokens.get(i);
            if (token.matches("[A-Z][A-Z0-9_]*")) {
                enumConstant = token;
                break;
            }
        }
        if (className != null && enumConstant != null) {
            return simpleTypeName(className) + "." + enumConstant;
        }
        if (enumConstant != null) {
            return enumConstant;
        }
        if (className != null) {
            return simpleTypeName(className);
        }
        return tokens.getLast();
    }

    private boolean looksLikeEnumHint(String hint) {
        return hint.endsWith(":byte[]")
                && !"Serializable:byte[]".equals(hint)
                && !"String:byte[]".equals(hint)
                && !hint.contains(".");
    }

    private String enumFallbackValue(String hint, byte[] bytes) {
        String textValue = decodePrintableUtf8(bytes);
        if (textValue != null && !textValue.isBlank()) {
            return textValue;
        }
        String enumType = hint.substring(0, hint.indexOf(':'));
        int ordinal = ByteBuffer.wrap(bytes, 0, Integer.BYTES).getInt();
        return enumType + "#" + ordinal;
    }

    private String decodePrintableUtf8(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String decoded = new String(bytes, StandardCharsets.UTF_8);
        if (!Arrays.equals(decoded.getBytes(StandardCharsets.UTF_8), bytes)) {
            return null;
        }
        for (int i = 0; i < decoded.length(); i++) {
            char ch = decoded.charAt(i);
            if (Character.isISOControl(ch)) {
                return null;
            }
        }
        return decoded;
    }

    private List<String> extractPrintableTokens(byte[] bytes) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (byte value : bytes) {
            int ch = value & 0xFF;
            if (ch >= 32 && ch <= 126) {
                current.append((char) ch);
            } else if (current.length() >= 3) {
                tokens.add(current.toString());
                current.setLength(0);
            } else {
                current.setLength(0);
            }
        }
        if (current.length() >= 3) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private String simpleTypeName(String className) {
        String withoutPackage = className.contains(".")
                ? className.substring(className.lastIndexOf('.') + 1)
                : className;
        return withoutPackage.contains("$")
                ? withoutPackage.substring(withoutPackage.lastIndexOf('$') + 1)
                : withoutPackage;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 160 ? value.substring(0, 157) + "..." : value;
    }

    private record DiscoveredPersistence(String name, String namespace, String backend, String version) {
    }

    private record TablePage(TableData table, boolean hasNext) {
    }
}
