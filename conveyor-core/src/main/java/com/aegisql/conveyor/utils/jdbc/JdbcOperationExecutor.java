package com.aegisql.conveyor.utils.jdbc;

public interface JdbcOperationExecutor<S> {

    void execute(S source);

    void executeBatch(Iterable<S> sources);
}
