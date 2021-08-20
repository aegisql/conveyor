package com.aegisql.conveyor.persistence.jdbc.engine.statement_executor;

import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

import java.io.IOException;

public class JdbcStatementExecutor extends AbstractStatementExecutor {

    public JdbcStatementExecutor(ConnectionFactory connection) {
        super(connection);
    }


    @Override
    public void close() throws IOException {

    }

}
