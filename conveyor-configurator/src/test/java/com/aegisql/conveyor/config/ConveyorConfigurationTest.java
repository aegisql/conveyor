package com.aegisql.conveyor.config;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.config.harness.NameLabel;
import com.aegisql.conveyor.config.harness.StringSupplier;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.loaders.ResultConsumerLoader;
import com.aegisql.conveyor.loaders.ScrapConsumerLoader;
import com.aegisql.conveyor.loaders.StaticPartLoader;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import com.aegisql.conveyor.parallel.KBalancedParallelConveyor;
import com.aegisql.conveyor.parallel.LBalancedParallelConveyor;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.utils.batch.BatchConveyor;
import com.aegisql.id_builder.IdSource;
import com.aegisql.id_builder.impl.TimeHostIdGenerator;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junitpioneer.jupiter.RestoreEnvironmentVariables;
import org.junitpioneer.jupiter.RestoreSystemProperties;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@RestoreSystemProperties
@RestoreEnvironmentVariables
public class ConveyorConfigurationTest {
	
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		tearDownAfterClass();

		ConveyorConfiguration.DEFAULT_TIMEOUT_MSEC = 5 * 1000;
		
		try {
			File dir = new File("./");
			
			Arrays.stream(dir.listFiles()).map(f->f.getName()).filter(f->(f.endsWith(".blog")||f.endsWith(".blog.zip"))).forEach(f->new File(f).delete());
			
		} catch (Exception e) {
			e.printStackTrace();
		}


		String conveyor_db_path = "testConv";
		File f = new File(conveyor_db_path);
		try {
			// Deleting the directory recursively using FileUtils.
			FileUtils.deleteDirectory(f);
			System.out.println("Directory has been deleted recursively !");
		} catch (IOException e) {
			System.err.println("Problem occurs when deleting the directory : " + conveyor_db_path);
			e.printStackTrace();
		}

