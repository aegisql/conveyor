package com.aegisql.conveyor.persistence.ui.store.credentials;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MasterPasswordCredentialStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void encryptsAndLoadsStoredCredentials() {
        FixedMasterPasswordProvider passwordProvider = new FixedMasterPasswordProvider("let-me-in".toCharArray());
        MasterPasswordCredentialStore store = new MasterPasswordCredentialStore(tempDir.resolve("profiles.db"), passwordProvider);

        store.save(7L, CredentialKind.PASSWORD, "secret-value");

        assertEquals(Optional.of("secret-value"), store.load(7L, CredentialKind.PASSWORD));

        MasterPasswordCredentialStore reloaded = new MasterPasswordCredentialStore(tempDir.resolve("profiles.db"), passwordProvider);
        assertEquals(Optional.of("secret-value"), reloaded.load(7L, CredentialKind.PASSWORD));

        reloaded.delete(7L, CredentialKind.PASSWORD);
        assertEquals(Optional.empty(), reloaded.load(7L, CredentialKind.PASSWORD));
    }

    private static final class FixedMasterPasswordProvider implements MasterPasswordProvider {
        private final char[] password;

        private FixedMasterPasswordProvider(char[] password) {
            this.password = password;
        }

        @Override
        public char[] createMasterPassword() {
            return password.clone();
        }

        @Override
        public char[] requestMasterPassword() {
            return password.clone();
        }

        @Override
        public void showError(String message) {
            throw new AssertionError("Unexpected master-password error: " + message);
        }
    }
}
