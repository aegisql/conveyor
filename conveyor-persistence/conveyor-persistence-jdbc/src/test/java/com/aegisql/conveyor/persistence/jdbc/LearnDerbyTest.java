package com.aegisql.conveyor.persistence.jdbc;

import static org.junit.Assert.*;
import java.sql.*;
import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.core.ObjectConverter;

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
		JdbcPersistence<String> p = new JdbcPersistence<>("org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:testConv;create=true",new StringConverter<String>() {
			@Override
			public String fromPersistence(String p) {
				return p;
			}
		});
		p.archiveAll();
		p.savePart(p.nextUniquePartId(), new ShoppingCart<String, String, String>("1", "one", "ONE"));
		p.savePartId("1", 100);
		p.savePart(p.nextUniquePartId(), new ShoppingCart<String, String, String>("1", "two", "TWO"));
		p.savePartId("1", 200);
		p.saveCompletedBuildKey("1");
		Collection<Cart<String,?,String>> allCarts = p.getAllParts();
		System.out.println(allCarts);
		assertNotNull(allCarts);
		assertEquals(2, allCarts.size());
		Collection<Long> ids = p.getAllPartIds("1");
		System.out.println(ids);
		assertNotNull(ids);
		assertEquals(2, ids.size());
		Collection<String> completed = p.getCompletedKeys();
		System.out.println(completed);
		assertNotNull(completed);
		assertEquals(1, completed.size());
		p.archiveParts(ids);
		p.archiveCompleteKeys(completed);
		p.archiveParts(ids);
		p.archiveAll();
	}

	@Test
	public void testPers2() throws ClassNotFoundException, SQLException {
		JdbcPersistence<String> p1 = new JdbcPersistence<>("org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:testConv;create=true",new StringConverter<String>() {
			@Override
			public String fromPersistence(String p) {
				return p;
			}
		});
		p1.archiveAll();
		p1.savePart(p1.nextUniquePartId(), new ShoppingCart<String, String, String>("1", "one", "ONE"));
		p1.savePartId("1", 100);
		p1.savePart(p1.nextUniquePartId(), new ShoppingCart<String, String, String>("1", "two", "TWO"));
		p1.savePartId("1", 200);
		p1.saveCompletedBuildKey("1");
		Collection<Cart<String,?,String>> allCarts = p1.getAllParts();
		System.out.println(allCarts);
		assertNotNull(allCarts);
		assertEquals(2, allCarts.size());
		Collection<Long> ids = p1.getAllPartIds("1");
		System.out.println(ids);
		assertNotNull(ids);
		assertEquals(2, ids.size());
		Collection<String> completed = p1.getCompletedKeys();
		System.out.println(completed);
		assertNotNull(completed);
		assertEquals(1, completed.size());
		
		JdbcPersistence<String> p2 = new JdbcPersistence<>("org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:testConv;create=true",new StringConverter<String>() {
			@Override
			public String fromPersistence(String p) {
				return p;
			}
		});

		Collection<Cart<String,?,String>> allCarts2 = p2.getAllParts();
		System.out.println(allCarts2);
		assertNotNull(allCarts2);
		assertEquals(2, allCarts2.size());
		Collection<Long> ids2 = p2.getAllPartIds("1");
		System.out.println(ids2);
		assertNotNull(ids2);
		assertEquals(2, ids2.size());
		Collection<String> completed2 = p2.getCompletedKeys();
		System.out.println(completed2);
		assertNotNull(completed2);
		assertEquals(0, completed2.size());

		
		p1.archiveParts(ids);
		p1.archiveCompleteKeys(completed);
		p1.archiveParts(ids);
		p1.archiveAll();
	}

	
}
