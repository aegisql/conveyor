package com.aegisql.conveyor.persistence.ui.store;

import com.aegisql.conveyor.persistence.ui.model.PersistenceKind;
import com.aegisql.conveyor.persistence.ui.model.PayloadEncryptionMode;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;
import com.aegisql.conveyor.persistence.ui.store.credentials.CredentialKind;
import com.aegisql.conveyor.persistence.ui.store.credentials.CredentialStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SqliteProfileStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void savesUpdatesAndDeletesProfiles() {
        InMemoryCredentialStore credentialStore = new InMemoryCredentialStore();
        SqliteProfileStore store = new SqliteProfileStore(tempDir.resolve("profiles.db"), credentialStore);

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
                "redis://localhost:6379/3",
                PayloadEncryptionMode.MANAGED_DEFAULT,
                "viewer-secret"
        ));

        List<PersistenceProfile> updatedProfiles = store.listProfiles();
        assertEquals(1, updatedProfiles.size());
        assertEquals("Redis Dev", updatedProfiles.getFirst().displayName());
        assertEquals(PersistenceKind.REDIS, updatedProfiles.getFirst().kind());
        assertEquals("orders", updatedProfiles.getFirst().persistenceName());
        assertEquals("redis://localhost:6379/3", updatedProfiles.getFirst().redisUri());
        assertEquals(PayloadEncryptionMode.MANAGED_DEFAULT, updatedProfiles.getFirst().payloadEncryptionMode());
        assertEquals(updated.id(), updatedProfiles.getFirst().id());
        assertNull(updatedProfiles.getFirst().password());
        assertNull(updatedProfiles.getFirst().encryptionSecret());
        PersistenceProfile resolved = store.withCredentials(updatedProfiles.getFirst());
        assertEquals("secret", resolved.password());
        assertEquals("viewer-secret", resolved.encryptionSecret());
        assertEquals(Optional.of("secret"), credentialStore.load(updated.id(), CredentialKind.PASSWORD));
        assertEquals(Optional.of("viewer-secret"), credentialStore.load(updated.id(), CredentialKind.ENCRYPTION_SECRET));
        assertNull(loadStoredPassword(tempDir.resolve("profiles.db"), updated.id()));

        store.delete(updated.id());
        assertEquals(0, store.listProfiles().size());
        assertEquals(Optional.empty(), credentialStore.load(updated.id(), CredentialKind.PASSWORD));
        assertEquals(Optional.empty(), credentialStore.load(updated.id(), CredentialKind.ENCRYPTION_SECRET));
    }

    @Test
    void listProfilesMigratesLegacyDerbyDatabaseName() throws Exception {
        Path storePath = tempDir.resolve("profiles.db");
        SqliteProfileStore store = new SqliteProfileStore(storePath, new InMemoryCredentialStore());

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

    @Test
    void savesHiddenColumnsPerProfileAndTable() {
        SqliteProfileStore store = new SqliteProfileStore(tempDir.resolve("profiles.db"), new InMemoryCredentialStore());

        PersistenceProfile first = store.save(new PersistenceProfile(
                null,
                "One",
                PersistenceKind.SQLITE,
                "java.lang.Long",
                null,
                null,
                null,
                tempDir.resolve("one.db").toString(),
                null,
                "PART",
                "COMPLETED_LOG",
                null,
                null,
                null
        ));
        PersistenceProfile second = store.save(new PersistenceProfile(
                null,
                "Two",
                PersistenceKind.SQLITE,
                "java.lang.Long",
                null,
                null,
                null,
                tempDir.resolve("two.db").toString(),
                null,
                "PART",
                "COMPLETED_LOG",
                null,
                null,
                null
        ));

        store.saveHiddenColumns(first.id(), "Parts", Set.of("Payload", "Additional"));
        store.saveHiddenColumns(first.id(), "Completed Keys", Set.of("Key"));
        store.saveHiddenColumns(second.id(), "Parts", Set.of("Created"));

        Map<String, Set<String>> firstHiddenColumns = store.loadHiddenColumns(first.id());
        Map<String, Set<String>> secondHiddenColumns = store.loadHiddenColumns(second.id());

        assertEquals(Set.of("Payload", "Additional"), firstHiddenColumns.get("Parts"));
        assertEquals(Set.of("Key"), firstHiddenColumns.get("Completed Keys"));
        assertEquals(Set.of("Created"), secondHiddenColumns.get("Parts"));
    }

    @Test
    void migratesLegacyPlaintextPasswordWhenProfileIsAccessed() throws Exception {
        Path storePath = tempDir.resolve("profiles.db");
        InMemoryCredentialStore credentialStore = new InMemoryCredentialStore();
        SqliteProfileStore store = new SqliteProfileStore(storePath, credentialStore);

        long profileId;
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + storePath);
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO profiles (
                         display_name, kind, key_class_name, host, port, database_name, part_table, completed_log_table,
                         user_name, password
                     ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "Legacy MySQL");
            statement.setString(2, PersistenceKind.MYSQL.name());
            statement.setString(3, "java.lang.Long");
            statement.setString(4, "localhost");
            statement.setInt(5, 3306);
            statement.setString(6, "orders");
            statement.setString(7, "PART");
            statement.setString(8, "COMPLETED_LOG");
            statement.setString(9, "tester");
            statement.setString(10, "legacy-secret");
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                profileId = keys.getLong(1);
            }
        }

        PersistenceProfile listed = store.listProfiles().getFirst();
        assertNull(listed.password());
        PersistenceProfile resolved = store.withCredentials(listed);

        assertEquals(profileId, resolved.id());
        assertEquals("legacy-secret", resolved.password());
        assertEquals(Optional.of("legacy-secret"), credentialStore.load(profileId, CredentialKind.PASSWORD));
        assertNull(loadStoredPassword(storePath, profileId));
    }

    private String loadStoredPassword(Path storePath, long profileId) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + storePath);
             PreparedStatement statement = connection.prepareStatement("SELECT password FROM profiles WHERE id = ?")) {
            statement.setLong(1, profileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return resultSet.getString("password");
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class InMemoryCredentialStore implements CredentialStore {
        private final Map<Long, Map<CredentialKind, String>> values = new HashMap<>();

        @Override
        public Optional<String> load(long profileId, CredentialKind credentialKind) {
            return Optional.ofNullable(values.getOrDefault(profileId, Map.of()).get(credentialKind));
        }

        @Override
        public void save(long profileId, CredentialKind credentialKind, String secret) {
            values.computeIfAbsent(profileId, ignored -> new HashMap<>()).put(credentialKind, secret);
        }

        @Override
        public void delete(long profileId, CredentialKind credentialKind) {
            Map<CredentialKind, String> secrets = values.get(profileId);
            if (secrets != null) {
                secrets.remove(credentialKind);
                if (secrets.isEmpty()) {
                    values.remove(profileId);
                }
            }
        }
    }
}
