package com.aegisql.conveyor.persistence.ui.store.credentials;

import java.util.Objects;
import java.util.Optional;

final class LinuxSecretToolCredentialStore implements CredentialStore {

    private final LinuxSecretToolClient secretToolClient;

    LinuxSecretToolCredentialStore() {
        this(new ProcessLinuxSecretToolClient());
    }

    LinuxSecretToolCredentialStore(LinuxSecretToolClient secretToolClient) {
        this.secretToolClient = Objects.requireNonNull(secretToolClient, "secretToolClient must not be null");
    }

    static boolean isSupported() {
        return ProcessLinuxSecretToolClient.isSupported();
    }

    @Override
    public String displayName() {
        return "Linux Secret Service";
    }

    @Override
    public Optional<String> load(long profileId, CredentialKind credentialKind) {
        return secretToolClient.load(profileId, credentialKind);
    }

    @Override
    public void save(long profileId, CredentialKind credentialKind, String secret) {
        secretToolClient.save(profileId, credentialKind, secret);
    }

    @Override
    public void delete(long profileId, CredentialKind credentialKind) {
        secretToolClient.delete(profileId, credentialKind);
    }
}
