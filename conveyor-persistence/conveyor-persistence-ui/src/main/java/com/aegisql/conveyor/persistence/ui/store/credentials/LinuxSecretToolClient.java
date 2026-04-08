package com.aegisql.conveyor.persistence.ui.store.credentials;

import java.util.Optional;

interface LinuxSecretToolClient {

    Optional<String> load(long profileId, CredentialKind credentialKind);

    void save(long profileId, CredentialKind credentialKind, String secret);

    void delete(long profileId, CredentialKind credentialKind);
}
