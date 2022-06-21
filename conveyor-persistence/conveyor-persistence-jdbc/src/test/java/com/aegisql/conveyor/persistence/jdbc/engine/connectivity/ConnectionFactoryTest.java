package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.CachingStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.NonCachingStatementExecutor;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;
import com.mockrunner.mock.jdbc.JDBCMockObjectFactory;
import com.mockrunner.mock.jdbc.MockDataSource;
import com.mockrunner.mock.jdbc.MockDriver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

//@Ignore
public class ConnectionFactoryTest {

    private final JDBCMockObjectFactory factory  = new JDBCMockObjectFactory();

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
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
    public void cachingDataSourceTest() throws SQLException {
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
    public void nonCachingDataSourceTest() throws SQLException {
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
    public void cachingExtConnectionTest() throws SQLException {
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
    public void nonCachingExtConnectionTest() throws SQLException {
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