package com.aegisql.conveyor.persistence.ui.store.credentials;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinuxSecretToolCredentialStoreTest {

    @Test
    void savesLoadsAndDeletesWithSecretToolClient() {
        InMemoryLinuxSecretToolClient client = new InMemoryLinuxSecretToolClient();
        LinuxSecretToolCredentialStore store = new LinuxSecretToolCredentialStore(client);

        store.save(9L, CredentialKind.PASSWORD, "linux-secret");

        assertEquals(Optional.of("linux-secret"), store.load(9L, CredentialKind.PASSWORD));

        store.delete(9L, CredentialKind.PASSWORD);

        assertEquals(Optional.empty(), store.load(9L, CredentialKind.PASSWORD));
    }

    private static final class InMemoryLinuxSecretToolClient implements LinuxSecretToolClient {
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
