package com.aegisql.conveyor.persistence.ui.store.credentials;

import java.util.Optional;

public interface CredentialStore {

    default String displayName() {
        return "Secure Credential Store";
    }

    Optional<String> load(long profileId, CredentialKind credentialKind);

    void save(long profileId, CredentialKind credentialKind, String secret);

    void delete(long profileId, CredentialKind credentialKind);
}
