package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * The type Wrapped external data source.
 */
public class WrappedExternalDataSource implements DataSource {

    private final Supplier<Connection> connectionSupplier;
    private PrintWriter logWriter;

    /**
     * Instantiates a new Wrapped external data source.
     *
     * @param connectionSupplier the connection supplier
     */
    public WrappedExternalDataSource(Supplier<Connection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connectionSupplier.get();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return connectionSupplier.get();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logWriter = out;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Unsupported getParentLogger");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
