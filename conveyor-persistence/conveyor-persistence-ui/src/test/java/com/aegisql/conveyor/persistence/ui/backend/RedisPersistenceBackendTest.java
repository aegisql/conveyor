package com.aegisql.conveyor.persistence.ui.backend;

import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.redis.RedisPersistenceBuilder;
import com.aegisql.conveyor.persistence.ui.model.ConnectionStatus;
import com.aegisql.conveyor.persistence.ui.model.PersistenceKind;
import com.aegisql.conveyor.persistence.ui.model.PayloadEncryptionMode;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;
import com.aegisql.conveyor.persistence.ui.model.PersistenceSnapshot;
import com.aegisql.conveyor.persistence.ui.model.TableData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import redis.clients.jedis.JedisPooled;

import java.nio.ByteBuffer;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisPersistenceBackendTest {

    @Test
    void initializesAndReadsRedisNamespaceWhenRedisIsAvailable() {
        Assumptions.assumeTrue(redisAvailable(), "Redis is not available for RedisPersistenceBackendTest");

        String persistenceName = "ui-test-" + System.nanoTime();
        String namespace = "conv:{" + persistenceName + '}';
        PersistenceProfile profile = new PersistenceProfile(
                null,
                "Redis UI",
                PersistenceKind.REDIS,
                null,
                persistenceName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "redis://localhost:6379"
        ).normalized();

        RedisPersistenceBackend backend = new RedisPersistenceBackend();
        assertEquals(ConnectionStatus.CONNECTED_UNINITIALIZED, backend.connectionStatus(profile));
        PersistenceSnapshot beforeInit = backend.inspect(profile, 20, 0);
        assertEquals(ConnectionStatus.CONNECTED_UNINITIALIZED, beforeInit.status());

        backend.initialize(profile);
        assertEquals(ConnectionStatus.READY, backend.connectionStatus(profile));
        assertTrue(backend.lookupPersistenceNames(profile).contains(persistenceName));

        PersistenceSnapshot afterInit = backend.inspect(profile, 20, 0);
        assertEquals(ConnectionStatus.READY, afterInit.status());
        assertFalse(afterInit.tables().isEmpty());
        assertEquals(1, afterInit.infoTables().size());
        assertEquals("Namespace Meta", afterInit.infoTables().getFirst().title());
        assertFalse(afterInit.infoTables().getFirst().rows().isEmpty());
        assertTrue(afterInit.infoTables().getFirst().rows().stream().anyMatch(row -> row.contains("redis") || row.contains("lua")));
        assertEquals(0, afterInit.pageIndex());
        assertFalse(afterInit.hasPreviousPage());

        backend.archiveAll(profile);
        try (JedisPooled jedis = new JedisPooled("redis://localhost:6379")) {
            assertFalse(jedis.exists(namespace + ":meta"));
        }
    }

    @Test
    void showsAvailablePersistencesWhenRedisNamespaceNameDoesNotMatchConveyorName() {
        Assumptions.assumeTrue(redisAvailable(), "Redis is not available for RedisPersistenceBackendTest");

        String persistenceName = "ui-test-" + System.nanoTime();
        PersistenceProfile persistenceProfile = new PersistenceProfile(
                null,
                "Redis Persistence",
                PersistenceKind.REDIS,
                null,
                persistenceName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "redis://localhost:6379"
        ).normalized();
        PersistenceProfile conveyorNameProfile = new PersistenceProfile(
                null,
                "Redis Conveyor",
                PersistenceKind.REDIS,
                null,
                "redisPerfShuffled",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "redis://localhost:6379"
        ).normalized();

        RedisPersistenceBackend backend = new RedisPersistenceBackend();
        backend.initialize(persistenceProfile);

        try {
            PersistenceSnapshot snapshot = backend.inspect(conveyorNameProfile, 20, 0);
            assertEquals(ConnectionStatus.CONNECTED_UNINITIALIZED, snapshot.status());
            assertTrue(snapshot.detailMessage().contains("not the runtime conveyor name"));

            TableData infoTable = snapshot.infoTables().stream()
                    .filter(table -> table.title().equals("Available Persistences"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(infoTable);
            assertTrue(infoTable.rows().stream().anyMatch(row ->
                    row.getFirst().equals(persistenceName) && row.get(1).equals("conv:{" + persistenceName + '}')));
        } finally {
            backend.archiveAll(persistenceProfile);
        }
    }

    @Test
    void decodesRedisPartKeyLabelAndValueInPreviewTable() throws Exception {
        Assumptions.assumeTrue(redisAvailable(), "Redis is not available for RedisPersistenceBackendTest");

        String persistenceName = "ui-visible-" + System.nanoTime();
        PersistenceProfile profile = new PersistenceProfile(
                null,
                "Redis Visible",
                PersistenceKind.REDIS,
                null,
                persistenceName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "redis://localhost:6379"
        ).normalized();

        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(persistenceName).noArchiving().build()) {
            persistence.archiveAll();
            persistence.savePart(1L, new ShoppingCart<>(17, "visible-value", "LBL"));
            persistence.savePart(2L, new ShoppingCart<>(null, "static-visible", "STATIC_LBL",
                    System.currentTimeMillis(), 0, null, LoadType.STATIC_PART, 5));
        }

        RedisPersistenceBackend backend = new RedisPersistenceBackend();
        try {
            PersistenceSnapshot snapshot = backend.inspect(profile, 20, 0);
            assertEquals(ConnectionStatus.READY, snapshot.status());

            TableData activeParts = snapshot.tables().stream()
                    .filter(table -> table.title().equals("Active Parts"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(activeParts);
            assertTrue(activeParts.columns().contains("Key"));
            assertTrue(activeParts.columns().contains("Label"));
            assertTrue(activeParts.columns().contains("Value"));
            assertTrue(activeParts.rows().stream().anyMatch(row ->
                    row.contains("17")
                            && row.contains("LBL")
                            && row.contains("visible-value")));

            TableData staticParts = snapshot.tables().stream()
                    .filter(table -> table.title().equals("Static Parts"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(staticParts);
            assertFalse(staticParts.columns().contains("Load Type"));
            assertFalse(staticParts.columns().contains("Key"));
            assertFalse(staticParts.columns().contains("Key Hint"));
            assertTrue(staticParts.columns().contains("Label"));
            assertTrue(staticParts.columns().contains("Value"));
            assertTrue(staticParts.rows().stream().anyMatch(row ->
                    row.contains("STATIC_LBL")
                            && row.contains("static-visible")));
        } finally {
            backend.archiveAll(profile);
        }
    }

    @Test
    void showsReadableFallbackForEnumLabelWhenClassIsUnavailable() throws Exception {
        Assumptions.assumeTrue(redisAvailable(), "Redis is not available for RedisPersistenceBackendTest");

        String persistenceName = "ui-fallback-" + System.nanoTime();
        PersistenceProfile profile = new PersistenceProfile(
                null,
                "Redis Fallback",
                PersistenceKind.REDIS,
                null,
                persistenceName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "redis://localhost:6379"
        ).normalized();

        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(persistenceName)
                .labelConverter(FallbackLabel.class)
                .noArchiving()
                .build()) {
            persistence.archiveAll();
            persistence.savePart(1L, new ShoppingCart<>(17, "visible-value", FallbackLabel.TEXT1));
        }

        RedisPersistenceBackend backend = new RedisPersistenceBackend();
        try {
            PersistenceSnapshot snapshot = backend.inspect(profile, 20, 0);
            assertEquals(ConnectionStatus.READY, snapshot.status());

            TableData activeParts = snapshot.tables().stream()
                    .filter(table -> table.title().equals("Active Parts"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(activeParts);
            assertTrue(activeParts.rows().stream().anyMatch(row ->
                    row.contains("TEXT1")
                            && row.contains("visible-value")));
        } finally {
            backend.archiveAll(profile);
        }
    }

    @Test
    void showsOrdinalFallbackForLegacyEnumLabelBytes() throws Exception {
        Assumptions.assumeTrue(redisAvailable(), "Redis is not available for RedisPersistenceBackendTest");

        String persistenceName = "ui-fallback-legacy-" + System.nanoTime();
        PersistenceProfile profile = new PersistenceProfile(
                null,
                "Redis Fallback Legacy",
                PersistenceKind.REDIS,
                null,
                persistenceName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "redis://localhost:6379"
        ).normalized();

        String namespace = "conv:{" + persistenceName + '}';
        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(persistenceName).noArchiving().build()) {
            persistence.archiveAll();
            persistence.savePart(1L, new ShoppingCart<>(17, "visible-value", "ORIGINAL"));
        }

        try (JedisPooled jedis = new JedisPooled("redis://localhost:6379")) {
            jedis.hset(namespace + ":part:1:meta", "labelHint", "FallbackLabel:byte[]");
            jedis.hset(namespace + ":part:1:meta", "labelData", encodeInt(0));
        }

        RedisPersistenceBackend backend = new RedisPersistenceBackend();
        try {
            PersistenceSnapshot snapshot = backend.inspect(profile, 20, 0);
            assertEquals(ConnectionStatus.READY, snapshot.status());

            TableData activeParts = snapshot.tables().stream()
                    .filter(table -> table.title().equals("Active Parts"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(activeParts);
            assertTrue(activeParts.rows().stream().anyMatch(row ->
                    row.contains("FallbackLabel#0")
                            && row.contains("visible-value")));
        } finally {
            backend.archiveAll(profile);
        }
    }

    @Test
    void decryptsEncryptedRedisValuesWhenSecretIsConfigured() throws Exception {
        Assumptions.assumeTrue(redisAvailable(), "Redis is not available for RedisPersistenceBackendTest");

        String persistenceName = "ui-encrypted-" + System.nanoTime();
        PersistenceProfile profile = new PersistenceProfile(
                null,
                "Redis Encrypted",
                PersistenceKind.REDIS,
                null,
                persistenceName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "redis://localhost:6379",
                PayloadEncryptionMode.MANAGED_DEFAULT,
                "viewer-secret"
        ).normalized();

        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(persistenceName)
                .encryptionSecret("viewer-secret")
                .noArchiving()
                .build()) {
            persistence.archiveAll();
            persistence.savePart(1L, new ShoppingCart<>(17, "visible-value", "LBL"));
        }

        RedisPersistenceBackend backend = new RedisPersistenceBackend();
        try {
            PersistenceSnapshot decrypted = backend.inspect(profile, 20, 0);
            assertEquals(ConnectionStatus.READY, decrypted.status());
            assertEquals("17", cellValue(decrypted, "Active Parts", "Key", 0));
            assertEquals("LBL", cellValue(decrypted, "Active Parts", "Label", 0));
            assertEquals("visible-value", cellValue(decrypted, "Active Parts", "Value", 0));
            assertTrue(cellValue(decrypted, "Active Parts", "Value Hint", 0).startsWith("__##"));

            PersistenceSnapshot hidden = backend.inspect(
                    profile.withPayloadEncryptionMode(PayloadEncryptionMode.NONE).withEncryptionSecret(null),
                    20,
                    0
            );
            assertEquals("17", cellValue(hidden, "Active Parts", "Key", 0));
            assertEquals("LBL", cellValue(hidden, "Active Parts", "Label", 0));
            assertEquals("<encrypted payload>", cellValue(hidden, "Active Parts", "Value", 0));

            PersistenceSnapshot wrongSecret = backend.inspect(profile.withEncryptionSecret("wrong-secret"), 20, 0);
            assertEquals("17", cellValue(wrongSecret, "Active Parts", "Key", 0));
            assertEquals("LBL", cellValue(wrongSecret, "Active Parts", "Label", 0));
            assertEquals("<decryption failed>", cellValue(wrongSecret, "Active Parts", "Value", 0));
        } finally {
            backend.archiveAll(profile);
        }
    }

    private boolean redisAvailable() {
        try (JedisPooled jedis = new JedisPooled("redis://localhost:6379")) {
            return "PONG".equalsIgnoreCase(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }

    private String cellValue(PersistenceSnapshot snapshot, String tableTitle, String columnName, int rowIndex) {
        return snapshot.tables().stream()
                .filter(table -> table.title().equals(tableTitle))
                .findFirst()
                .map(table -> {
                    int columnIndex = table.columns().indexOf(columnName);
                    if (columnIndex < 0) {
                        throw new AssertionError("Column not found: " + columnName);
                    }
                    return table.rows().get(rowIndex).get(columnIndex);
                })
                .orElseThrow(() -> new AssertionError("Table not found: " + tableTitle));
    }

    private static String encodeInt(int value) {
        byte[] bytes = new byte[Integer.BYTES];
        ByteBuffer.wrap(bytes).putInt(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private enum FallbackLabel {
        TEXT1
    }
}
