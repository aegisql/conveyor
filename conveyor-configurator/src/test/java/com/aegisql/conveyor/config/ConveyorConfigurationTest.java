package com.aegisql.conveyor.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.config.harness.NameLabel;
import com.aegisql.conveyor.config.harness.StringSupplier;
import com.aegisql.conveyor.parallel.KBalancedParallelConveyor;
import com.aegisql.conveyor.parallel.LBalancedParallelConveyor;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.DerbyPersistence;
import com.aegisql.conveyor.utils.batch.BatchConveyor;

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
		DerbyPersistence
		.forKeyClass(Integer.class)
		.schema("testConv")
		.partTable("persistent")
		.completedLogTable("persistentCompleted")
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
		assertNotNull(Conveyor.byName("test.part"));
		Conveyor<Integer,NameLabel,String> c = Conveyor.byName("test2");
		assertNotNull(c);
		assertTrue(c instanceof PersistentConveyor);
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
		assertNotNull(Conveyor.byName("c4.p1.x"));
		Conveyor<Integer,NameLabel,String> c = Conveyor.byName("c4-1");
		assertNotNull(c);
		CompletableFuture<String> f = c.build().id(1).createFuture();
		c.part().id(1).label(NameLabel.FIRST).value("FIRST").place();
		String res = f.get();
		System.out.println(res);
		assertEquals("FIRSTpreffix-c4-1-suffix", res);
		CompletableFuture<String> f2 = c.build().id(2).createFuture();
		c.part().id(2).label(NameLabel.LAST).value("LAST").place();
		String res2 = f2.get();
		System.out.println(res2);
		assertEquals("preffix-c4-1-suffixLAST", res2);

		CompletableFuture<String> f3 = c.build().id(3).createFuture();
		c.part().id(2).label(NameLabel.END).value("END").place();
		String res3 = f3.get();
		assertEquals("preffix-c4-1-suffix", res3);
		assertTrue(c instanceof AssemblingConveyor);

	}

	@Test
	public void testSuportedTypes() throws Exception {
		//Assembling
		ConveyorConfiguration.build("classpath:types.properties");
		Conveyor<Integer,String,String> ac = Conveyor.byName("assembling");
		assertNotNull(ac);
		assertTrue(ac instanceof AssemblingConveyor);

		//K
		Conveyor<Integer,String,String> kc = Conveyor.byName("kbalanced");
		assertNotNull(kc);
		assertTrue(kc instanceof KBalancedParallelConveyor);

		//K
		Conveyor<Integer,String,String> lc = Conveyor.byName("lbalanced");
		assertNotNull(lc);
		assertTrue(lc instanceof LBalancedParallelConveyor);

		//BATCH
		Conveyor<Integer,String,String> bc = Conveyor.byName("batch");
		assertNotNull(bc);
		assertTrue(bc instanceof BatchConveyor);

		//PERSISTENT
		Conveyor<Integer,String,String> pc = Conveyor.byName("persistent");
		assertNotNull(pc);
		assertTrue(pc instanceof PersistentConveyor);

	}

	
}
