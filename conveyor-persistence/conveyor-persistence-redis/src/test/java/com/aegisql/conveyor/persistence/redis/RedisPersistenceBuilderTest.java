package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.archive.ArchiveStrategy;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.JedisPooled;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisPersistenceBuilderTest extends RedisTestSupport {

    @Test
    void validatesBuilderArgumentsAndSupportsLazyInitialization() throws Exception {
        assertThrows(NullPointerException.class, () -> new RedisPersistenceBuilder<>(null));

        String name = testNamespace("builder-validation");
        RedisPersistenceBuilder<Integer> builder = new RedisPersistenceBuilder<Integer>(name);
        assertEquals(RestoreOrder.BY_ID, builder.restoreOrder());
        assertEquals(ArchiveStrategy.DELETE, builder.archiveStrategy());
        assertThrows(NullPointerException.class, () -> builder.redisUri(null));
        assertThrows(NullPointerException.class, () -> builder.jedis(null));
        assertThrows(NullPointerException.class, () -> builder.nonPersistentProperty(null));
        assertThrows(NullPointerException.class, () -> builder.persistentPartFilter(null));
        assertThrows(NullPointerException.class, () -> builder.restoreOrder(null));
        assertThrows(NullPointerException.class, () -> builder.clientName(null));
        assertThrows(NullPointerException.class, () -> builder.user(null));
        assertThrows(NullPointerException.class, () -> builder.password(null));
        assertThrows(IllegalArgumentException.class, () -> builder.maxTotal(0));
        assertThrows(IllegalArgumentException.class, () -> builder.maxIdle(-1));
        assertThrows(IllegalArgumentException.class, () -> builder.minIdle(-1));
        assertThrows(IllegalArgumentException.class, () -> builder.connectionTimeoutMillis(0));
        assertThrows(IllegalArgumentException.class, () -> builder.socketTimeoutMillis(0));
        assertThrows(IllegalArgumentException.class, () -> builder.blockingSocketTimeoutMillis(0));
        assertThrows(IllegalArgumentException.class, () -> builder.database(-1));
        assertEquals(RestoreOrder.BY_PRIORITY_AND_ID, builder.restoreOrder(RestoreOrder.BY_PRIORITY_AND_ID).restoreOrder());
        assertEquals(ArchiveStrategy.NO_ACTION, builder.noArchiving().archiveStrategy());

        String namespaceMetaKey = namespaceMetaKey(name);
        try (JedisPooled jedis = openRedis()) {
            jedis.del(namespaceMetaKey);
        }

        try (Persistence<Integer> persistence = builder.autoInit(false).build();
             JedisPooled jedis = openRedis()) {
            assertTrue(jedis.hgetAll(namespaceMetaKey).isEmpty(),
                    "autoInit(false) should not bootstrap the namespace before the first persistence operation");
            persistence.archiveAll();
            assertEquals(1L, persistence.nextUniquePartId());
            assertEquals(
                    Map.of("backend", RedisPersistence.BACKEND_NAME, "version", RedisPersistence.BACKEND_VERSION, "name", name),
                    jedis.hgetAll(namespaceMetaKey),
                    "The first persistence operation should lazily bootstrap compatible namespace metadata"
            );
        }
    }

    @Test
    void acceptsPreBootstrappedNamespaceMetadataAndRejectsIncompatibleMetadata() throws Exception {
        openRedis().close();

        String validName = testNamespace("builder-bootstrap-valid");
        String validMetaKey = namespaceMetaKey(validName);
        try (JedisPooled jedis = openRedis()) {
            jedis.hset(validMetaKey, Map.of(
                    "backend", RedisPersistence.BACKEND_NAME,
                    "version", RedisPersistence.BACKEND_VERSION,
                    "name", validName,
                    "marker", "keep-me"
            ));
        }

        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(validName).autoInit(true).build();
             JedisPooled jedis = openRedis()) {
            assertEquals(1L, persistence.nextUniquePartId());
            assertEquals("keep-me", jedis.hget(validMetaKey, "marker"),
                    "Valid namespace bootstrap metadata should be validated, not overwritten");
        }

        String wrongBackendName = testNamespace("builder-bootstrap-wrong-backend");
        try (JedisPooled jedis = openRedis()) {
            jedis.hset(namespaceMetaKey(wrongBackendName), Map.of(
                    "backend", "jdbc",
                    "version", RedisPersistence.BACKEND_VERSION,
                    "name", wrongBackendName
            ));
        }
        PersistenceException wrongBackendError = assertThrows(
                PersistenceException.class,
                () -> new RedisPersistenceBuilder<Integer>(wrongBackendName).autoInit(true).build()
        );
        assertTrue(wrongBackendError.getMessage().contains("belongs to backend"),
                "Unexpected error for incompatible backend metadata: " + wrongBackendError.getMessage());

        String wrongVersionName = testNamespace("builder-bootstrap-wrong-version");
        try (JedisPooled jedis = openRedis()) {
            jedis.hset(namespaceMetaKey(wrongVersionName), Map.of(
                    "backend", RedisPersistence.BACKEND_NAME,
                    "version", "999",
                    "name", wrongVersionName
            ));
        }
        PersistenceException wrongVersionError = assertThrows(
                PersistenceException.class,
                () -> new RedisPersistenceBuilder<Integer>(wrongVersionName).autoInit(true).build()
        );
        assertTrue(wrongVersionError.getMessage().contains("uses version"),
                "Unexpected error for incompatible version metadata: " + wrongVersionError.getMessage());

        String wrongName = testNamespace("builder-bootstrap-wrong-name");
        try (JedisPooled jedis = openRedis()) {
            jedis.hset(namespaceMetaKey(wrongName), Map.of(
                    "backend", RedisPersistence.BACKEND_NAME,
                    "version", RedisPersistence.BACKEND_VERSION,
                    "name", "some-other-name"
            ));
        }
        PersistenceException wrongNameError = assertThrows(
                PersistenceException.class,
                () -> new RedisPersistenceBuilder<Integer>(wrongName).autoInit(true).build()
        );
        assertTrue(wrongNameError.getMessage().contains("is bound to name"),
                "Unexpected error for incompatible namespace name metadata: " + wrongNameError.getMessage());

        String incompleteName = testNamespace("builder-bootstrap-incomplete");
        try (JedisPooled jedis = openRedis()) {
            jedis.hset(namespaceMetaKey(incompleteName), Map.of(
                    "backend", RedisPersistence.BACKEND_NAME,
                    "name", incompleteName
            ));
        }
        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(incompleteName).autoInit(false).build()) {
            PersistenceException incompleteError = assertThrows(PersistenceException.class, persistence::nextUniquePartId);
            assertTrue(incompleteError.getMessage().contains("metadata is incomplete"),
                    "Unexpected error for incomplete namespace metadata: " + incompleteError.getMessage());
        }
    }

    @Test
    void handlesNoOpBranchesAndNonPersistentFilter() throws Exception {
        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(testNamespace("builder-noops"))
                .persistentPartFilter(cart -> false)
                .build()) {
            persistence.archiveAll();

            ShoppingCart<Integer, String, String> cart = new ShoppingCart<>(1, "value", "label");
            assertFalse(persistence.isPartPersistent(cart));

            persistence.savePart(1L, cart);
            assertNull(persistence.getPart(1L));
            assertEquals(0L, persistence.getNumberOfParts());

            persistence.savePartId(null, 1L);
            persistence.saveCompletedBuildKey(null);
            assertTrue(persistence.getAllPartIds(null).isEmpty());

            assertDoesNotThrow(() -> persistence.archiveParts(null));
            assertDoesNotThrow(() -> persistence.archiveParts(java.util.List.of()));
            assertDoesNotThrow(() -> persistence.archiveKeys(null));
            assertDoesNotThrow(() -> persistence.archiveKeys(Arrays.asList(null, 1)));
            assertDoesNotThrow(() -> persistence.archiveCompleteKeys(null));
            assertDoesNotThrow(() -> persistence.archiveCompleteKeys(java.util.List.of()));
            assertTrue(persistence.toString().contains("RedisPersistence["));
        }
    }

    @Test
    void rejectsNonSerializableKeysThroughPublicApi() throws Exception {
        openRedis().close();

        try (Persistence<Object> persistence = new RedisPersistenceBuilder<Object>(testNamespace("builder-nonserializable")).build()) {
            PersistenceException completedKeyError =
                    assertThrows(PersistenceException.class, () -> persistence.saveCompletedBuildKey(new Object()));
            assertTrue(completedKeyError.getMessage().contains("Value is not serializable"));

            PersistenceException partIdError =
                    assertThrows(PersistenceException.class, () -> persistence.savePartId(new Object(), 1L));
            assertTrue(partIdError.getMessage().contains("Value is not serializable"));

            PersistenceException getIdsError =
                    assertThrows(PersistenceException.class, () -> persistence.getAllPartIds(new Object()));
            assertTrue(getIdsError.getMessage().contains("Value is not serializable"));

            PersistenceException archiveKeysError =
                    assertThrows(PersistenceException.class, () -> persistence.archiveKeys(List.of(new Object())));
            assertTrue(archiveKeysError.getMessage().contains("Value is not serializable"));
        }
    }

    @Test
    void rejectsNullInInternalRedisEncodingHelper() throws Exception {
        openRedis().close();

        try (Persistence<Integer> rawPersistence = new RedisPersistenceBuilder<Integer>(testNamespace("builder-null-encoding")).build()) {
            RedisPersistence<Integer> persistence = (RedisPersistence<Integer>) rawPersistence;
            Method encodeSerializable = RedisPersistence.class.getDeclaredMethod("encodeSerializable", Object.class);
            encodeSerializable.setAccessible(true);

            InvocationTargetException error = assertThrows(
                    InvocationTargetException.class,
                    () -> encodeSerializable.invoke(persistence, new Object[]{null})
            );

            Throwable cause = error.getCause();
            assertInstanceOf(PersistenceException.class, cause);
            assertEquals("Redis persistence cannot encode null values as indexed keys", cause.getMessage());
        }
    }

    @Test
    void copySharesOwnedRedisClientAndRemainsUsableAfterOriginalClose() throws Exception {
        openRedis().close();

        RedisPersistence<Integer> original =
                (RedisPersistence<Integer>) new RedisPersistenceBuilder<Integer>(testNamespace("builder-shared-copy")).build();
        original.archiveAll();

        RedisPersistence<Integer> copy = (RedisPersistence<Integer>) original.copy();

        try {
            assertSame(readClientHandle(original), readClientHandle(copy));

            original.close();

            assertEquals(1L, copy.nextUniquePartId());
        } finally {
            copy.close();
        }
    }

    @Test
    void externalJedisClientIsNotClosedWhenPersistenceCloses() throws Exception {
        openRedis().close();

        JedisPooled external = RedisConnectionFactory.openDefault();
        try {
            RedisPersistenceBuilder<Integer> builder = new RedisPersistenceBuilder<Integer>(testNamespace("builder-external-jedis"))
                    .jedis(external);

            try (Persistence<Integer> persistence = builder.build()) {
                persistence.archiveAll();
                assertEquals(1L, persistence.nextUniquePartId());
            }

            assertEquals("PONG", external.ping());
        } finally {
            external.close();
        }
    }

    @Test
    void capturesExplicitPoolAndClientSettingsInBuilderConnectionSettings() {
        RedisPersistenceBuilder<Integer> builder = new RedisPersistenceBuilder<Integer>(testNamespace("builder-config-surface"))
                .maxTotal(12)
                .maxIdle(7)
                .minIdle(3)
                .connectionTimeoutMillis(1_100)
                .socketTimeoutMillis(2_200)
                .blockingSocketTimeoutMillis(3_300)
                .database(5)
                .clientName("builder-client")
                .user("builder-user")
                .password("builder-pass")
                .ssl(true);

        RedisConnectionSettings settings = builder.connectionSettings();
        ConnectionPoolConfig poolConfig = RedisConnectionFactory.createPoolConfig(settings);
        DefaultJedisClientConfig clientConfig = RedisConnectionFactory.createClientConfig(settings);

        assertEquals(12, settings.maxTotal());
        assertEquals(7, settings.maxIdle());
        assertEquals(3, settings.minIdle());
        assertEquals(1_100, settings.connectionTimeoutMillis());
        assertEquals(2_200, settings.socketTimeoutMillis());
        assertEquals(3_300, settings.blockingSocketTimeoutMillis());
        assertEquals(5, settings.database());
        assertEquals("builder-client", settings.clientName());
        assertEquals("builder-user", settings.user());
        assertEquals("builder-pass", settings.password());
        assertTrue(settings.ssl());

        assertEquals(12, poolConfig.getMaxTotal());
        assertEquals(7, poolConfig.getMaxIdle());
        assertEquals(3, poolConfig.getMinIdle());

        assertEquals(1_100, clientConfig.getConnectionTimeoutMillis());
        assertEquals(2_200, clientConfig.getSocketTimeoutMillis());
        assertEquals(3_300, clientConfig.getBlockingSocketTimeoutMillis());
        assertEquals(5, clientConfig.getDatabase());
        assertEquals("builder-client", clientConfig.getClientName());
        assertEquals("builder-user", clientConfig.getUser());
        assertEquals("builder-pass", clientConfig.getPassword());
        assertTrue(clientConfig.isSsl());
    }

    @Test
    void capturesArchiveAndEncryptionBuilderSurface() throws Exception {
        String name = testNamespace("builder-surface");
        SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();
        BinaryLogConfiguration binaryLogConfiguration = BinaryLogConfiguration.builder()
                .path("target/redis-builder-surface")
                .moveToPath("target/redis-builder-surface")
                .partTableName("builder")
                .build();
        RecordingArchiver customArchiver = new RecordingArchiver();

        try (Persistence<Integer> archivePersistence = new RedisPersistenceBuilder<Integer>(testNamespace("builder-archive-target")).build()) {
            RedisPersistenceBuilder<Integer> base = new RedisPersistenceBuilder<Integer>(name);
            RedisPersistenceBuilder<Integer> configured = base
                    .minCompactSize(7)
                    .maxArchiveBatchSize(17)
                    .maxArchiveBatchTime(19_000L)
                    .nonPersistentProperty("TMP")
                    .deleteArchiving();

            assertEquals(7, configured.minCompactSize());
            assertEquals(17, configured.maxArchiveBatchSize());
            assertEquals(19_000L, configured.maxArchiveBatchTime());
            assertTrue(configured.nonPersistentProperties().contains("TMP"));
            assertEquals(ArchiveStrategy.DELETE, configured.archiveStrategy());

            assertNotNull(base.encryptionSecret("builder-secret").encryptionBuilder().get());
            assertNotNull(base.encryptionSecret(secretKey).encryptionBuilder().get());
            assertNotNull(base.encryptionAlgorithm("AES")
                    .encryptionTransformation("AES/ECB/PKCS5Padding")
                    .encryptionKeyLength(16)
                    .encryptionSecret("legacy-secret")
                    .encryptionBuilder()
                    .get());

            assertThrows(NullPointerException.class, () -> base.archiver((Persistence<Integer>) null));
            assertThrows(NullPointerException.class, () -> base.archiver((BinaryLogConfiguration) null));
            assertThrows(NullPointerException.class, () -> base.archiver((Archiver<Integer>) null));

            assertEquals(ArchiveStrategy.MOVE_TO_PERSISTENCE, base.archiver(archivePersistence).archiveStrategy());
            assertEquals(ArchiveStrategy.MOVE_TO_FILE, base.archiver(binaryLogConfiguration).archiveStrategy());
            assertEquals(ArchiveStrategy.CUSTOM, base.archiver(customArchiver).archiveStrategy());
            assertEquals(ArchiveStrategy.DELETE, base.noArchiving().deleteArchiving().archiveStrategy());
        }
    }

    @Test
    void ownedBuilderConnectionsHonorLivePoolTuning() throws Exception {
        openRedis().close();

        try (Persistence<Integer> rawPersistence = new RedisPersistenceBuilder<Integer>(testNamespace("builder-live-settings"))
                .maxTotal(5)
                .maxIdle(3)
                .minIdle(1)
                .connectionTimeoutMillis(2_000)
                .socketTimeoutMillis(2_500)
                .blockingSocketTimeoutMillis(3_000)
                .clientName("redis-builder-live")
                .database(0)
                .ssl(false)
                .build()) {
            rawPersistence.archiveAll();
            assertEquals(1L, rawPersistence.nextUniquePartId());

            RedisPersistence<Integer> persistence = (RedisPersistence<Integer>) rawPersistence;
            JedisPooled jedis = readJedis(persistence);
            assertEquals(5, jedis.getPool().getMaxTotal());
            assertEquals(3, jedis.getPool().getMaxIdle());
            assertEquals(1, jedis.getPool().getMinIdle());
            Assumptions.assumeTrue("PONG".equals(jedis.ping()));
        }
    }

    @Test
    void usesCustomArchiverWhenConfigured() throws Exception {
        RecordingArchiver customArchiver = new RecordingArchiver();

        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(testNamespace("builder-custom-archiver"))
                .archiver(customArchiver)
                .build()) {
            persistence.archiveParts(List.of(1L, 2L));
            persistence.archiveKeys(List.of(10));
            persistence.archiveCompleteKeys(List.of(20));
            persistence.archiveExpiredParts();
            persistence.archiveAll();
        }

        assertEquals(List.of("parts", "keys", "complete", "expired", "all"), customArchiver.calls);
        assertNotNull(customArchiver.persistence);
    }

    private static Object readClientHandle(RedisPersistence<?> persistence) throws Exception {
        var field = RedisPersistence.class.getDeclaredField("clientHandle");
        field.setAccessible(true);
        return field.get(persistence);
    }

    private static JedisPooled readJedis(RedisPersistence<?> persistence) throws Exception {
        var field = RedisPersistence.class.getDeclaredField("jedis");
        field.setAccessible(true);
        return (JedisPooled) field.get(persistence);
    }

    private static String namespaceMetaKey(String name) {
        return "conv:{" + name + "}:meta";
    }

    private static final class RecordingArchiver implements Archiver<Integer> {
        private final java.util.ArrayList<String> calls = new java.util.ArrayList<>();
        private Persistence<Integer> persistence;

        @Override
        public void setPersistence(Persistence<Integer> persistence) {
            this.persistence = persistence;
        }

        @Override
        public void archiveParts(Collection<Long> ids) {
            calls.add("parts");
        }

        @Override
        public void archiveKeys(Collection<Integer> keys) {
            calls.add("keys");
        }

        @Override
        public void archiveCompleteKeys(Collection<Integer> keys) {
            calls.add("complete");
        }

        @Override
        public void archiveExpiredParts() {
            calls.add("expired");
        }

        @Override
        public void archiveAll() {
            calls.add("all");
        }
    }
}
