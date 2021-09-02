package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.CachingStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.NonCachingStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConnectionFactory <T extends DataSource> {

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
    protected T dataSource;
    protected Function<ConnectionFactory<T>,T> dataSourceInitializer = thiz->dataSource;

    protected Supplier<Connection> cachingConnectionSupplier = ()->{
        try {
            if(connection==null || connection.isClosed()) {
                if(dataSource == null) {
                    dataSource = dataSourceInitializer.apply(this);
                }
                connection = dataSource.getConnection();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed obtaining connection",e);
        }
        return connection;
    };
    protected Supplier<Connection> nonCachingConnectionSupplier = ()->{
        if(dataSource == null) {
            dataSource = dataSourceInitializer.apply(this);
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new PersistenceException("Failed obtaining connection",e);
        }
    };

    protected Supplier<Connection> connectionSupplier;

    public final Supplier<StatementExecutor> cachingExecutorSupplier = ()->new CachingStatementExecutor(this::getConnection);
    public final Supplier<StatementExecutor> nonCachingExecutorSupplier = ()->new NonCachingStatementExecutor(this::getConnection);
    protected Supplier<StatementExecutor> executorSupplier;

    private ConnectionFactory(Function<ConnectionFactory<T>,T> dataSourceInitializer) {
        this.dataSourceInitializer = dataSourceInitializer;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public static ConnectionFactory<DriverManagerDataSource> driverManagerFactoryInstance() {
        ConnectionFactory<DriverManagerDataSource> cf = cachingFactoryInstance(f->{
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(f.driverClassName);
            dataSource.setUrl(f.getUrl());
            dataSource.setProperties(f.getProperties());
            return dataSource;
        });
        return cf;
    }

    public static ConnectionFactory<BasicDataSource> DBCP2FactoryInstance() {
        ConnectionFactory<BasicDataSource> cf = nonCachingFactoryInstance(f->{
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setUrl(f.getUrl());
            dataSource.setDriverClassName(f.getDriverClassName());
            dataSource.setUsername(f.getUser());
            dataSource.setPassword(f.getPassword());
            dataSource.setInitialSize(3);
            return dataSource;
        });
        return cf;
    }

    public static ConnectionFactory<WrappedExternalDataSource> nonCachingExternalConnectionFactoryInstance(Supplier<Connection> connectionSupplier) {
        ConnectionFactory<WrappedExternalDataSource> cf = nonCachingFactoryInstance(f->{
            WrappedExternalDataSource dataSource = new WrappedExternalDataSource(connectionSupplier);
            return dataSource;
        });
        return cf;
    }

    public static ConnectionFactory<WrappedExternalDataSource> cachingExternalConnectionFactoryInstance(Supplier<Connection> connectionSupplier) {
        ConnectionFactory<WrappedExternalDataSource> cf = cachingFactoryInstance(f->{
            WrappedExternalDataSource dataSource = new WrappedExternalDataSource(connectionSupplier);
            return dataSource;
        });
        return cf;
    }

    public static <T extends DataSource> ConnectionFactory<T> cachingFactoryInstance(Function<ConnectionFactory<T>,T> dataSourceInitializer) {
        ConnectionFactory<T> cf = new ConnectionFactory<>(dataSourceInitializer);
            cf.connectionSupplier = cf.cachingConnectionSupplier;
            cf.executorSupplier = cf.cachingExecutorSupplier;
        return cf;
    }

    public static <T extends DataSource> ConnectionFactory<T> nonCachingFactoryInstance(Function<ConnectionFactory<T>,T> dataSourceInitializer) {
        ConnectionFactory<T> cf = new ConnectionFactory<>(dataSourceInitializer);
        cf.connectionSupplier = cf.nonCachingConnectionSupplier;
        cf.executorSupplier = cf.nonCachingExecutorSupplier;
        return cf;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        resetConnection();
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

    public void addProperty(String key, String value) {
        this.properties.put(key,value);
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public String getUrl() {
        if (notBlank(url)) {
            return url;
        } else {
            url =  urlTemplate.replace("{host}", host == null ? "" : host)
                    .replace("{port}", "" + port)
                    .replace("{database}", database == null ? "" : database)
                    .replace("{schema}", schema == null ? "" : schema)
                    .replace("{user}", user == null ? "" : user)
                    .replace("{password}", password == null ? "" : password);
            return url;
        }
    }

    protected static boolean notBlank(String str) {
        return str != null && ! str.isBlank();
    }

    protected static boolean blank(String str) {
        return str == null || str.isBlank();
    }

    public Connection getConnection() {
        return connectionSupplier.get();
    }

    public StatementExecutor getStatementExecutor() {
        return executorSupplier.get();
    }

    public void closeConnection() {
        if(connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new PersistenceException("Failed closing connection",e);
            }
        }
    }

    public void resetConnection() {
        closeConnection();
        dataSource = null;
        connection = null;
        url = null;
    }

}
