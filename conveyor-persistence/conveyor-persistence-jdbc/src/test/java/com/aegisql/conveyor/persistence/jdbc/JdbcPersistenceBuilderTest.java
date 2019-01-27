package com.aegisql.conveyor.persistence.jdbc;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JdbcPersistenceBuilderTest {

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
	public void accessDefaultDerbyEmbeddedPersistence() {
		JdbcPersistenceBuilder<Integer> jpb = JdbcPersistenceBuilder.getBuilder("derby-embedded",Integer.class);
		assertNotNull(jpb);
		JdbcPersistence<Integer> jp = jpb.autoInit(true).build();
		assertNotNull(jp);
		
	}

	@Test
	public void testConstructor() {
		JdbcPersistenceBuilder<Integer> jpb = new JdbcPersistenceBuilder<>(Integer.class)
				.driverClass("org.apache.derby.jdbc.EmbeddedDriver")
				.connectionUrl("jdbc:derby:test;create=true");
				;
		
		JdbcPersistence<Integer> jp = jpb.build();
		
		assertNotNull(jp);
	
	}

}
