package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.core.PersistenceException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public abstract class AbstractConnectionFactory implements ConnectionFactory {

    protected String driverClassName;
    protected String urlTemplate;
    protected String url;
    protected String host;
    protected int port;
    protected String user;
    protected String password;
    protected String database;
    protected String schema;
    protected Properties properties = new Properties();

    protected Connection connection;

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        try {
            if (notBlank(driverClassName)) {
                Class.forName(driverClassName);
                this.driverClassName = driverClassName;
            }
        } catch (ClassNotFoundException e) {
            throw new PersistenceException("Driver not found: " + driverClassName, e);
        }
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    protected String getUrl() {
        if (notBlank(url)) {
            return url;
        } else {
            url =  urlTemplate.replace("{host}", host == null ? "" : host).replace("{port}", "" + port)
                    .replace("{database}", database == null ? "" : database)
                    .replace("{schema}", schema == null ? "" : schema).replace("{user}", user == null ? "" : user)
                    .replace("{password}", password == null ? "" : password);
            return url;
        }
    }

    public void setUrl(String url) {
        this.url = url;
    }

    protected static boolean notBlank(String str) {
        return str != null && ! str.isBlank();
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
}
