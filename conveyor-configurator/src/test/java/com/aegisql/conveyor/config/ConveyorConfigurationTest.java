package com.aegisql.conveyor.config;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;
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
import com.aegisql.conveyor.parallel.PBalancedParallelConveyor;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.redis.RedisConnectionFactory;
import com.aegisql.conveyor.persistence.redis.RedisPersistenceMBean;
import com.aegisql.conveyor.utils.batch.BatchConveyor;
import com.aegisql.id_builder.IdSource;
import com.aegisql.id_builder.impl.DecimalIdGenerator;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junitpioneer.jupiter.RestoreEnvironmentVariables;
import org.junitpioneer.jupiter.RestoreSystemProperties;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import javax.management.ObjectName;

import redis.clients.jedis.JedisPooled;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@RestoreSystemProperties
@RestoreEnvironmentVariables
public class ConveyorConfigurationTest {

	private static final String TEST_ARTIFACTS_DIR = "test-artifacts";
	private static final String TEST_PERSISTENCE_DIR = TEST_ARTIFACTS_DIR + "/persistence";
	
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		tearDownAfterClass();

		ConveyorConfiguration.DEFAULT_TIMEOUT_MSEC = 5 * 1000;

		deleteLegacyPersistenceArtifacts();
		FileUtils.deleteDirectory(new File(TEST_ARTIFACTS_DIR));

