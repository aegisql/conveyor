package com.aegisql.conveyor.persistence.ui.store.credentials;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

final class MasterPasswordCredentialStore implements CredentialStore {

    private static final String CREATE_STATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS credential_store_state (
                store_id INTEGER PRIMARY KEY CHECK (store_id = 1),
                salt BLOB NOT NULL,
                verifier_iv BLOB NOT NULL,
                verifier_ciphertext BLOB NOT NULL
            )
            """;

    private static final String CREATE_CREDENTIAL_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS stored_credentials (
                profile_id INTEGER NOT NULL,
                secret_kind TEXT NOT NULL,
                iv BLOB NOT NULL,
                ciphertext BLOB NOT NULL,
                PRIMARY KEY (profile_id, secret_kind)
            )
            """;

    private static final byte[] VERIFIER_TEXT = "conveyor-persistence-ui-master-key".getBytes(StandardCharsets.UTF_8);
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final int KEY_SIZE_BITS = 256;
    private static final int SALT_SIZE_BYTES = 16;
    private static final int IV_SIZE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final Path databasePath;
    private final MasterPasswordProvider passwordProvider;
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile SecretKeySpec sessionKey;

    MasterPasswordCredentialStore(Path databasePath, MasterPasswordProvider passwordProvider) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath must not be null").toAbsolutePath();
        this.passwordProvider = Objects.requireNonNull(passwordProvider, "passwordProvider must not be null");
        ensureParentDirectory();
        ensureSchema();
    }

    @Override
    public String displayName() {
        return "Master Password (Encrypted Local Store)";
    }

    @Override
    public Optional<String> load(long profileId, CredentialKind credentialKind) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT iv, ciphertext
                     FROM stored_credentials
                     WHERE profile_id = ? AND secret_kind = ?
                     """)) {
            statement.setLong(1, profileId);
            statement.setString(2, credentialKind.storageKey());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                SecretKeySpec key = ensureSessionKey(connection, false);
                byte[] plaintext = decrypt(key, resultSet.getBytes("iv"), resultSet.getBytes("ciphertext"));
                return Optional.of(new String(plaintext, StandardCharsets.UTF_8));
            }
        } catch (SQLException | GeneralSecurityException e) {
            throw new IllegalStateException("Failed to load encrypted credential", e);
        }
    }

    @Override
    public void save(long profileId, CredentialKind credentialKind, String secret) {
        if (secret == null || secret.isBlank()) {
            delete(profileId, credentialKind);
            return;
        }
        try (Connection connection = openConnection()) {
            SecretKeySpec key = ensureSessionKey(connection, true);
            byte[] iv = randomBytes(IV_SIZE_BYTES);
            byte[] ciphertext = encrypt(key, iv, secret.getBytes(StandardCharsets.UTF_8));
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO stored_credentials (profile_id, secret_kind, iv, ciphertext)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(profile_id, secret_kind)
                    DO UPDATE SET iv = excluded.iv, ciphertext = excluded.ciphertext
                    """)) {
                statement.setLong(1, profileId);
                statement.setString(2, credentialKind.storageKey());
                statement.setBytes(3, iv);
                statement.setBytes(4, ciphertext);
                statement.executeUpdate();
            }
        } catch (SQLException | GeneralSecurityException e) {
            throw new IllegalStateException("Failed to save encrypted credential", e);
        }
    }

    @Override
    public void delete(long profileId, CredentialKind credentialKind) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM stored_credentials WHERE profile_id = ? AND secret_kind = ?
                     """)) {
            statement.setLong(1, profileId);
            statement.setString(2, credentialKind.storageKey());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete encrypted credential", e);
        }
    }

    private SecretKeySpec ensureSessionKey(Connection connection, boolean allowCreate) {
        SecretKeySpec cachedKey = sessionKey;
        if (cachedKey != null) {
            return cachedKey;
        }
        StoreState state = loadState(connection);
        if (state == null) {
            if (!allowCreate) {
                throw new IllegalStateException("Stored credentials exist, but the master-password store is not initialized.");
            }
            return initializeMasterPassword(connection);
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            char[] password = passwordProvider.requestMasterPassword();
            if (password == null) {
                throw new IllegalStateException("Master password is required to access stored credentials.");
            }
            try {
                SecretKeySpec key = deriveKey(password, state.salt());
                verifyState(key, state);
                sessionKey = key;
                return key;
            } catch (GeneralSecurityException e) {
                passwordProvider.showError("The master password is incorrect.");
            } finally {
                Arrays.fill(password, '\0');
            }
        }
        throw new IllegalStateException("Master password verification failed.");
    }

    private SecretKeySpec initializeMasterPassword(Connection connection) {
        char[] password = passwordProvider.createMasterPassword();
        if (password == null) {
            throw new IllegalStateException("Master password creation was canceled.");
        }
        try {
            byte[] salt = randomBytes(SALT_SIZE_BYTES);
            SecretKeySpec key = deriveKey(password, salt);
            byte[] iv = randomBytes(IV_SIZE_BYTES);
            byte[] ciphertext = encrypt(key, iv, VERIFIER_TEXT);
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO credential_store_state (store_id, salt, verifier_iv, verifier_ciphertext)
                    VALUES (1, ?, ?, ?)
                    """)) {
                statement.setBytes(1, salt);
                statement.setBytes(2, iv);
                statement.setBytes(3, ciphertext);
                statement.executeUpdate();
            }
            sessionKey = key;
            return key;
        } catch (SQLException | GeneralSecurityException e) {
            throw new IllegalStateException("Failed to initialize master-password credential store", e);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private StoreState loadState(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT salt, verifier_iv, verifier_ciphertext
                FROM credential_store_state
                WHERE store_id = 1
                """);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return null;
            }
            return new StoreState(
                    resultSet.getBytes("salt"),
                    resultSet.getBytes("verifier_iv"),
                    resultSet.getBytes("verifier_ciphertext")
            );
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load master-password store state", e);
        }
    }

    private void verifyState(SecretKeySpec key, StoreState state) throws GeneralSecurityException {
        byte[] decrypted = decrypt(key, state.verifierIv(), state.verifierCiphertext());
        if (!Arrays.equals(VERIFIER_TEXT, decrypted)) {
            throw new GeneralSecurityException("Invalid master password");
        }
    }

    private SecretKeySpec deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS);
        try {
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return new SecretKeySpec(encoded, "AES");
        } finally {
            spec.clearPassword();
        }
    }

    private byte[] encrypt(SecretKeySpec key, byte[] iv, byte[] plaintext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(plaintext);
    }

    private byte[] decrypt(SecretKeySpec key, byte[] iv, byte[] ciphertext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(ciphertext);
    }

    private byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private void ensureParentDirectory() {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create credential-store directory for " + databasePath, e);
        }
    }

    private void ensureSchema() {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_STATE_TABLE_SQL);
            statement.executeUpdate(CREATE_CREDENTIAL_TABLE_SQL);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize master-password credential store", e);
        }
    }

    private Connection openConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("SQLite JDBC driver is not available", e);
        }
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    private record StoreState(byte[] salt, byte[] verifierIv, byte[] verifierCiphertext) {
    }
}
