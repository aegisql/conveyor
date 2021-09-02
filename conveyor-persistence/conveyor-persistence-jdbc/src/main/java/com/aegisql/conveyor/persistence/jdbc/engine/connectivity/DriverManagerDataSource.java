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

public class DriverManagerDataSource implements DataSource {

    private String driverClassName = "";
    private Properties properties = new Properties();
    private String url;

    private PrintWriter logWriter;

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

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void addProperty(String key, String value) {
        properties.put(key,value);
    }

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
