package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.DBCPStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

public class DbcpConnectionFactory<T extends DataSource> extends AbstractDataSourceConnectionFactory<T> {

    public DbcpConnectionFactory(T dataSource) {
        super(f->dataSource);
    }

    public DbcpConnectionFactory(Function<AbstractDataSourceConnectionFactory<T>, T> initializer) {
        super(initializer);
    }

    @Override
    public Connection getConnection() {
        try {
                initDataSource();
                return dataSource.getConnection();
//                System.err.println("Created connection "+connection.getClass().getSimpleName()+" "+connection.getMetaData().getURL());
        } catch (SQLException e) {
            throw new PersistenceException("Failed obtaining connection",e);
        }
    }

    @Override
    public void resetConnection() {
        super.resetConnection();
        //dataSource = null;
    }

    @Override
    public void closeConnection() {
        super.closeConnection();
        connection = null;
    }

    @Override
    public StatementExecutor getStatementExecutor() {
        return new DBCPStatementExecutor(this);
    }

}
