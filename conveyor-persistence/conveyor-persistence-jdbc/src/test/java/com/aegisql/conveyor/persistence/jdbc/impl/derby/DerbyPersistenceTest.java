package com.aegisql.conveyor.persistence.jdbc.impl.derby;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.DerbyPersistence.DerbyPersistenceBuilder;

public class DerbyPersistenceTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		String conveyor_db_path = "conveyor_db";
		File f = new File(conveyor_db_path);
		try {
			//Deleting the directory recursively using FileUtils.
			FileUtils.deleteDirectory(f);
			System.out.println("Directory has been deleted recursively !");
		} catch (IOException e) {
			System.err.println("Problem occurs when deleting the directory : " + conveyor_db_path);
			e.printStackTrace();
		}
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
	public void test() throws Exception {
		DerbyPersistenceBuilder<Integer> pb = DerbyPersistence.forKeyClass(Integer.class);
		assertNotNull(pb);
		
		Persistence<Integer> p = pb.build();
		assertNotNull(p);		
	}

}
