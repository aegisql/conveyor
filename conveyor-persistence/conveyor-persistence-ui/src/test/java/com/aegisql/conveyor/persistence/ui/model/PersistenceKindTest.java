package com.aegisql.conveyor.persistence.ui.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceKindTest {

    @Test
    void mysqlExposesOnlyNetworkJdbcFields() {
        assertTrue(PersistenceKind.MYSQL.showsKeyClassField());
        assertTrue(PersistenceKind.MYSQL.showsHostField());
        assertTrue(PersistenceKind.MYSQL.showsPortField());
        assertTrue(PersistenceKind.MYSQL.showsDatabaseField());
        assertFalse(PersistenceKind.MYSQL.showsSchemaField());
        assertTrue(PersistenceKind.MYSQL.showsUserField());
        assertTrue(PersistenceKind.MYSQL.showsPasswordField());
        assertFalse(PersistenceKind.MYSQL.showsPersistenceNameField());
        assertFalse(PersistenceKind.MYSQL.showsRedisUriField());
        assertEquals("Lookup", PersistenceKind.MYSQL.databaseActionLabel());
    }

    @Test
    void sqliteUsesLocalFileSelection() {
        assertTrue(PersistenceKind.SQLITE.showsDatabaseField());
        assertTrue(PersistenceKind.SQLITE.usesLocalDatabasePath());
        assertFalse(PersistenceKind.SQLITE.usesDirectoryDatabasePath());
        assertFalse(PersistenceKind.SQLITE.showsHostField());
        assertFalse(PersistenceKind.SQLITE.showsUserField());
        assertEquals("Database File", PersistenceKind.SQLITE.databaseFieldLabel());
        assertEquals("Browse", PersistenceKind.SQLITE.databaseActionLabel());
    }

    @Test
    void derbyUsesLocalDirectorySelection() {
        assertTrue(PersistenceKind.DERBY.showsDatabaseField());
        assertTrue(PersistenceKind.DERBY.usesLocalDatabasePath());
        assertTrue(PersistenceKind.DERBY.usesDirectoryDatabasePath());
        assertTrue(PersistenceKind.DERBY.showsSchemaField());
        assertFalse(PersistenceKind.DERBY.usesSchemaLookup());
        assertEquals("Database Home Directory", PersistenceKind.DERBY.databaseFieldLabel());
        assertEquals("Database Name", PersistenceKind.DERBY.schemaFieldLabel());
        assertEquals("Browse", PersistenceKind.DERBY.databaseActionLabel());
    }

    @Test
    void redisShowsOnlyRedisSpecificFields() {
        assertTrue(PersistenceKind.REDIS.showsPersistenceNameField());
        assertTrue(PersistenceKind.REDIS.showsRedisUriField());
        assertFalse(PersistenceKind.REDIS.showsDatabaseField());
        assertFalse(PersistenceKind.REDIS.showsHostField());
        assertFalse(PersistenceKind.REDIS.showsKeyClassField());
    }
}
