package com.aegisql.conveyor.persistence.jdbc.engine.statement_executor;

import com.aegisql.conveyor.persistence.core.PersistenceException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The type Abstract statement executor.
 */
public abstract class AbstractStatementExecutor implements StatementExecutor {

    /**
     * The Connection factory.
     */
    protected final Supplier<Connection> connectionFactory;
    /**
     * The Connection.
     */
    protected Connection connection;

    /**
     * Instantiates a new Abstract statement executor.
     *
     * @param connection the connection
     */
    public AbstractStatementExecutor(Supplier<Connection> connection) {
        this.connectionFactory = connection;
    }

    @Override
    public void execute(String sql) {
        connection = connectionFactory.get();
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException e) {
            throw new PersistenceException("Failed executing "+sql+"; "+e.getMessage(),e);
        }
    }

    @Override
    public void execute(String sql, Consumer<PreparedStatement> consumer) {
        connection = connectionFactory.get();
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            consumer.accept(statement);
            statement.execute();
        } catch (SQLException e) {
            throw new PersistenceException("Failed executing "+sql+"; "+e.getMessage(),e);
        }
    }

    @Override
    public void executeUpdate(String sql) {
        connection = connectionFactory.get();
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed executing "+sql+"; "+e.getMessage(),e);
        }
    }

    @Override
    public <T> T fetchOne(String sql, Consumer<PreparedStatement> consumer, Function<ResultSet,T> transformer) {
        connection = connectionFactory.get();
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            consumer.accept(statement);
            T t = null;
            try (ResultSet rs =statement.executeQuery() ) {
                while (rs.next()) {
                    if (t != null) {
                        throw new PersistenceException("Expected single object for " + t);
                    }
                    t = transformer.apply(rs);
                }
            }
            return t;
        } catch (SQLException e) {
            throw new PersistenceException("Failed executing "+sql+"; "+e.getMessage(),e);
        }
    }

    @Override
    public <T> List<T> fetchMany(String sql, Consumer<PreparedStatement> consumer, Function<ResultSet,T> transformer) {
        connection = connectionFactory.get();
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            consumer.accept(statement);
            List<T> list = new ArrayList<>();
            try (ResultSet rs =statement.executeQuery() ) {
                while (rs.next()) {
                    T t = transformer.apply(rs);
                    list.add(t);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Failed executing "+sql+"; "+e.getMessage(),e);
        }
    }


    @Override
    public void meta(Consumer<DatabaseMetaData> metaDataConsumer) {
        connection = connectionFactory.get();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            metaDataConsumer.accept(metaData);
        } catch (SQLException e) {
            throw new PersistenceException("Failed processing metadata",e);
        }
    }
}
