package com.aegisql.conveyor.persistence.ui.model;

public enum ConnectionStatus {
    FAILED(false, false),
    CONNECTED_UNINITIALIZED(true, false),
    READY(true, true);

    private final boolean connected;
    private final boolean initialized;

    ConnectionStatus(boolean connected, boolean initialized) {
        this.connected = connected;
        this.initialized = initialized;
    }

    public boolean connected() {
        return connected;
    }

    public boolean initialized() {
        return initialized;
    }
}
