package com.aegisql.conveyor.persistence.ui.store;

import com.aegisql.conveyor.persistence.ui.model.PersistenceKind;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
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
}
