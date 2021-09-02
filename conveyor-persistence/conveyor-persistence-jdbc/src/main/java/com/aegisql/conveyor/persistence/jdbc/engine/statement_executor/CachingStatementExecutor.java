package com.aegisql.conveyor.persistence.jdbc.engine.statement_executor;

import java.io.IOException;
import java.sql.Connection;
import java.util.function.Supplier;

public class CachingStatementExecutor extends AbstractStatementExecutor {

    public CachingStatementExecutor(Supplier<Connection> connectionSupplier) {
        super(connectionSupplier);
    }


    @Override
    public void close() throws IOException {
        // just do nothings.
        // close with the connection factory close method
        // if needed
    }

}
