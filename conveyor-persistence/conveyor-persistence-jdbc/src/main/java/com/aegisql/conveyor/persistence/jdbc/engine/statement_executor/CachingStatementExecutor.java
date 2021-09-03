package com.aegisql.conveyor.persistence.jdbc.engine.statement_executor;

import java.io.IOException;
import java.sql.Connection;
import java.util.function.Supplier;

/**
 * The type Caching statement executor.
 */
public class CachingStatementExecutor extends AbstractStatementExecutor {

    /**
     * Instantiates a new Caching statement executor.
     *
     * @param connectionSupplier the connection supplier
     */
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
