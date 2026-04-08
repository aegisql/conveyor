package com.aegisql.conveyor.persistence.ui.backend;

import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.ui.model.ConnectionStatus;
import com.aegisql.conveyor.persistence.ui.model.PersistenceKind;
import com.aegisql.conveyor.persistence.ui.model.PayloadEncryptionMode;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;
import com.aegisql.conveyor.persistence.ui.model.PersistenceSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcPersistenceBackendTest {

    @TempDir
    Path tempDir;

    @Test
    void initializesAndPreviewsSqlitePersistence() throws Exception {
        Path databaseFile = tempDir.resolve("workbench.db");
        PersistenceProfile profile = new PersistenceProfile(
                null,
                "SQLite Workbench",
                PersistenceKind.SQLITE,
                Integer.class.getName(),
                null,
                null,
                null,
                databaseFile.toString(),
                null,
                "PART",
                "COMPLETED_LOG",
                null,
                null,
                null
        ).normalized();

        JdbcPersistenceBackend backend = new JdbcPersistenceBackend();
        assertEquals(ConnectionStatus.CONNECTED_UNINITIALIZED, backend.connectionStatus(profile));
        PersistenceSnapshot beforeInit = backend.inspect(profile, 20, 0);
        assertEquals(ConnectionStatus.CONNECTED_UNINITIALIZED, beforeInit.status());
        assertTrue(backend.initializationScript(profile).contains("CREATE TABLE"));

        backend.initialize(profile);
        assertEquals(ConnectionStatus.READY, backend.connectionStatus(profile));

        try (Persistence<Integer> persistence = buildPersistence(profile)) {
            for (int i = 0; i < 3; i++) {
                long id = persistence.nextUniquePartId();
                int key = 101 + i;
                ShoppingCart<Integer, Integer, String> cart = new ShoppingCart<>(key, 42 + i, "LBL");
                persistence.savePart(id, cart);
                persistence.savePartId(key, id);
            }
            persistence.saveCompletedBuildKey(101);
        }

        PersistenceSnapshot afterInit = backend.inspect(profile, 2, 0);
        assertEquals(ConnectionStatus.READY, afterInit.status());
        assertEquals(2, afterInit.tables().size());
        assertEquals(1, afterInit.infoTables().size());
        assertEquals("Indexes", afterInit.infoTables().getFirst().title());
        assertFalse(afterInit.infoTables().getFirst().rows().isEmpty());
        assertFalse(afterInit.tables().getFirst().rows().isEmpty());
        assertTrue(afterInit.summaryEntries().stream().anyMatch(entry -> entry.label().equals("Stored Parts") && entry.value().equals("3")));
        assertTrue(afterInit.summaryEntries().stream().anyMatch(entry -> entry.label().equals("Completed Keys") && entry.value().equals("1")));
        assertEquals("42", cellValue(afterInit, "Parts", "CART_VALUE", 0));
        assertEquals("Integer:byte[]", cellValue(afterInit, "Parts", "VALUE_TYPE", 0));
        assertEquals(0, afterInit.pageIndex());
        assertFalse(afterInit.hasPreviousPage());
        assertTrue(afterInit.hasNextPage());

        PersistenceSnapshot secondPage = backend.inspect(profile, 2, 1);
        assertEquals(ConnectionStatus.READY, secondPage.status());
        assertEquals("44", cellValue(secondPage, "Parts", "CART_VALUE", 0));
        assertEquals(1, secondPage.pageIndex());
        assertTrue(secondPage.hasPreviousPage());
        assertFalse(secondPage.hasNextPage());
    }

    @Test
    void requiresDatabaseForNetworkJdbcInitializationActions() {
        PersistenceProfile profile = new PersistenceProfile(
                null,
                "MySQL Workbench",
                PersistenceKind.MYSQL,
                Integer.class.getName(),
                null,
                "localhost",
                3306,
                null,
                null,
                "PART",
                "COMPLETED_LOG",
                "tester",
                null,
                null
        ).normalized();

        JdbcPersistenceBackend backend = new JdbcPersistenceBackend();

        IllegalArgumentException scriptFailure = assertThrows(IllegalArgumentException.class, () -> backend.initializationScript(profile));
        assertTrue(scriptFailure.getMessage().contains("Database name is required"));

        IllegalArgumentException initFailure = assertThrows(IllegalArgumentException.class, () -> backend.initialize(profile));
        assertTrue(initFailure.getMessage().contains("Database name is required"));
    }

    @Test
    void looksUpDatabaseAndSchemaChoicesForSqliteWithoutFailing() {
        Path databaseFile = tempDir.resolve("lookup.db");
        PersistenceProfile profile = new PersistenceProfile(
                null,
                "SQLite Lookup",
                PersistenceKind.SQLITE,
                Integer.class.getName(),
                null,
                null,
                null,
                databaseFile.toString(),
                null,
                "PART",
                "COMPLETED_LOG",
                null,
                null,
                null
        ).normalized();

        JdbcPersistenceBackend backend = new JdbcPersistenceBackend();
        backend.initialize(profile);

        assertTrue(backend.lookupDatabases(profile).contains(databaseFile.toString()));
        assertNotNull(backend.lookupSchemas(profile));
    }

    @Test
    void generatesJavaInitializationCodeForJdbcProfile() {
        Path databaseFile = tempDir.resolve("java-preview.db");
        PersistenceProfile profile = new PersistenceProfile(
                null,
                "SQLite Java",
                PersistenceKind.SQLITE,
                Integer.class.getName(),
                null,
                null,
                null,
                databaseFile.toString(),
                null,
                "PART",
                "COMPLETED_LOG",
                null,
                "secret",
                null,
                PayloadEncryptionMode.LEGACY_AES_ECB,
                "viewer-secret"
        ).normalized();

        JdbcPersistenceBackend backend = new JdbcPersistenceBackend();
        String code = backend.initializationJavaCode(profile);

        assertTrue(code.contains("JdbcPersistenceBuilder"));
        assertTrue(code.contains(".presetInitializer(\"sqlite\", Integer.class)"));
        assertTrue(code.contains(".database(\"" + databaseFile.toString().replace("\\", "\\\\") + "\")"));
        assertTrue(code.contains(".partTable(\"PART\")"));
        assertTrue(code.contains(".completedLogTable(\"COMPLETED_LOG\")"));
        assertTrue(code.contains(".password(\"<jdbc-password>\")"));
        assertTrue(code.contains(".encryptionSecret(\"<encryption-secret>\")"));
        assertTrue(code.contains(".encryptionTransformation(\"AES/ECB/PKCS5Padding\")"));
        assertTrue(code.contains("builder.init();"));
    }

    @Test
    void treatsMissingDerbyDatabaseAsInitializable() throws Exception {
        Path databaseDir = tempDir.resolve("fresh-derby-db");
        Files.createDirectories(databaseDir);
        PersistenceProfile profile = new PersistenceProfile(
                null,
                "Derby Workbench",
                PersistenceKind.DERBY,
                Integer.class.getName(),
                null,
                null,
                null,
                databaseDir.toString(),
                "app",
                "PART",
                "COMPLETED_LOG",
                null,
                null,
                null
        ).normalized();

        JdbcPersistenceBackend backend = new JdbcPersistenceBackend();
        assertEquals(ConnectionStatus.CONNECTED_UNINITIALIZED, backend.connectionStatus(profile));

        PersistenceSnapshot beforeInit = backend.inspect(profile, 20, 0);
        assertEquals(ConnectionStatus.CONNECTED_UNINITIALIZED, beforeInit.status());
        assertTrue(beforeInit.canInitialize());

        backend.initialize(profile);
        assertEquals(ConnectionStatus.READY, backend.connectionStatus(profile));

        PersistenceSnapshot afterInit = backend.inspect(profile, 20, 0);
        assertEquals(ConnectionStatus.READY, afterInit.status());
    }

    @Test
    void decryptsEncryptedJdbcCartValuesWhenSecretIsConfigured() throws Exception {
        Path databaseFile = tempDir.resolve("encrypted-workbench.db");
        PersistenceProfile profile = new PersistenceProfile(
                null,
                "SQLite Encrypted",
                PersistenceKind.SQLITE,
                Integer.class.getName(),
                null,
                null,
                null,
                databaseFile.toString(),
                null,
                "PART",
                "COMPLETED_LOG",
                null,
                null,
                null,
                PayloadEncryptionMode.MANAGED_DEFAULT,
                "viewer-secret"
        ).normalized();

        JdbcPersistenceBackend backend = new JdbcPersistenceBackend();
        backend.initialize(profile);

        try (Persistence<Integer> persistence = buildPersistence(profile, "viewer-secret")) {
            long id = persistence.nextUniquePartId();
            persistence.savePart(id, new ShoppingCart<>(401, "shielded-value", "LBL"));
            persistence.savePartId(401, id);
        }

        PersistenceSnapshot decrypted = backend.inspect(profile, 20, 0);
        assertEquals("shielded-value", cellValue(decrypted, "Parts", "CART_VALUE", 0));
        assertTrue(cellValue(decrypted, "Parts", "VALUE_TYPE", 0).startsWith("__##"));

        PersistenceSnapshot hidden = backend.inspect(
                profile.withPayloadEncryptionMode(PayloadEncryptionMode.NONE).withEncryptionSecret(null),
                20,
                0
        );
        assertEquals("<encrypted payload>", cellValue(hidden, "Parts", "CART_VALUE", 0));

        PersistenceSnapshot wrongSecret = backend.inspect(profile.withEncryptionSecret("wrong-secret"), 20, 0);
        assertEquals("<decryption failed>", cellValue(wrongSecret, "Parts", "CART_VALUE", 0));
    }

    @SuppressWarnings("unchecked")
    private Persistence<Integer> buildPersistence(PersistenceProfile profile) {
        return buildPersistence(profile, null);
    }

    @SuppressWarnings("unchecked")
    private Persistence<Integer> buildPersistence(PersistenceProfile profile, String encryptionSecret) {
        try {
            JdbcPersistenceBuilder<Integer> builder = JdbcPersistenceBuilder.presetInitializer("sqlite", Integer.class)
                    .autoInit(false)
                    .database(profile.database())
                    .partTable(profile.partTable())
                    .completedLogTable(profile.completedLogTable());
            if (encryptionSecret != null) {
                builder = builder.encryptionSecret(encryptionSecret);
            }
            return (Persistence<Integer>) builder.build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
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
}
