package com.aegisql.conveyor.utils.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public abstract class AbstractJdbcOperationExecutor<S> implements JdbcOperationExecutor<S> {

    private final JdbcOperationConfig<S> config;

    protected AbstractJdbcOperationExecutor(JdbcOperationConfig<S> config) {
        this.config = Objects.requireNonNull(config, "JDBC operation config must be provided");
    }

    @Override
    public void execute(S source) {
        Connection connection = acquireConnection();
        try (PreparedStatement statement = connection.prepareStatement(config.sql())) {
            bind(statement, source);
            statement.executeUpdate();
            commitIfNeeded(connection);
        } catch (SQLException e) {
            rollbackIfNeeded(connection);
            throw new JdbcExecutionException("Failed to execute JDBC statement: " + config.sql(), e);
        } finally {
            releaseConnection(connection);
        }
    }

    @Override
    public void executeBatch(Iterable<S> sources) {
        List<S> collected = new ArrayList<>();
        for (S source : sources) {
            collected.add(source);
        }
        if (collected.isEmpty()) {
            return;
        }

        Connection connection = acquireConnection();
        try (PreparedStatement statement = connection.prepareStatement(config.sql())) {
            for (S source : collected) {
                bind(statement, source);
                statement.addBatch();
            }
            statement.executeBatch();
            commitIfNeeded(connection);
        } catch (SQLException e) {
            rollbackIfNeeded(connection);
            throw new JdbcExecutionException("Failed to execute JDBC batch statement: " + config.sql(), e);
        } finally {
            releaseConnection(connection);
        }
    }

    protected final JdbcOperationConfig<S> config() {
        return config;
    }

    protected abstract Connection acquireConnection();

    protected abstract void releaseConnection(Connection connection);

    protected Connection openConnection() {
        Connection connection = config.connectionSupplier().get();
        if (connection == null) {
            throw new JdbcExecutionException("Connection supplier returned null");
        }
        return connection;
    }

    protected void closeConnection(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new JdbcExecutionException("Failed to close JDBC connection", e);
        }
    }

    private void bind(PreparedStatement statement, S source) throws SQLException {
        List<Function<S, ?>> mappers = config.parameterMappers();
        for (int i = 0; i < mappers.size(); i++) {
            statement.setObject(i + 1, mappers.get(i).apply(source));
        }
    }

    private void commitIfNeeded(Connection connection) {
        try {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new JdbcExecutionException("Failed to commit JDBC transaction", e);
        }
    }

    private void rollbackIfNeeded(Connection connection) {
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (SQLException e) {
            throw new JdbcExecutionException("Failed to rollback JDBC transaction", e);
        }
    }
}
