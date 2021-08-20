package com.aegisql.conveyor.persistence.jdbc.engine.statement_executor;

import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

import java.io.IOException;

public class DBCPStatementExecutor extends AbstractStatementExecutor {


    public DBCPStatementExecutor(ConnectionFactory connection) {
        super(connection);
    }


    @Override
    public void close() throws IOException {
        connectionFactory.resetConnection();
    }
}
