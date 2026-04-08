package com.aegisql.conveyor.persistence.ui.store.credentials;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CredentialStoresTest {

    @TempDir
    Path tempDir;

    @Test
    void prefersMacKeychainWhenAvailable() {
        CredentialStore nativeStore = new MarkerCredentialStore();
        CredentialStore store = CredentialStores.defaultStore(
                tempDir.resolve("profiles.db"),
                "Mac OS X",
                new StubMasterPasswordProvider(),
                true,
                false,
                false,
                () -> nativeStore,
                MarkerCredentialStore::new,
                MarkerCredentialStore::new
        );

        assertInstanceOf(MarkerCredentialStore.class, store);
    }

    @Test
    void prefersWindowsDpapiWhenAvailable() {
        CredentialStore nativeStore = new MarkerCredentialStore();
        CredentialStore store = CredentialStores.defaultStore(
                tempDir.resolve("profiles.db"),
                "Windows 11",
                new StubMasterPasswordProvider(),
                false,
                true,
                false,
                MarkerCredentialStore::new,
                () -> nativeStore,
                MarkerCredentialStore::new
        );

        assertInstanceOf(MarkerCredentialStore.class, store);
    }

    @Test
    void prefersLinuxSecretServiceWhenAvailable() {
        CredentialStore nativeStore = new MarkerCredentialStore();
        CredentialStore store = CredentialStores.defaultStore(
                tempDir.resolve("profiles.db"),
                "Linux",
                new StubMasterPasswordProvider(),
                false,
                false,
                true,
                MarkerCredentialStore::new,
                MarkerCredentialStore::new,
                () -> nativeStore
        );

        assertInstanceOf(MarkerCredentialStore.class, store);
    }

    @Test
    void fallsBackToMasterPasswordStoreWhenNoNativeStoreIsAvailable() {
        CredentialStore store = CredentialStores.defaultStore(
                tempDir.resolve("profiles.db"),
                "Linux",
                new StubMasterPasswordProvider(),
                false,
                false,
                false,
                MarkerCredentialStore::new,
                MarkerCredentialStore::new,
                MarkerCredentialStore::new
        );

        assertInstanceOf(MasterPasswordCredentialStore.class, store);
    }

    private static final class StubMasterPasswordProvider implements MasterPasswordProvider {
        @Override
        public char[] createMasterPassword() {
            return "test-password".toCharArray();
        }

        @Override
        public char[] requestMasterPassword() {
            return "test-password".toCharArray();
        }

        @Override
        public void showError(String message) {
            throw new AssertionError("Unexpected master-password error: " + message);
        }
    }

    private static final class MarkerCredentialStore implements CredentialStore {
        @Override
        public java.util.Optional<String> load(long profileId, CredentialKind credentialKind) {
            return java.util.Optional.empty();
        }

        @Override
        public void save(long profileId, CredentialKind credentialKind, String secret) {
        }

        @Override
        public void delete(long profileId, CredentialKind credentialKind) {
        }
    }
}
