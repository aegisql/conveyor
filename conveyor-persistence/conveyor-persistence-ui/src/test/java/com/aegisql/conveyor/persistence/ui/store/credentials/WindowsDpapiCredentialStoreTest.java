package com.aegisql.conveyor.persistence.ui.store.credentials;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowsDpapiCredentialStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void savesLoadsAndDeletesProtectedCredentials() {
        WindowsDpapiCredentialStore store = new WindowsDpapiCredentialStore(
                tempDir.resolve("profiles.db"),
                new PrefixingWindowsDataProtector()
        );

        store.save(11L, CredentialKind.PASSWORD, "windows-secret");

        assertEquals(Optional.of("windows-secret"), store.load(11L, CredentialKind.PASSWORD));

        WindowsDpapiCredentialStore reloaded = new WindowsDpapiCredentialStore(
                tempDir.resolve("profiles.db"),
                new PrefixingWindowsDataProtector()
        );
        assertEquals(Optional.of("windows-secret"), reloaded.load(11L, CredentialKind.PASSWORD));

        reloaded.delete(11L, CredentialKind.PASSWORD);
        assertEquals(Optional.empty(), reloaded.load(11L, CredentialKind.PASSWORD));
    }

    private static final class PrefixingWindowsDataProtector implements WindowsDataProtector {
        @Override
        public String protect(String secret) {
            return "dpapi::" + secret;
        }

        @Override
        public String unprotect(String protectedSecret) {
            return protectedSecret.replaceFirst("^dpapi::", "");
        }
    }
}
