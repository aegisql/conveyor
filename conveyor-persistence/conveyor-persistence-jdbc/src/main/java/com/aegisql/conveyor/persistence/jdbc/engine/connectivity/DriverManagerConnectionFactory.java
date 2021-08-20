package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.JdbcStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DriverManagerConnectionFactory extends AbstractConnectionFactory {
    @Override
    public Connection getConnection() {
        try {
            if(connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(getUrl(), getProperties());
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed get connection for "+getUrlTemplate()+" properties:"+properties, e);
        }
        return connection;
    }

    @Override
    public StatementExecutor getStatementExecutor() {
        return new JdbcStatementExecutor(this);
    }

}
