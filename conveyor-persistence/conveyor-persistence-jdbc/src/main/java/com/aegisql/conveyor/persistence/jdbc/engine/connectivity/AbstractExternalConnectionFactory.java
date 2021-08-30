package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.core.PersistenceException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public abstract class AbstractExternalConnectionFactory implements ConnectionFactory {

    protected final Supplier<Connection> supplier;
    protected Connection connection;
    protected String database;
    protected String schema;

    public AbstractExternalConnectionFactory(Supplier<Connection> supplier) {
        this.supplier = supplier;
    }

    @Override
    public Connection getConnection() {
        try {
            if(connection == null || connection.isClosed()) {
                connection = supplier.get();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed checking connection",e);
        }
        return connection;
    }

    @Override
    public void closeConnection() {
        if(connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new PersistenceException("Failed closing connection",e);
            }
        }
    }

    @Override
    public void resetConnection() {
        closeConnection();
        connection = null;
    }

    @Override
    public void setDatabase(String database) {
        this.database = database;
    }

    @Override
    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Override
    public String getDatabase() {
        if(database==null) {
            try {
                database = getConnection().getCatalog();
            } catch (SQLException e) {
                throw new PersistenceException("Cannot retrieve database catalog", e);
            }
        }
        return database;
    }

    @Override
    public String getSchema() {
        if(schema==null) {
            try {
                schema = getConnection().getSchema();
            } catch (SQLException e) {
                throw new PersistenceException("Cannot retrieve database schema", e);
            }
        }
        return schema;
    }
}
