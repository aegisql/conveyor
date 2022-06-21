package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
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
    public Connection getConnection() {
        return connectionSupplier.get();
    }

    @Override
    public Connection getConnection(String username, String password) {
        return connectionSupplier.get();
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public void setLoginTimeout(int seconds) {

    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Unsupported getParentLogger");
    }

    @Override
    public <T> T unwrap(Class<T> iface) {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }
}
