package com.aegisql.conveyor.persistence.jdbc.engine.statement_executor;

import com.aegisql.conveyor.persistence.core.PersistenceException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public class NonCachingStatementExecutor extends AbstractStatementExecutor {


    public NonCachingStatementExecutor(Supplier<Connection> connectionSupplier) {
        super(connectionSupplier);
    }

    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new PersistenceException("Failed closing connection",e);
        }
    }
}
