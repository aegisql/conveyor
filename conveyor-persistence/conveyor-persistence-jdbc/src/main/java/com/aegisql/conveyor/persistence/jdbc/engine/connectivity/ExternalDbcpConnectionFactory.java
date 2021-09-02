package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.NonCachingStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;

import java.sql.Connection;
import java.util.function.Supplier;

public class ExternalDbcpConnectionFactory extends AbstractExternalConnectionFactory{

    public ExternalDbcpConnectionFactory(Supplier<Connection> supplier) {
        super(supplier);
    }

    @Override
    public StatementExecutor getStatementExecutor() {
        return new NonCachingStatementExecutor(this::getConnection);
    }

}
