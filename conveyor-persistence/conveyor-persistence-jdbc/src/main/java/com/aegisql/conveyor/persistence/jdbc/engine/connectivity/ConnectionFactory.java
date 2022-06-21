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

/**
 * The type Connection factory.
 *
 * @param <T> the type parameter
 */
public class ConnectionFactory <T extends DataSource> {

    /**
     * The Driver class name.
     */
    protected String driverClassName;
    /**
     * The Url template.
     */
    protected String urlTemplate;
    /**
     * The Url.
     */
    protected String url;
    /**
     * The Host.
     */
    protected String host;
    /**
     * The Port.
     */
    protected int port;
    /**
     * The User.
     */
    protected String user;
    /**
     * The Password.
     */
    protected String password;
    /**
     * The Database.
     */
    protected String database;
    /**
     * The Schema.
     */
    protected String schema;
    /**
     * The Properties.
     */
    protected Properties properties = new Properties();

    /**
     * The Connection.
     */
    protected Connection connection;
    /**
     * The Data source.
     */
    protected T dataSource;
    /**
     * The Data source initializer.
     */
    protected Function<ConnectionFactory<T>,T> dataSourceInitializer = thiz->dataSource;

    /**
     * The Caching connection supplier.
     */
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
    /**
     * The Non caching connection supplier.
     */
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

    /**
     * The Connection supplier.
     */
    protected Supplier<Connection> connectionSupplier;

    /**
     * The Caching executor supplier.
     */
    public final Supplier<StatementExecutor> cachingExecutorSupplier = ()->new CachingStatementExecutor(this::getConnection);
    /**
     * The Non caching executor supplier.
     */
    public final Supplier<StatementExecutor> nonCachingExecutorSupplier = ()->new NonCachingStatementExecutor(this::getConnection);
    /**
     * The Executor supplier.
     */
    protected Supplier<StatementExecutor> executorSupplier;

    private ConnectionFactory(Function<ConnectionFactory<T>,T> dataSourceInitializer) {
        this.dataSourceInitializer = dataSourceInitializer;
    }

    /**
     * Gets driver class name.
     *
     * @return the driver class name
     */
    public String getDriverClassName() {
        return driverClassName;
    }

    /**
     * Driver manager factory instance connection factory.
     *
     * @return the connection factory
     */
    public static ConnectionFactory<DriverManagerDataSource> driverManagerFactoryInstance() {
        return cachingFactoryInstance(f->{
            DriverManagerDataSource dataSource1 = new DriverManagerDataSource();
            dataSource1.setDriverClassName(f.driverClassName);
            dataSource1.setUrl(f.getUrl());
            dataSource1.setProperties(f.getProperties());
            return dataSource1;
        });
    }

    /**
     * Dbcp 2 factory instance connection factory.
     *
     * @return the connection factory
     */
    public static ConnectionFactory<BasicDataSource> DBCP2FactoryInstance() {
        return nonCachingFactoryInstance(f->{
            BasicDataSource dataSource1 = new BasicDataSource();
            dataSource1.setUrl(f.getUrl());
            dataSource1.setDriverClassName(f.getDriverClassName());
            dataSource1.setUsername(f.getUser());
            dataSource1.setPassword(f.getPassword());
            dataSource1.setInitialSize(3);
            return dataSource1;
        });
    }

    /**
     * Non caching external connection factory instance connection factory.
     *
     * @param connectionSupplier the connection supplier
     * @return the connection factory
     */
    public static ConnectionFactory<WrappedExternalDataSource> nonCachingExternalConnectionFactoryInstance(Supplier<Connection> connectionSupplier) {
        return nonCachingFactoryInstance(f->{
            return new WrappedExternalDataSource(connectionSupplier);
        });
    }

    /**
     * Caching external connection factory instance connection factory.
     *
     * @param connectionSupplier the connection supplier
     * @return the connection factory
     */
    public static ConnectionFactory<WrappedExternalDataSource> cachingExternalConnectionFactoryInstance(Supplier<Connection> connectionSupplier) {
        return cachingFactoryInstance(f->{
            return new WrappedExternalDataSource(connectionSupplier);
        });
    }

