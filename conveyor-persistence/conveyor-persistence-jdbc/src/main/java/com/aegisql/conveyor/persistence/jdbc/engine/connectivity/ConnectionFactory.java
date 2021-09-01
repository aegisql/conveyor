package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;

import java.sql.Connection;

/**
 * The interface Connection factory.
 */
public interface ConnectionFactory {
    /**
     * Gets connection.
     *
     * @return the connection
     */
    Connection getConnection();

    /**
     * Gets statement executor.
     *
     * @return the statement executor
     */
    StatementExecutor getStatementExecutor();

    /**
     * Close connection. calls connection.close()
     */
    void closeConnection();

    /**
     * Reset connection. closes connection and force re-initialization when next time connection is requested.
     */
    void resetConnection();

    /**
     * Gets database.
     *
     * @return the database
     */
    String getDatabase();

    /**
     * Gets schema.
     *
     * @return the schema
     */
    String getSchema();

    /**
     * Sets database.
     *
     * @param database the database
     */
    void setDatabase(String database);

    /**
     * Sets schema.
     *
     * @param schema the schema
     */
    void setSchema(String schema);
}
