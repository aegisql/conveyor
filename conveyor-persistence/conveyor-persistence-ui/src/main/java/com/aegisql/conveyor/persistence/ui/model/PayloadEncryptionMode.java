package com.aegisql.conveyor.persistence.ui.model;

public enum PayloadEncryptionMode {
    NONE("None"),
    MANAGED_DEFAULT("Managed AES/GCM (default)"),
    LEGACY_AES_ECB("Legacy AES/ECB");

    private final String displayName;

    PayloadEncryptionMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
