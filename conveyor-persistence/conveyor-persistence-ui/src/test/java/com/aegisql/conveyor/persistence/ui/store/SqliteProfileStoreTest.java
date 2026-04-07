package com.aegisql.conveyor.persistence.ui.store;

import com.aegisql.conveyor.persistence.ui.model.PersistenceKind;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SqliteProfileStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void savesUpdatesAndDeletesProfiles() {
        SqliteProfileStore store = new SqliteProfileStore(tempDir.resolve("profiles.db"));

        PersistenceProfile saved = store.save(new PersistenceProfile(
                null,
                "Local SQLite",
                PersistenceKind.SQLITE,
                "java.lang.Long",
                null,
                null,
                null,
                tempDir.resolve("local.db").toString(),
                null,
                "PART",
                "COMPLETED_LOG",
                null,
                null,
                null
        ));

        assertNotNull(saved.id());
        List<PersistenceProfile> profiles = store.listProfiles();
        assertEquals(1, profiles.size());
        assertEquals("Local SQLite", profiles.getFirst().displayName());

        PersistenceProfile updated = store.save(new PersistenceProfile(
                saved.id(),
                "Redis Dev",
                PersistenceKind.REDIS,
                null,
                "orders",
                null,
                null,
                null,
                null,
                null,
                null,
                "default",
                "secret",
                "redis://localhost:6379/3"
        ));

        List<PersistenceProfile> updatedProfiles = store.listProfiles();
        assertEquals(1, updatedProfiles.size());
        assertEquals("Redis Dev", updatedProfiles.getFirst().displayName());
        assertEquals(PersistenceKind.REDIS, updatedProfiles.getFirst().kind());
        assertEquals("orders", updatedProfiles.getFirst().persistenceName());
        assertEquals("redis://localhost:6379/3", updatedProfiles.getFirst().redisUri());
        assertEquals(updated.id(), updatedProfiles.getFirst().id());

        store.delete(updated.id());
        assertEquals(0, store.listProfiles().size());
    }

    @Test
    void listProfilesMigratesLegacyDerbyDatabaseName() throws Exception {
        Path storePath = tempDir.resolve("profiles.db");
        SqliteProfileStore store = new SqliteProfileStore(storePath);

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + storePath);
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO profiles (
                         display_name, kind, key_class_name, database_name, schema_name, part_table, completed_log_table
                     ) VALUES (?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, "Legacy Derby");
            statement.setString(2, PersistenceKind.DERBY.name());
            statement.setString(3, "java.lang.Long");
            statement.setString(4, tempDir.resolve("derby-home").toString());
            statement.setString(5, "conveyor-db");
            statement.setString(6, "PART");
            statement.setString(7, "COMPLETED_LOG");
            statement.executeUpdate();
        }

        List<PersistenceProfile> profiles = store.listProfiles();

        assertEquals(1, profiles.size());
        assertEquals(PersistenceKind.DERBY, profiles.getFirst().kind());
        assertEquals("conveyor_db", profiles.getFirst().schema());
    }
}
