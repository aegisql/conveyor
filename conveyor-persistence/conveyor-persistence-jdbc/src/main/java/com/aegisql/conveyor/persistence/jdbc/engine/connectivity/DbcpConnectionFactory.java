package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.DBCPStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;

import javax.sql.DataSource;

public class DbcpConnectionFactory<T extends DataSource> extends AbstractDataSourceConnectionFactory<T> {

    public DbcpConnectionFactory(T dataSource) {
        super(dataSource);
    }

    @Override
    public StatementExecutor getStatementExecutor() {
        return new DBCPStatementExecutor(this);
    }

    @Override
    public ConnectionFactory copy() {
        DbcpConnectionFactory next = new DbcpConnectionFactory(this.getDataSource());
        copyThisToOther(next);
        return next;
    }

}
