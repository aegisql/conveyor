package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.CachingStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

public class JdbcConnectionFactory <T extends DataSource> extends AbstractDataSourceConnectionFactory<T> {

    public JdbcConnectionFactory(T dataSource) {
        super(f->dataSource);
    }

    public JdbcConnectionFactory(Function<AbstractDataSourceConnectionFactory<T>, T> initializer) {
        super(initializer);
    }

    @Override
    public Connection getConnection() {
        try {
            if(connection == null || connection.isClosed()) {
                if(dataSource==null){
                    initDataSource();
                }
                connection = dataSource.getConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    @Override
    public StatementExecutor getStatementExecutor() {
        return new CachingStatementExecutor(this::getConnection);
    }

}
