package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public abstract class AbstractExternalConnectionFactory implements ConnectionFactory {

    private final Supplier<Connection> supplier;
    private Connection connection;

    public AbstractExternalConnectionFactory(Supplier<Connection> supplier) {
        this.supplier = supplier;
    }

    @Override
    public Connection getConnection() {
        try {
            if(connection == null || connection.isClosed()) {
                connection = supplier.get();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed checking connection",e);
        }
        return connection;
    }

    @Override
    public StatementExecutor getStatementExecutor() {
        return null;
    }

    @Override
    public void closeConnection() {
        if(connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new PersistenceException("Failed closing connection",e);
            }
        }
    }

    @Override
    public void resetConnection() {
        closeConnection();
        connection = null;
    }
}
