package com.aegisql.conveyor.persistence.jdbc.engine.statement_executor;

import java.io.Closeable;
import java.sql.PreparedStatement;
import java.util.function.Consumer;
import java.util.function.Function;

public interface StatementExecutor extends Closeable {
    void execute(String sql);
    void execute(String sql, Consumer<PreparedStatement> consumer);
    <T> T execute(String sql, Function<PreparedStatement,T> function);
}
