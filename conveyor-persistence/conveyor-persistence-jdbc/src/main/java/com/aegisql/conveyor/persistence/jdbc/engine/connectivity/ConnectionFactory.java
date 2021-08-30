package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;

import java.sql.Connection;

public interface ConnectionFactory {
    Connection getConnection();
    StatementExecutor getStatementExecutor();
    void closeConnection();
    void resetConnection();
    String getDatabase();
    String getSchema();
    void setDatabase(String database);
    void setSchema(String schema);
    ConnectionFactory copy();
}
