package com.aegisql.conveyor.persistence.jdbc.engine.statement_executor;

import java.io.Closeable;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The interface Statement executor.
 */
public interface StatementExecutor extends Closeable {
    /**
     * Execute.
     *
     * @param sql the sql
     */
    void execute(String sql);

    /**
     * Execute.
     *
     * @param sql      the sql
     * @param consumer the consumer
     */
    void execute(String sql, Consumer<PreparedStatement> consumer);

    /**
     * Execute update.
     *
     * @param sql the sql
     */
    void executeUpdate(String sql);

    /**
     * Fetch one t.
     *
     * @param <T>         the type parameter
     * @param sql         the sql
     * @param function    the function
     * @param transformer the transformer
     * @return the t
     */
    <T> T fetchOne(String sql, Consumer<PreparedStatement> function, Function<ResultSet,T> transformer);

    /**
     * Fetch many list.
     *
     * @param <T>         the type parameter
     * @param sql         the sql
     * @param function    the function
     * @param transformer the transformer
     * @return the list
     */
    <T> List<T> fetchMany(String sql, Consumer<PreparedStatement> function, Function<ResultSet,T> transformer);

    /**
     * Meta.
     *
     * @param metaDataConsumer the meta data consumer
     */
    void meta(Consumer<DatabaseMetaData> metaDataConsumer);
}