    /**
     * Caching factory instance connection factory.
     *
     * @param <T>                   the type parameter
     * @param dataSourceInitializer the data source initializer
     * @return the connection factory
     */
    public static <T extends DataSource> ConnectionFactory<T> cachingFactoryInstance(Function<ConnectionFactory<T>,T> dataSourceInitializer) {
        ConnectionFactory<T> cf = new ConnectionFactory<>(dataSourceInitializer);
            cf.connectionSupplier = cf.cachingConnectionSupplier;
            cf.executorSupplier = cf.cachingExecutorSupplier;
        return cf;
    }

    /**
     * Non caching factory instance connection factory.
     *
     * @param <T>                   the type parameter
     * @param dataSourceInitializer the data source initializer
     * @return the connection factory
     */
    public static <T extends DataSource> ConnectionFactory<T> nonCachingFactoryInstance(Function<ConnectionFactory<T>,T> dataSourceInitializer) {
        ConnectionFactory<T> cf = new ConnectionFactory<>(dataSourceInitializer);
        cf.connectionSupplier = cf.nonCachingConnectionSupplier;
        cf.executorSupplier = cf.nonCachingExecutorSupplier;
        return cf;
    }

    /**
     * Sets driver class name.
     *
     * @param driverClassName the driver class name
     */
    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    /**
     * Gets url template.
     *
     * @return the url template
     */
    public String getUrlTemplate() {
        return urlTemplate;
    }

    /**
     * Sets url template.
     *
     * @param urlTemplate the url template
     */
    public void setUrlTemplate(String urlTemplate) {
        resetConnection();
        this.urlTemplate = urlTemplate;
    }

    /**
     * Gets host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets host.
     *
     * @param host the host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Gets port.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets port.
     *
     * @param port the port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets user.
     *
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets user.
     *
     * @param user the user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Gets password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets password.
     *
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets database.
     *
     * @return the database
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Sets database.
     *
     * @param database the database
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Gets schema.
     *
     * @return the schema
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets schema.
     *
     * @param schema the schema
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Gets properties.
     *
     * @return the properties
     */
    public Properties getProperties() {
        return properties;
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
        this.properties.put(key,value);
    }

    /**
     * Sets connection.
     *
     * @param connection the connection
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    private String s(String str) {
        if(str == null) {
            return "";
        } else {
            return str;
        }
    }

    private String o(Object obj) {
        if(obj == null) {
            return "";
        } else {
            return obj.toString();
        }
    }

    /**
     * Gets url.
     *
     * @return the url
     */
    public String getUrl() {
        if (notBlank(url)) {
            return url;
        } else {
            url =  urlTemplate.replace("{host}", s(host))
                    .replace("{port}", "" + port)
                    .replace("{database}", s(database))
                    .replace("{schema}", s(schema))
                    .replace("{user}", s(user))
                    .replace("{password}", s(password));
            properties.forEach((key,val)->{
                url = url.replace("{"+key+"}",o(val));
            });
            return url;
        }
    }

    /**
     * Not blank boolean.
     *
     * @param str the str
     * @return the boolean
     */
    protected static boolean notBlank(String str) {
        return str != null && ! str.isBlank();
    }

    /**
     * Blank boolean.
     *
     * @param str the str
     * @return the boolean
     */
    protected static boolean blank(String str) {
        return str == null || str.isBlank();
    }

    /**
     * Gets connection.
     *
     * @return the connection
     */
    public Connection getConnection() {
        return connectionSupplier.get();
    }

    /**
     * Gets statement executor.
     *
     * @return the statement executor
     */
    public StatementExecutor getStatementExecutor() {
        return executorSupplier.get();
    }

    /**
     * Close connection.
     */
    public void closeConnection() {
        if(connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new PersistenceException("Failed closing connection",e);
            }
        }
    }

    /**
     * Reset connection.
     */
    public void resetConnection() {
        closeConnection();
        dataSource = null;
        connection = null;
        url = null;
    }

}
