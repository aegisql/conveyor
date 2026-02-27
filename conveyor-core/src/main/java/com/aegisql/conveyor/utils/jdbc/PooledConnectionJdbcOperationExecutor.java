package com.aegisql.conveyor.utils.jdbc;

import java.sql.Connection;

/**
 * Pooled executor.
 * Acquires and closes a connection on every operation/batch call.
 */
public class PooledConnectionJdbcOperationExecutor<S> extends AbstractJdbcOperationExecutor<S> {

    public PooledConnectionJdbcOperationExecutor(JdbcOperationConfig<S> config) {
        super(config);
    }

    @Override
    protected Connection acquireConnection() {
        return openConnection();
    }

    @Override
    protected void releaseConnection(Connection connection) {
        closeConnection(connection);
    }
}
