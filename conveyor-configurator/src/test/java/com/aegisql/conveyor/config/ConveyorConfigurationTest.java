package com.aegisql.conveyor.config;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.DerbyPersistence;

public class ConveyorConfigurationTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ConveyorConfiguration.DEFAULT_TIMEOUT_MSEC = 10*1000;
		
		String conveyor_db_path = "testConf";
		File f = new File(conveyor_db_path);
		try {
			//Deleting the directory recursively using FileUtils.
			FileUtils.deleteDirectory(f);
			System.out.println("Directory has been deleted recursively !");
		} catch (IOException e) {
			System.err.println("Problem occurs when deleting the directory : " + conveyor_db_path);
			e.printStackTrace();
		}

		
		DerbyPersistence
		.forKeyClass(Integer.class)
		.schema("testConv")
		.partTable("test2")
		.completedLogTable("test2Completed")
		.whenArchiveRecords().markArchived()
		.maxBatchSize(3)
		.build();
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
	public void testSimpleYamlFileByAbsolutePathAndExtYAML() throws Exception {
		ConveyorConfiguration.build("src/test/resources/test1.yaml");
		Conveyor<Integer,String,String> c = Conveyor.byName("test1");
		assertNotNull(c);
	}

	@Test
	public void testSimpleYamlFileByClassPathAndExtYML() throws Exception {
		ConveyorConfiguration.build("classpath:test1.yml");
		Conveyor<Integer,String,String> c = Conveyor.byName("test1");
		assertNotNull(c);
	}
	
	@Test
	public void testSimplePropertiesFileByAbsolutePath() throws Exception {
		ConveyorConfiguration.build("src/test/resources/test2.properties");
		//assertNotNull(Conveyor.byName("test0"));
		//assertNotNull(Conveyor.byName("test1"));
		assertNotNull(Conveyor.byName("test2"));
		assertNotNull(Conveyor.byName("test.part"));
	}

	@Test
	public void testSimplePropertiesFileByClassPath() throws Exception {
		ConveyorConfiguration.build("CLASSPATH:test2.properties");
		//assertNotNull(Conveyor.byName("test0"));
		//assertNotNull(Conveyor.byName("test1"));
		assertNotNull(Conveyor.byName("test2"));
		assertNotNull(Conveyor.byName("test.part"));
	}

	@Test
	public void testSimpleYampFileIdenticalToProperties() throws Exception {
		ConveyorConfiguration.build("CLASSPATH:test3.yml");
		//assertNotNull(Conveyor.byName("test0"));
		//assertNotNull(Conveyor.byName("test1"));
		assertNotNull(Conveyor.byName("c3-1"));
		assertNotNull(Conveyor.byName("c3.p1"));
	}

	@Test
	public void testSimpleYampFileWithStructure() throws Exception {
		ConveyorConfiguration.build("CLASSPATH:test4.yml");
		//assertNotNull(Conveyor.byName("test0"));
		//assertNotNull(Conveyor.byName("test1"));
		assertNotNull(Conveyor.byName("c4-1"));
		assertNotNull(Conveyor.byName("c4.p1.x"));
	}

	
}
