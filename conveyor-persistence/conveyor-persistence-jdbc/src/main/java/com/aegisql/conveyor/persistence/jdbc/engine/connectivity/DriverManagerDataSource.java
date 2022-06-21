package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.core.PersistenceException;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * The type Driver manager data source.
 */
public class DriverManagerDataSource implements DataSource {

    private String driverClassName = "";
    private Properties properties = new Properties();
    private String url;

    private PrintWriter logWriter;

    /**
     * Sets driver class name.
     *
     * @param driverClassName the driver class name
     */
    public void setDriverClassName(String driverClassName) {
        try {
            if (driverClassName != null && ! driverClassName.isBlank()) {
                Class.forName(driverClassName);
                this.driverClassName = driverClassName;
            }
        } catch (ClassNotFoundException e) {
            throw new PersistenceException("Driver not found: " + driverClassName, e);
        }
    }

    /**
     * Sets properties.
     *
     * @param properties the properties
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Add property.
     *
     * @param key   the key
     * @param value the value
     */
    public void addProperty(String key, String value) {
        properties.put(key,value);
    }

    /**
     * Sets url.
     *
     * @param url the url
     */
    public void setUrl(String url) {
        Objects.requireNonNull(url,"URL is null");
        this.url = url;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, properties);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(url,username,password);
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DriverManagerDataSource{");
        sb.append("driverClassName='").append(driverClassName).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append(", properties=").append(properties);
        sb.append('}');
        return sb.toString();
    }
}
