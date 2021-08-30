package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.DBCPStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;

public class DBCP2ConnectionFactory extends AbstractDataSourceConnectionFactory<BasicDataSource> {

    public DBCP2ConnectionFactory() {
        super(new BasicDataSource());
    }

    @Override
    public Connection getConnection() {
        if(notBlank(driverClassName)) {
            dataSource.setDriverClassName(getDriverClassName());
        }
        dataSource.setUrl(getUrl());
        if(properties != null) {
            properties.forEach((k,v)->dataSource.addConnectionProperty(k.toString(),v==null?null:v.toString()));
        }
        return super.getConnection();
    }

    @Override
    public StatementExecutor getStatementExecutor() {
        return new DBCPStatementExecutor(this);
    }

    @Override
    public ConnectionFactory copy() {
        DBCP2ConnectionFactory next = new DBCP2ConnectionFactory();
        copyThisToOther(next);
        return next;
    }
}
