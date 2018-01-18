package com.aegisql.conveyor.config;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.Conveyor;

public class ConveyorConfigurationTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ConveyorConfiguration.DEFAULT_TIMEOUT_MSEC = 10*1000;
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

}
