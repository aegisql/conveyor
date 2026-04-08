package com.aegisql.conveyor.persistence.ui.store.credentials;

public interface MasterPasswordProvider {

    char[] createMasterPassword();

    char[] requestMasterPassword();

    void showError(String message);
}