		JdbcPersistenceBuilder.presetInitializer("derby",Integer.class).autoInit(true)
				.database(new File(TEST_PERSISTENCE_DIR, "testConv").getPath())
				.schema("testConv").partTable("test2")
				.completedLogTable("test2Completed").setArchived().maxBatchSize(3).build();
		JdbcPersistenceBuilder.presetInitializer("derby",Integer.class).autoInit(true)
				.database(new File(TEST_PERSISTENCE_DIR, "testConv").getPath())
				.schema("testConv").partTable("persistent")
				.completedLogTable("persistentCompleted").setArchived().maxBatchSize(3).build();
	}

	@AfterAll
	public static void tearDownAfterClass() {
		try {
			deleteLegacyPersistenceArtifacts();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void deleteLegacyPersistenceArtifacts() throws IOException {
		FileUtils.deleteDirectory(new File("testConv"));
		FileUtils.deleteDirectory(new File("test"));
		FileUtils.deleteDirectory(new File("c9"));
		FileUtils.deleteDirectory(new File("p11"));
		FileUtils.deleteQuietly(new File("derby.log"));
		File dir = new File("./");
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		Arrays.stream(files)
				.filter(file ->
						file.getName().startsWith("conveyor.db")
								|| file.getName().endsWith(".blog")
								|| file.getName().endsWith(".blog.zip"))
				.forEach(FileUtils::deleteQuietly);
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
	
	public IdSource idSource = DecimalIdGenerator.idGenerator_10x4x5(1000);
	
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

		AtomicInteger found = new AtomicInteger(0);
		FileUtils.listFiles(new File(TEST_PERSISTENCE_DIR), null, true).stream()
				.map(File::getName)
				.filter(name -> name.endsWith(".blog") || name.endsWith(".blog.zip"))
				.forEach(file -> {
					System.out.println("Found " + file);
					found.incrementAndGet();
				});
		assertEquals(2, found.get());
		assertTrue(new File(TEST_PERSISTENCE_DIR).isDirectory());
		assertFalse(new File("c9").exists());
	}

	@Test
	public void testYampFileWithCompaction() throws Exception {
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

		Collection<Cart<Integer, ?, Object>> carts = waitForPartCount(p, 100, Duration.ofSeconds(5));

		assertEquals(100,carts.size());
		assertTrue(new File(TEST_PERSISTENCE_DIR).isDirectory());
		assertFalse(new File("p11").exists());
		for(int i = 0; i < 100; i++) {
			lastPart = c.part().id(i).label(NameLabel.END).place();
		}

		assertTrue(lastPart.get());
		
	}

	private static Collection<Cart<Integer, ?, Object>> waitForPartCount(Persistence<Integer> persistence, int expectedCount, Duration timeout) throws InterruptedException {
		long deadline = System.nanoTime() + timeout.toNanos();
		Collection<Cart<Integer, ?, Object>> carts = persistence.getAllParts();
		while (System.nanoTime() < deadline) {
			if (carts.size() == expectedCount) {
				return carts;
			}
			Thread.sleep(50);
			carts = persistence.getAllParts();
		}
		return carts;
	}

	private static void awaitTrue(BooleanSupplier condition, String message, Duration timeout) throws InterruptedException {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			if (condition.getAsBoolean()) {
				return;
			}
			Thread.sleep(50);
		}
		assertTrue(condition.getAsBoolean(), message);
	}

	private static void deleteRedisNamespace(JedisPooled jedis, String persistenceName) {
		String prefix = "conv:{" + persistenceName + "}";
		Set<String> keys = jedis.keys(prefix + "*");
		if (!keys.isEmpty()) {
			jedis.del(keys.toArray(String[]::new));
		}
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

	@Test
	public void testRedisPersistenceYaml() throws Exception {
		requireRedisAvailability();

		String persistenceName = "test.parts15";
		String jmxName = "com.aegisql.conveyor.persistence.redis." + persistenceName + ":type=" + persistenceName;
		String authPersistenceName = "test.parts15auth";
		String authJmxName = "com.aegisql.conveyor.persistence.redis." + authPersistenceName + ":type=" + authPersistenceName;
		String namespaceMetaKey = "conv:{" + persistenceName + "}:meta";
		ObjectName objectName = new ObjectName(jmxName);
		ObjectName authObjectName = new ObjectName(authJmxName);

		ConveyorConfiguration.build("CP:test15.yml");

		Conveyor<Integer, NameLabel, String> c = Conveyor.byName("c15");
		assertNotNull(c);
		assertTrue(c instanceof PersistentConveyor);

		Persistence<Integer> persistence = Persistence.byName(jmxName);
		Persistence<Integer> authPersistence = Persistence.byName(authJmxName);
		assertNotNull(persistence);
		assertNotNull(authPersistence);

		try (JedisPooled db0 = RedisConnectionFactory.open("redis://localhost:6379/0");
		     JedisPooled db1 = RedisConnectionFactory.open("redis://localhost:6379/1");
		     JedisPooled db2 = RedisConnectionFactory.open("redis://localhost:6379/2")) {
			deleteRedisNamespace(db0, persistenceName);
			deleteRedisNamespace(db1, persistenceName);
			deleteRedisNamespace(db2, authPersistenceName);

			persistence.archiveAll();

			assertTrue(RedisPersistenceMBean.mBeanServer.isRegistered(objectName));
			assertEquals("redis://localhost:6379/0", RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "RedisUri"));
			assertEquals("BY_PRIORITY_AND_ID", RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "RestoreOrder"));
			assertEquals("REDIS_INDEX", RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "PriorityRestoreStrategy"));
			assertEquals(25, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "MaxBatchSize"));
			assertEquals(2000L, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "MaxBatchTime"));
			assertEquals(7, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "MaxTotal"));
			assertEquals(4, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "MaxIdle"));
			assertEquals(1, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "MinIdle"));
			assertEquals(1500, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "ConnectionTimeoutMillis"));
			assertEquals(1600, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "SocketTimeoutMillis"));
			assertEquals(1700, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "BlockingSocketTimeoutMillis"));
			assertEquals(1, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "Database"));
			assertEquals("redis-configurator-test15", RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "ClientName"));
			assertNull(RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "Username"));
			assertEquals(Boolean.FALSE, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "PasswordConfigured"));
			assertEquals(Boolean.FALSE, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "Ssl"));
			assertEquals(1, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "AdditionalFieldCount"));
			assertEquals(1, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "ConverterCount"));
			assertEquals(Boolean.TRUE, RedisPersistenceMBean.mBeanServer.getAttribute(objectName, "AutoInit"));

			assertTrue(RedisPersistenceMBean.mBeanServer.isRegistered(authObjectName));
			assertEquals("redis://localhost:6379/2", RedisPersistenceMBean.mBeanServer.getAttribute(authObjectName, "RedisUri"));
			assertEquals(2, RedisPersistenceMBean.mBeanServer.getAttribute(authObjectName, "Database"));
			assertEquals("redis-configurator-test15-auth", RedisPersistenceMBean.mBeanServer.getAttribute(authObjectName, "ClientName"));
			assertEquals("redis-user", RedisPersistenceMBean.mBeanServer.getAttribute(authObjectName, "Username"));
			assertEquals(Boolean.TRUE, RedisPersistenceMBean.mBeanServer.getAttribute(authObjectName, "PasswordConfigured"));
			assertEquals(Boolean.FALSE, RedisPersistenceMBean.mBeanServer.getAttribute(authObjectName, "Ssl"));
			assertEquals(3, RedisPersistenceMBean.mBeanServer.getAttribute(authObjectName, "MaxTotal"));
			assertEquals(2, RedisPersistenceMBean.mBeanServer.getAttribute(authObjectName, "MaxIdle"));
			assertEquals(1, RedisPersistenceMBean.mBeanServer.getAttribute(authObjectName, "MinIdle"));
			assertEquals(Boolean.FALSE, RedisPersistenceMBean.mBeanServer.getAttribute(authObjectName, "AutoInit"));
			assertEquals(Boolean.FALSE, RedisPersistenceMBean.mBeanServer.getAttribute(authObjectName, "ExternalClient"));

			CompletableFuture<Boolean> accepted = c.part().id(15).label(NameLabel.FIRST).value("FIRST_15")
					.addProperty("AUDIT", "trace-15")
					.addProperty("transientA", "drop-me")
					.place();
			assertTrue(accepted.join());

			Collection<Cart<Integer, ?, Object>> carts = waitForPartCount(persistence.copy(), 1, Duration.ofSeconds(5));
			assertEquals(1, carts.size());
			assertEquals(2, persistence.getMinCompactSize());

			Map<String, String> namespaceMeta = db1.hgetAll(namespaceMetaKey);
			assertEquals("redis", namespaceMeta.get("backend"));
			assertEquals("REDIS_INDEX", namespaceMeta.get("priorityRestoreStrategy"));
			assertTrue(db0.hgetAll(namespaceMetaKey).isEmpty(), "Configured Redis database should isolate persistence state from db 0");
			Long persistedId = persistence.getAllPartIds(15).iterator().next();
			Map<String, String> partMeta = db1.hgetAll("conv:{test.parts15}:part:" + persistedId + ":meta");
			assertTrue(partMeta.containsKey("field:AUDIT:hint"));
			assertTrue(partMeta.containsKey("field:AUDIT:data"));
			assertFalse(partMeta.containsKey("field:transientA:hint"));

			assertTrue(c.part().id(15).label(NameLabel.LAST).value("LAST_15").place().join());
		} finally {
			persistence.archiveAll();
			authPersistence.close();
			c.completeAndStop().join();
		}
	}

	@Test
	public void testPersistentConveyorUnloadOnBuilderTimeoutYaml() throws Exception {
		ConveyorConfiguration.build("CP:test16.yml");

		Conveyor<Integer, NameLabel, String> c = Conveyor.byName("c16");
		assertNotNull(c);
		assertTrue(c instanceof PersistentConveyor);

		Map<Integer, String> results = new ConcurrentHashMap<>();
		List<ScrapBin<Integer, ?>> scraps = Collections.synchronizedList(new ArrayList<>());
		c.resultConsumer().andThen(bin -> results.put(bin.key, bin.product)).set();
		c.scrapConsumer().andThen(scrap -> scraps.add((ScrapBin<Integer, ?>) scrap)).set();

		try (Persistence<Integer> persistence = Persistence.byName("derby.p16.parts16").copy()) {
			try {
				persistence.archiveAll();

				assertTrue(c.part().id(16).label(NameLabel.FIRST).value("FIRST_16").place().join());
				assertEquals(1, waitForPartCount(persistence, 1, Duration.ofSeconds(5)).size());

				awaitTrue(() -> scraps.stream().anyMatch(scrap ->
								scrap.key.equals(16)
										&& scrap.failureType == ScrapBin.FailureType.BUILD_EXPIRED
										&& scrap.comment.startsWith("Site expired. No timeout action")),
						"Configurator should propagate unloadOnBuilderTimeout so the timed out build produces timeout scrap",
						Duration.ofSeconds(5));

				assertFalse(persistence.getAllPartIds(16).isEmpty(),
						"Unload-on-timeout should keep the timed out carts persisted for later replay");

				assertTrue(c.part().id(16).label(NameLabel.LAST).value("LAST_16").place().join());
				awaitTrue(() -> "FIRST_16c16LAST_16".equals(results.get(16)),
						"Placing the missing part after timeout should replay the persisted carts and finish the build",
						Duration.ofSeconds(5));
			} finally {
				c.stop();
				try (Persistence<Integer> cleanup = Persistence.byName("derby.p16.parts16").copy()) {
					cleanup.archiveAll();
				}
			}
		}
	}

	@Test
	public void testPBalancedYaml() throws Exception {
		ConveyorConfiguration.build("CP:test17.yml");

		Conveyor<Integer, NameLabel, String> c = Conveyor.byName("c17");
		assertNotNull(c);
		assertTrue(c instanceof PBalancedParallelConveyor);

		Map<Integer, String> results = new ConcurrentHashMap<>();
		c.resultConsumer().andThen(bin -> results.put(bin.key, bin.product)).set();

		try {
			assertTrue(c.part().id(17).label(NameLabel.FIRST).addProperty("version", 1).addProperty("lane", "A").value("LEFT-").place().join());
			assertTrue(c.part().id(18).label(NameLabel.FIRST).addProperty("version", 2).addProperty("lane", "B").value("RIGHT-").place().join());
			assertTrue(c.part().id(17).label(NameLabel.LAST).addProperty("version", 1).addProperty("lane", "A").value("-DONE").place().join());
			assertTrue(c.part().id(18).label(NameLabel.LAST).addProperty("version", 2).addProperty("lane", "B").value("-DONE").place().join());

			awaitTrue(() -> "LEFT-V1-DONE".equals(results.get(17)),
					"PBalanced configurator route should send version=1/lane=A carts to c17.v1",
					Duration.ofSeconds(5));
			awaitTrue(() -> "RIGHT-V2-DONE".equals(results.get(18)),
					"PBalanced configurator route should send version=2/lane=B carts to c17.v2",
					Duration.ofSeconds(5));

			assertFalse(c.part().id(19).label(NameLabel.FIRST).addProperty("version", 9).addProperty("lane", "Z").value("MISS").place().join());
		} finally {
			c.completeAndStop().join();
		}
	}

	private static void requireRedisAvailability() {
		String redisUri = RedisConnectionFactory.resolveRedisUri();
		try (JedisPooled jedis = RedisConnectionFactory.open(redisUri)) {
			Assumptions.assumeTrue("PONG".equals(jedis.ping()), "Redis did not respond with PONG at " + redisUri);
		} catch (Exception e) {
			Assumptions.assumeTrue(false, "Redis is not available at " + redisUri + ": " + e.getMessage());
		}
	}
}
