package com.aegisql.conveyor.persistence.jdbc.engine.statement_executor;

import com.aegisql.conveyor.persistence.core.PersistenceException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * The type Non caching statement executor.
 */
public class NonCachingStatementExecutor extends AbstractStatementExecutor {


    /**
     * Instantiates a new Non caching statement executor.
     *
     * @param connectionSupplier the connection supplier
     */
    public NonCachingStatementExecutor(Supplier<Connection> connectionSupplier) {
        super(connectionSupplier);
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new PersistenceException("Failed closing connection",e);
        }
    }
}
