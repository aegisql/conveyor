package com.aegisql.conveyor.persistence.ui.store.credentials;

public enum CredentialKind {
    PASSWORD("password"),
    ENCRYPTION_SECRET("encryption-secret");

    private final String storageKey;

    CredentialKind(String storageKey) {
        this.storageKey = storageKey;
    }

    public String storageKey() {
        return storageKey;
    }
}
