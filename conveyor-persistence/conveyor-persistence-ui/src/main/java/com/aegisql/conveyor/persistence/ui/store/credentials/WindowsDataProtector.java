package com.aegisql.conveyor.persistence.ui.store.credentials;

interface WindowsDataProtector {

    String protect(String secret);

    String unprotect(String protectedSecret);
}
