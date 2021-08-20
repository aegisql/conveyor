package com.aegisql.conveyor.persistence.jdbc.engine.statement_executor;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractStatementExecutor implements StatementExecutor {

    protected final ConnectionFactory connectionFactory;

    public AbstractStatementExecutor(ConnectionFactory connection) {
        this.connectionFactory = connection;
    }

    @Override
    public void execute(String sql) {
        try(PreparedStatement statement = connectionFactory.getConnection().prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException e) {
            throw new PersistenceException("Failed executing "+sql+"; "+e.getMessage(),e);
        }
    }

    @Override
    public void execute(String sql, Consumer<PreparedStatement> consumer) {
        try(PreparedStatement statement = connectionFactory.getConnection().prepareStatement(sql)) {
            consumer.accept(statement);
        } catch (SQLException e) {
            throw new PersistenceException("Failed executing "+sql+"; "+e.getMessage(),e);
        }
    }

    @Override
    public <T> T execute(String sql, Function<PreparedStatement, T> function) {
        try(PreparedStatement statement = connectionFactory.getConnection().prepareStatement(sql)) {
            return function.apply(statement);
        } catch (SQLException e) {
            throw new PersistenceException("Failed executing "+sql+"; "+e.getMessage(),e);
        }
    }


}
