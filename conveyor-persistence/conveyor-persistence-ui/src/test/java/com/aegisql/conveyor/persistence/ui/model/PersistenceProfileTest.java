package com.aegisql.conveyor.persistence.ui.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceProfileTest {

    @Test
    void networkJdbcProfilesDoNotInventDatabaseOrPassword() {
        PersistenceProfile normalized = new PersistenceProfile(
                null,
                "MySQL Dev",
                PersistenceKind.MYSQL,
                "java.lang.Long",
                null,
                "",
                null,
                "",
                "",
                "PART",
                "COMPLETED_LOG",
                "tester",
                "",
                null
        ).normalized();

        assertEquals("localhost", normalized.host());
        assertEquals(3306, normalized.port());
        assertNull(normalized.database());
        assertNull(normalized.schema());
        assertEquals("tester", normalized.user());
        assertNull(normalized.password());
    }

    @Test
    void jdbcProfilesUseDatabaseAndPartTableAsSuggestedName() {
        PersistenceProfile normalized = new PersistenceProfile(
                null,
                "",
                PersistenceKind.MYSQL,
                "java.lang.Long",
                null,
                "localhost",
                3306,
                "orders",
                null,
                "PARTS_V2",
                "COMPLETED_LOG",
                null,
                null,
                null
        ).normalized();

        assertEquals("orders / PARTS_V2", normalized.displayName());
        assertEquals("orders / PARTS_V2", normalized.suggestedDisplayName());
    }

    @Test
    void redisProfilesUsePersistenceNameAsSuggestedName() {
        PersistenceProfile normalized = new PersistenceProfile(
                null,
                "",
                PersistenceKind.REDIS,
                null,
                "orders-cache",
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

        assertEquals("orders-cache", normalized.displayName());
        assertEquals("orders-cache", normalized.suggestedDisplayName());
    }

    @Test
    void duplicateAsNewClearsOnlyStoredId() {
        PersistenceProfile original = new PersistenceProfile(
                42L,
                "Orders / PART",
                PersistenceKind.MYSQL,
                "java.lang.Long",
                null,
                "localhost",
                3306,
                "orders",
                "app",
                "PART",
                "COMPLETED_LOG",
                "tester",
                "secret",
                null
        );

        PersistenceProfile duplicate = original.duplicateAsNew();

        assertNull(duplicate.id());
        assertEquals(original.displayName(), duplicate.displayName());
        assertEquals(original.kind(), duplicate.kind());
        assertEquals(original.keyClassName(), duplicate.keyClassName());
        assertEquals(original.host(), duplicate.host());
        assertEquals(original.port(), duplicate.port());
        assertEquals(original.database(), duplicate.database());
        assertEquals(original.schema(), duplicate.schema());
        assertEquals(original.partTable(), duplicate.partTable());
        assertEquals(original.completedLogTable(), duplicate.completedLogTable());
        assertEquals(original.user(), duplicate.user());
        assertEquals(original.password(), duplicate.password());
    }

    @Test
    void derbyDefaultsUseValidDatabaseName() {
        PersistenceProfile defaults = PersistenceProfile.defaults(PersistenceKind.DERBY).normalized();

        assertNull(defaults.database());
        assertEquals("conveyor_db", defaults.schema());
        assertTrue(defaults.displayName().contains("Derby"));
    }

    @Test
    void derbyProfilesMigrateLegacyDatabaseName() {
        PersistenceProfile normalized = new PersistenceProfile(
                7L,
                "Legacy Derby",
                PersistenceKind.DERBY,
                "java.lang.Long",
                null,
                null,
                null,
                "/tmp/derby-home",
                "conveyor-db",
                "PART",
                "COMPLETED_LOG",
                null,
                null,
                null
        ).normalized();

        assertEquals("conveyor_db", normalized.schema());
    }
}