		JdbcPersistenceBuilder.presetInitializer("derby",Integer.class).autoInit(true).schema("testConv").partTable("test2")
				.completedLogTable("test2Completed").setArchived().maxBatchSize(3).build();
		JdbcPersistenceBuilder.presetInitializer("derby",Integer.class).autoInit(true).schema("testConv").partTable("persistent")
				.completedLogTable("persistentCompleted").setArchived().maxBatchSize(3).build();
	}

	@AfterAll
	public static void tearDownAfterClass() {
		try {
			File dir = new File("./");
			
			Arrays.stream(dir.listFiles()).map(f->f.getName()).filter(f->(f.endsWith(".blog")||f.endsWith(".blog.zip"))).forEach(f->new File(f).delete());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@BeforeEach
	public void setUp() {
	}

	@AfterEach
	public void tearDown() {
	}

	@Test
	public void testSimpleYamlFileByAbsolutePathAndExtYAML() throws Exception {
		ConveyorConfiguration.build("src/test/resources/test1.yaml");
		Conveyor<Integer, String, String> c = Conveyor.byName("test1");
		assertNotNull(c);
	}

	@Test
	@SetSystemProperty(key = "conveyor.env.init", value = "test")
	@SetEnvironmentVariable(key = "conveyor.prop.init", value = "test")
	public void testSimpleYamlFileByClassPathAndExtYML() throws Exception {
		ConveyorConfiguration.build("classpath:test1.yml");
		Conveyor<Integer, String, String> c = Conveyor.byName("test1");
		assertNotNull(c);
		Conveyor<Integer, String, String> pr = Conveyor.byName("prop");
		assertNotNull(pr);
		Conveyor<Integer, String, String> en = Conveyor.byName("env");
		assertNotNull(en);
	}

	@Test
	public void testSimplePropertiesFileByAbsolutePath() throws Exception {
		ConveyorConfiguration.build("src/test/resources/test2.properties");
		assertNotNull(Conveyor.byName("test.part"));
		Conveyor<Integer, NameLabel, String> c = Conveyor.byName("test2");
		assertNotNull(c);
		assertTrue(c instanceof PersistentConveyor);
	}

	@Test
	public void testSimplePropertiesFileByClassPath() throws Exception {
		ConveyorConfiguration.build("CLASSPATH:test2.properties");
		assertNotNull(Conveyor.byName("test2"));
		assertNotNull(Conveyor.byName("test.part"));
	}

	@Test
	public void testSimpleYampFileIdenticalToProperties() throws Exception {
		ConveyorConfiguration.build("CLASSPATH:test3.yml");
		assertNotNull(Conveyor.byName("c3-1"));
		assertNotNull(Conveyor.byName("c3.p1"));
	}

	@Test
	public void testSimpleYampFileWithStructure() throws Exception {
		ConveyorConfiguration.build("CLASSPATH:test4.yml");
		assertNotNull(Conveyor.byName("c4.p1.x"));
		Conveyor<Integer, NameLabel, String> c = Conveyor.byName("c4-1");
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
	
	public static Predicate<StringSupplier> predRE = ss -> {
		System.out.println("PREDICATE TEST "+ss.get());
		return "FIRSTpreffix-c5-1-suffixLAST_1LAST_2".equals(ss.get());
	};


	@Test
	public void testYampFileWithStructureAndReadiness() throws Exception {
		ConveyorConfiguration.build("CLASSPATH:test5.yml");
		Conveyor<Integer, NameLabel, String> c = Conveyor.byName("c5-1");
		assertNotNull(c);
		CompletableFuture<String> f = c.build().id(1).createFuture();
		c.part().id(1).label(NameLabel.FIRST).value("FIRST").place();
		c.part().id(1).label(NameLabel.LAST).value("LAST_1").place();
		c.part().id(1).label(NameLabel.LAST).value("LAST_2").place();
		c.part().id(1).label(NameLabel.END).place();
		String res = f.get();
		System.out.println(res);
		assertEquals("FIRSTpreffix-c5-1-suffixLAST_1LAST_2", res);

	}

	
	@Test
	public void testSuportedTypes() throws Exception {
		// Assembling
		ConveyorConfiguration.build("classpath:types.properties");
		Conveyor<Integer, String, String> ac = Conveyor.byName("assembling");
		assertNotNull(ac);
		assertTrue(ac instanceof AssemblingConveyor);

		// K
		Conveyor<Integer, String, String> kc = Conveyor.byName("kbalanced");
		assertNotNull(kc);
		assertTrue(kc instanceof KBalancedParallelConveyor);

		// K
		Conveyor<Integer, String, String> lc = Conveyor.byName("lbalanced");
		assertNotNull(lc);
		assertTrue(lc instanceof LBalancedParallelConveyor);

		// BATCH
		Conveyor<Integer, String, String> bc = Conveyor.byName("batch");
		assertNotNull(bc);
		assertTrue(bc instanceof BatchConveyor);

		// PERSISTENT
		Conveyor<Integer, String, String> pc = Conveyor.byName("persistent");
		assertNotNull(pc);
		assertTrue(pc instanceof PersistentConveyor);

	}
	
	@Test
	public void testSuportedReadable() throws Exception {
		ConveyorConfiguration.build("classpath:supported.properties");
	}

	@Test
	public void testSuportedReadablePersistenceProperties() throws Exception {
		ConveyorConfiguration.build("classpath:persistence.properties");
		Thread.sleep(1000);
	}

	public Archiver archiver = null;
	
	public IdSource idSource = TimeHostIdGenerator.idGenerator_10x4x5(1000);
	
	@Test
	public void testPersistenceProperties() throws Exception {
		ConveyorConfiguration.build("classpath:test6.properties");
		assertNotNull(Conveyor.byName("c6_1"));
		assertTrue(Conveyor.byName("c6_1") instanceof PersistentConveyor);
		assertNotNull(Persistence.byName("com.aegisql.conveyor.persistence.derby.test:type=c6a_persist"));
		assertNotNull(Persistence.byName("com.aegisql.conveyor.persistence.derby.test:type=c6_persist"));
	}

	@Test
	public void testPersistenceYaml() throws Exception {
		ConveyorConfiguration.build("classpath:test7.yml");
		assertNotNull(Conveyor.byName("c7_1"));
		assertTrue(Conveyor.byName("c7_1") instanceof PersistentConveyor);
		assertNotNull(Persistence.byName("com.aegisql.conveyor.persistence.derby.test:type=c7a_persist"));
		assertNotNull(Persistence.byName("com.aegisql.conveyor.persistence.derby.test:type=c7_persist"));
	}

	public static Conveyor mockConveyor = mock(Conveyor.class);
	static {
		when(mockConveyor.resultConsumer(any(ResultConsumer.class))).thenReturn(new ResultConsumerLoader(null, p->{}, r->{}));
		when(mockConveyor.scrapConsumer(any(ScrapConsumer.class))).thenReturn(new ScrapConsumerLoader(p->{}, r->{}));
		when(mockConveyor.resultConsumer()).thenReturn(new ResultConsumerLoader(null, p->{}, r->{}));
		when(mockConveyor.scrapConsumer()).thenReturn(new ScrapConsumerLoader(p->{}, r->{}));
		when(mockConveyor.staticPart()).thenReturn(new StaticPartLoader<>(p->new CompletableFuture<>()));
	}

	@Test
	@SetEnvironmentVariable(key = "conveyor.c10_1.supplier", value = "com.aegisql.conveyor.config.ConveyorConfigurationTest.mockConveyor")
	public void testPersistenceConveyorYamlSettersCall() throws Exception {
		ConveyorConfiguration.build("classpath:test10.yml");
		verify(mockConveyor,times(1)).setName("c10_1");
		verify(mockConveyor,times(1)).setIdleHeartBeat(Duration.ofMillis(1500));
		verify(mockConveyor,times(1)).setDefaultBuilderTimeout(Duration.ofMillis(1000));
		verify(mockConveyor,times(1)).rejectUnexpireableCartsOlderThan(Duration.ofMillis(10000));
		verify(mockConveyor,times(1)).resultConsumer(any(ResultConsumer.class));
		verify(mockConveyor,times(1)).scrapConsumer(any(ScrapConsumer.class));
		verify(mockConveyor,times(1)).setOnTimeoutAction(any());
		verify(mockConveyor,times(1)).setBuilderSupplier(any(BuilderSupplier.class));
		verify(mockConveyor,times(1)).setReadinessEvaluator(any(BiPredicate.class));
		verify(mockConveyor,times(2)).resultConsumer(); //next and readiness
		verify(mockConveyor,times(1)).scrapConsumer();
		verify(mockConveyor,times(1)).acceptLabels(any()); //one array
		verify(mockConveyor,times(2)).staticPart(); // 2 parts in test
		verify(mockConveyor,times(1)).setDefaultCartConsumer(any());
		verify(mockConveyor,times(2)).addCartBeforePlacementValidator(any());
		verify(mockConveyor,times(1)).addBeforeKeyEvictionAction(any());
		verify(mockConveyor,times(1)).addBeforeKeyReschedulingAction(any());
		verify(mockConveyor,times(1)).enablePostponeExpiration(false);
		verify(mockConveyor,times(1)).enablePostponeExpirationOnTimeout(false);
		verify(mockConveyor,times(1)).setExpirationPostponeTime(Duration.ofMillis(1000));
		verify(mockConveyor,times(1)).setAutoAcknowledge(true);
		verify(mockConveyor,times(1)).autoAcknowledgeOnStatus(Status.READY, Status.TIMED_OUT, Status.CANCELED);
		verify(mockConveyor,times(1)).setCartPayloadAccessor(any());
	}

	@Test
	public void testPersistenceComplexYaml() throws Exception {
		ConveyorConfiguration.build("classpath:test8.yml");
		assertNotNull(Conveyor.byName("c8_1"));
		assertTrue(Conveyor.byName("c8_1") instanceof PersistentConveyor);
		assertNotNull(Persistence.byName("com.aegisql.conveyor.persistence.derby.test:type=c8a_persist"));
		assertNotNull(Persistence.byName("com.aegisql.conveyor.persistence.derby.test:type=c8_persist"));
	}

	@Test
	public void testYampFileWithPersistence() throws Exception {
		
		String conveyor_db_path = "c9";
		String blog_db_path = "parts.blog";
		File f = new File(conveyor_db_path);
		try {
			FileUtils.deleteDirectory(f);
			System.out.println("Directory c9 has been deleted!");
		} catch (IOException e) {
			System.err.println("Problem occurs when deleting the directory : " + conveyor_db_path);
			e.printStackTrace();
		}
		f = new File(blog_db_path);
		try {
			f.delete();
			System.out.println("Directory backup has been deleted!");
		} catch (Exception e) {
			System.err.println("Problem occurs when deleting the directory : " + blog_db_path);
			e.printStackTrace();
		}
		
		ConveyorConfiguration.build("CLASSPATH:test9.yml");
		Conveyor<Integer, NameLabel, String> c = Conveyor.byName("c9-1");
		assertNotNull(c);
		assertTrue(c.isRunning());
		CompletableFuture<Boolean> lastPart = null;
		for(int i = 0; i < 100; i++) {
			c.part().id(i).label(NameLabel.FIRST).value("FIRST_"+i).place();
			lastPart = c.part().id(i).label(NameLabel.LAST).value("LAST_"+i).place();
			//c.part().id(1).label(NameLabel.END).place();
		}
		
		assertTrue(lastPart.get());

		Thread.sleep(1000);
		File dir = new File("./");
		
		AtomicInteger found = new AtomicInteger(0);
		
		Arrays.stream(dir.listFiles())
			.map(file->file.getName())
			.filter(name->(name.endsWith(".blog")||name.endsWith(".blog.zip")))
			.forEach(
				file->{
					System.out.println("Found "+file);
					found.incrementAndGet();
				}
				);
		assertEquals(2, found.get());
	}

	@Test
	public void testYampFileWithCompaction() throws Exception {
		
		String conveyor_db_path = "p11";
		String blog_db_path = "parts11";
		File f = new File(conveyor_db_path);
		try {
			FileUtils.deleteDirectory(f);
			System.out.println("Directory p11 has been deleted!");
		} catch (IOException e) {
			System.err.println("Problem occurs when deleting the directory : " + conveyor_db_path);
			e.printStackTrace();
		}
		f = new File(blog_db_path+".blog");
		try {
			f.delete();
			System.out.println("Directory backup has been deleted!");
		} catch (Exception e) {
			System.err.println("Problem occurs when deleting the directory : " + blog_db_path);
			e.printStackTrace();
		}
		
		ConveyorConfiguration.build("CLASSPATH:test11.yml");
		Conveyor<Integer, NameLabel, String> c = Conveyor.byName("c11");
		Persistence<Integer> p = Persistence.byName("derby.p11.parts11").copy();
		assertNotNull(c);
		assertNotNull(p);
		assertTrue(c.isRunning());
		CompletableFuture<Boolean> lastPart = null;
		for(int i = 0; i < 100; i++) {
			c.part().id(i).label(NameLabel.FIRST).value("f1"+i).place();
			c.part().id(i).label(NameLabel.FIRST).value("f2"+i).place();
			c.part().id(i).label(NameLabel.LAST).value("l1_"+i).place();
			lastPart = c.part().id(i).label(NameLabel.LAST).value("l2_"+i).place();
		}
		
		assertTrue(lastPart.join());

		Collection<Cart<Integer, ?, Object>> carts = p.getAllParts();

		assertEquals(100,carts.size());
		for(int i = 0; i < 100; i++) {
			lastPart = c.part().id(i).label(NameLabel.END).place();
		}

		assertTrue(lastPart.get());
		
	}
	@Test
	public void testYampFile12WithDbcp() throws Exception {
		ConveyorConfiguration.build("CP:test12.yml","JP:com.aegisql.conveyor.config.harness.TestBean");
		Conveyor<Integer, NameLabel, String> c = Conveyor.byName("c12");
		Persistence<Integer> p = Persistence.byName("sqlite-memory.p12.parts12").copy();
		assertNotNull(c);
		assertNotNull(p);
		assertTrue(c.isRunning());
		p.archiveAll();
		CompletableFuture<Boolean> lastPart = null;
		for(int i = 0; i < 100; i++) {
			c.part().id(i).label(NameLabel.FIRST).value("f1"+i).place();
			c.part().id(i).label(NameLabel.FIRST).value("f2"+i).place();
			c.part().id(i).label(NameLabel.LAST).value("l1_"+i).place();
			lastPart = c.part().id(i).label(NameLabel.LAST).value("l2_"+i).place();
		}

		assertTrue(lastPart.join());

		Collection<Cart<Integer, ?, Object>> carts = p.getAllParts();

		assertEquals(400,carts.size());
		for(int i = 0; i < 100; i++) {
			lastPart = c.part().id(i).label(NameLabel.END).place();
		}

		assertTrue(lastPart.join());

	}

	@Test
	public void metaInfoYamlTest() throws Exception {
		ConveyorConfiguration.build("CP:test13.yml");
		Conveyor<Integer, NameLabel, String> c = Conveyor.byName("c13");
		ConveyorMetaInfo<Integer, NameLabel, String> metaInfo = c.getMetaInfo();
		assertNotNull(metaInfo);
		System.out.println(metaInfo);
	}
	@Test
	public void metaInfoPropertiesTest() throws Exception {
		ConveyorConfiguration.build("CP:test14.properties");
		Conveyor<Integer, NameLabel, String> c = Conveyor.byName("c14");
		Set<String> knownConveyorNames = Conveyor.getKnownConveyorNames();
		ConveyorMetaInfo<Integer, NameLabel, String> metaInfo = c.getMetaInfo();
		assertNotNull(metaInfo);
		System.out.println(metaInfo);
	}
}
