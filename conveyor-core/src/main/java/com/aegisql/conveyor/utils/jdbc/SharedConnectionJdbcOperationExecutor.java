package com.aegisql.conveyor.utils.jdbc;

import java.sql.Connection;

/**
 * Shared (non-pooled) executor.
 * Keeps one connection for the whole executor lifetime and never closes it.
 */
public class SharedConnectionJdbcOperationExecutor<S> extends AbstractJdbcOperationExecutor<S> {

    private Connection sharedConnection;

    public SharedConnectionJdbcOperationExecutor(JdbcOperationConfig<S> config) {
        super(config);
    }

    @Override
    protected Connection acquireConnection() {
        if (sharedConnection == null) {
            sharedConnection = openConnection();
        }
        return sharedConnection;
    }

    @Override
    protected void releaseConnection(Connection connection) {
        // kept open for reuse
    }
}
