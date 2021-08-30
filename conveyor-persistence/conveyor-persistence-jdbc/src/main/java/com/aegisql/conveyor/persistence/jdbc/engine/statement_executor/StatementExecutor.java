package com.aegisql.conveyor.persistence.jdbc.engine.statement_executor;

import java.io.Closeable;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface StatementExecutor extends Closeable {
    void execute(String sql);
    void execute(String sql, Consumer<PreparedStatement> consumer);
    void executeUpdate(String sql);
    <T> T fetchOne(String sql, Consumer<PreparedStatement> function, Function<ResultSet,T> transformer);
    <T> List<T> fetchMany(String sql, Consumer<PreparedStatement> function, Function<ResultSet,T> transformer);
    void meta(Consumer<DatabaseMetaData> metaDataConsumer);
}
