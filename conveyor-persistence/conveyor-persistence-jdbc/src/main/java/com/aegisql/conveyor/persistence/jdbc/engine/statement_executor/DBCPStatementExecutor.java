package com.aegisql.conveyor.persistence.jdbc.engine.statement_executor;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

import java.io.IOException;
import java.sql.SQLException;

public class DBCPStatementExecutor extends AbstractStatementExecutor {


    public DBCPStatementExecutor(ConnectionFactory connection) {
        super(connection);
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
