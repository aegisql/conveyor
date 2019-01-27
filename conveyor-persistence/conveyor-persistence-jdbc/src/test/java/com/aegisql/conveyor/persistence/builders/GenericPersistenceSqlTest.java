package com.aegisql.conveyor.persistence.builders;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.persistence.jdbc.builders.GenericPersistenceSql;

public class GenericPersistenceSqlTest {

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
		GenericPersistenceSql gps1 = new GenericPersistenceSql("schema", "part","log");
		System.out.println(gps1);
		GenericPersistenceSql gps2 = new GenericPersistenceSql("part","log");
		System.out.println(gps2);
		assertNotNull(gps1.saveCartQuery());
		assertNotNull(gps1.saveCompletedBuildKeyQuery());
		assertNotNull(gps1.getPartQuery());
		assertNotNull(gps1.getNumberOfPartsQuery());
		assertNotNull(gps1.getExpiredPartQuery());
		assertNotNull(gps1.getAllStaticPartsQuery());
		assertNotNull(gps1.getAllCompletedKeysQuery());
		assertNotNull(gps1.getAllPartIdsQuery());
		assertNotNull(gps1.getAllUnfinishedPartIdsQuery());
		
		System.out.println(gps1.saveCartQuery());
		System.out.println(gps1.getPartQuery());
		System.out.println(gps1.getNumberOfPartsQuery());
		System.out.println(gps1.getExpiredPartQuery());
		System.out.println(gps1.getAllStaticPartsQuery());
		System.out.println(gps1.getAllPartIdsQuery());
		System.out.println(gps1.getAllUnfinishedPartIdsQuery());
		System.out.println(gps1.saveCompletedBuildKeyQuery());
		System.out.println(gps1.getAllCompletedKeysQuery());
	}

}
