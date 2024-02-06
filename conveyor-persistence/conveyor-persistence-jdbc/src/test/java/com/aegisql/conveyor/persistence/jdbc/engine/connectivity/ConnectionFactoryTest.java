package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.CachingStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.NonCachingStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;
import com.mockrunner.mock.jdbc.JDBCMockObjectFactory;
import com.mockrunner.mock.jdbc.MockDataSource;
import com.mockrunner.mock.jdbc.MockDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

//@Ignore
public class ConnectionFactoryTest {

    private final JDBCMockObjectFactory factory  = new JDBCMockObjectFactory();

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
        factory.restoreDrivers();
    }

    private void setupConnectionFactory(ConnectionFactory cf) {
        cf.setUrlTemplate("jdbc:mock://{host}:{port}/{database}?user={user};password={password};schema={schema};property={property}");
        cf.setPort(1000);
        cf.setHost("my.db.com");
        cf.setDatabase("test_db");
        cf.setSchema("test_schema");
        cf.setUser("me");
        cf.setPassword("secret");
        cf.addProperty("property","value");
    }

    @Test
    public void basicDriverTest() throws SQLException {
        MockDriver mockDriver = factory.createMockDriver();
        ConnectionFactory<DriverManagerDataSource> cf = ConnectionFactory.driverManagerFactoryInstance();
        cf.setDriverClassName(MockDriver.class.getName());
        setupConnectionFactory(cf);
        Connection connection = cf.getConnection();
        assertNotNull(connection);
        assertEquals("jdbc:mock://my.db.com:1000/test_db?user=me;password=secret;schema=test_schema;property=value",cf.getUrl());
        StatementExecutor statementExecutor = cf.getStatementExecutor();
        assertNotNull(statementExecutor);
        assertEquals(statementExecutor.getClass(), CachingStatementExecutor.class);
        DriverManager.deregisterDriver(mockDriver);
    }

    @Test
    public void cachingDataSourceTest() {
        ConnectionFactory<MockDataSource> cf = ConnectionFactory.cachingFactoryInstance(f->{
            MockDataSource mockDataSource = factory.createMockDataSource();
            mockDataSource.setupConnection(factory.createMockConnection());
            return mockDataSource;
        });
        setupConnectionFactory(cf);
        Connection connection = cf.getConnection();
        assertNotNull(connection);
        assertEquals("jdbc:mock://my.db.com:1000/test_db?user=me;password=secret;schema=test_schema;property=value",cf.getUrl());
        StatementExecutor statementExecutor = cf.getStatementExecutor();
        assertNotNull(statementExecutor);
        assertEquals(statementExecutor.getClass(), CachingStatementExecutor.class);
    }

    @Test
    public void nonCachingDataSourceTest() {
        ConnectionFactory<MockDataSource> cf = ConnectionFactory.nonCachingFactoryInstance(f->{
            MockDataSource mockDataSource = factory.createMockDataSource();
            mockDataSource.setupConnection(factory.createMockConnection());
            return mockDataSource;
        });
        setupConnectionFactory(cf);
        Connection connection = cf.getConnection();
        assertNotNull(connection);
        assertEquals("jdbc:mock://my.db.com:1000/test_db?user=me;password=secret;schema=test_schema;property=value",cf.getUrl());
        StatementExecutor statementExecutor = cf.getStatementExecutor();
        assertNotNull(statementExecutor);
        assertEquals(statementExecutor.getClass(), NonCachingStatementExecutor.class);
    }

    @Test
    public void cachingExtConnectionTest() {
        ConnectionFactory<WrappedExternalDataSource> cf = ConnectionFactory.cachingExternalConnectionFactoryInstance(()->factory.createMockConnection());
        setupConnectionFactory(cf);
        Connection connection = cf.getConnection();
        assertNotNull(connection);
        assertEquals("jdbc:mock://my.db.com:1000/test_db?user=me;password=secret;schema=test_schema;property=value",cf.getUrl());
        StatementExecutor statementExecutor = cf.getStatementExecutor();
        assertNotNull(statementExecutor);
        assertEquals(statementExecutor.getClass(), CachingStatementExecutor.class);
    }

    @Test
    public void nonCachingExtConnectionTest() {
        ConnectionFactory<WrappedExternalDataSource> cf = ConnectionFactory.nonCachingExternalConnectionFactoryInstance(()->factory.createMockConnection());
        setupConnectionFactory(cf);
        Connection connection = cf.getConnection();
        assertNotNull(connection);
        assertEquals("jdbc:mock://my.db.com:1000/test_db?user=me;password=secret;schema=test_schema;property=value",cf.getUrl());
        StatementExecutor statementExecutor = cf.getStatementExecutor();
        assertNotNull(statementExecutor);
        assertEquals(statementExecutor.getClass(), NonCachingStatementExecutor.class);
    }


}