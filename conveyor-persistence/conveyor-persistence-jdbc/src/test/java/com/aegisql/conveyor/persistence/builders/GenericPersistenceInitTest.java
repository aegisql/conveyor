package com.aegisql.conveyor.persistence.builders;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.persistence.jdbc.builders.GenericInitPersistenceSql;

public class GenericPersistenceInitTest {

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
	public void test() {
		GenericInitPersistenceSql<Integer> init = new GenericInitPersistenceSql(Integer.class);
		String createDatabase = init.createDatabaseSql();
		String createSchema = init.createSchemaSql();
		String createPart = init.createPartTableSql();
		String createPartsIndex = init.createPartTableIndexSql();
		String createLogs = init.createCompletedLogTableSql();
		
		System.out.println(createDatabase);
		System.out.println(createSchema);
		System.out.println(createPart);
		System.out.println(createPartsIndex);
		System.out.println(createLogs);
		assertNull(createDatabase);
		assertNotNull(createSchema);
		assertNotNull(createPart);
		assertNotNull(createPartsIndex);
		assertNotNull(createLogs);
		
	}

}
