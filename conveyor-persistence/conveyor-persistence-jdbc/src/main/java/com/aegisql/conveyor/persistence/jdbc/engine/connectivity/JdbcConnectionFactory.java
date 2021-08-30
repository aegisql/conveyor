package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.JdbcStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;

import javax.sql.DataSource;

public class JdbcConnectionFactory <T extends DataSource> extends AbstractDataSourceConnectionFactory<T> {

    public JdbcConnectionFactory(T dataSource) {
        super(dataSource);
    }

    @Override
    public StatementExecutor getStatementExecutor() {
        return new JdbcStatementExecutor(this);
    }

    @Override
    public ConnectionFactory copy() {
        JdbcConnectionFactory next = new JdbcConnectionFactory(this.getDataSource());
        copyThisToOther(next);
        return next;
    }

}
