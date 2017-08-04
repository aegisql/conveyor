package com.aegisql.conveyor.persistence.jdbc;

import static org.junit.Assert.*;
import java.sql.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class LearnDerbyTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDbConnection() throws Exception {
		String connectionURL = "jdbc:derby:testDb1;create=true";
		String driver = "org.apache.derby.jdbc.EmbeddedDriver";

	    Class.forName(driver);
	    
	    Connection conn = DriverManager.getConnection(connectionURL);
	    Statement st = conn.createStatement();
	    try {
	    	st.execute("DROP TABLE TEST");
	    } catch(SQLException e) {
	    	
	    }
	    st.close();

	    st = conn.createStatement();
	    try {
	    	st.execute("CREATE TABLE TEST (ID INT PRIMARY KEY)");
	    } catch(SQLException e) {
	    	
	    }
	    st.close();
	    st = conn.createStatement();
	    st.execute("INSERT INTO TEST (ID) VALUES(100)");
	    st.close();

	    st = conn.createStatement();
	    ResultSet r = st.executeQuery("SELECT * FROM TEST");
	    r.next();
	    long id = r.getLong(1);
	    System.out.println(id);
	    st.close();

	    conn.close();
	}
	
	@Test
	public void testPers() throws ClassNotFoundException, SQLException {
		JdbcPersistence<String> p = new JdbcPersistence<>("org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:testConv;create=true");
	}

}
