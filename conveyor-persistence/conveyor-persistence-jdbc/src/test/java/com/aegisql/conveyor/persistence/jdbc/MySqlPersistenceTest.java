package com.aegisql.conveyor.persistence.jdbc;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNoException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;

public class MySqlPersistenceTest {

	private static String mysqlConnectionUrl = "jdbc:mysql://localhost:3306/";
	private static Connection connnection;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Throwable driverNotFound = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (Exception e) {
			driverNotFound = e;
		}
		assumeNoException("MySQL Driver required for this test", driverNotFound);
		connnection=DriverManager.getConnection(mysqlConnectionUrl,"root","");
		Statement st = connnection.createStatement();
		st.execute("DROP DATABASE IF EXISTS mysql_persistence_test");
		st.close();		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		connnection.close();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {
		JdbcPersistenceBuilder<Integer> jpb = JdbcPersistenceBuilder.presetInitializer("mysql", Integer.class)
				.autoInit(true)
				.schema("mysql_persistence_test")
				.partTable("PART")
				.completedLogTable("COMPLETED_LOG")
				.setProperty("user", "root")
				;
		
		JdbcPersistence<Integer> p = jpb.build();
		
		assertNotNull(p);
		Cart<Integer,String,String> cart = new ShoppingCart<Integer, String, String>(100, "test", "label");
		p.savePart(1, cart);
		Cart restored = p.getPart(1);
		assertNotNull(restored);
	}

}
