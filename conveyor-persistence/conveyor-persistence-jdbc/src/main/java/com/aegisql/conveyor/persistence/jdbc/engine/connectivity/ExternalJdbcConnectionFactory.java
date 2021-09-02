package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.CachingStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;

import java.sql.Connection;
import java.util.function.Supplier;

public class ExternalJdbcConnectionFactory extends AbstractExternalConnectionFactory{

    public ExternalJdbcConnectionFactory(Supplier<Connection> supplier) {
        super(supplier);
    }

    @Override
    public StatementExecutor getStatementExecutor() {
        return new CachingStatementExecutor(this::getConnection);
    }

}
