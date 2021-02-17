package com.aegisql.conveyor.persistence.jdbc;

import com.aegisql.conveyor.*;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.core.harness.*;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.converters.EnumConverter;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;
import org.apache.log4j.BasicConfigurator;
import org.junit.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class PersistentConveyorTest {

	JdbcPersistenceBuilder<Integer> persistenceBuilder = JdbcPersistenceBuilder.presetInitializer("derby", Integer.class)
			.schema("testConv").autoInit(true).setArchived();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
		Tester.removeDirectory("testConv");
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
	
	Persistence<Integer> getPersitence(String table) {
		try {						
			Thread.sleep(1000);
			return persistenceBuilder
					.partTable(table)
					.completedLogTable(table + "Completed")
					.labelConverter(TrioPart.class)
					.maxBatchSize(3)
					.minCompactSize(1000)
					.doNotSaveCartProperties("ignore_me","ignore_me_too")
					.build();

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	Persistence<Integer> getPersitenceWithField(String table) {
		try {						
			Thread.sleep(1000);
			return persistenceBuilder
					.partTable(table)
					.completedLogTable(table + "Completed")
					.addField(String.class, "ADDON")
					.labelConverter(TrioPart.class)
					.maxBatchSize(3)
					.minCompactSize(1000)
					.addUniqueFields("ADDON")
					.build();

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	Persistence<Integer> getPersitenceExp(String table) {
		try {
			Thread.sleep(1000);
			return persistenceBuilder
					.partTable(table)
					.completedLogTable(table + "Completed")
					.labelConverter(TrioPartExpireable.class)
					.maxBatchSize(3)
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Test
	public void veryBasicTest() throws Exception {
		Persistence<Integer> p = getPersitence("veryBasicTest");
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.wrapConveyor(tc);
	
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println(p);
		pc.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(p);
		assertEquals(1, tc.results.size());
	}

	@Test
	public void veryBasicTestWithAdditionalUniqField() throws Exception {
		Persistence<Integer> p = getPersitenceWithField("withAdditionalUniqField");
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.wrapConveyor(tc);
	
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").addProperty("ADDON", "A").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt2").addProperty("ADDON", "B").place().join();
		System.out.println(p);
		pc.part().id(1).label(TrioPart.NUMBER).value(1).addProperty("ADDON", "C").place().join();
		System.out.println(p);
		assertEquals(1, tc.results.size());
	}

	@Test(expected=CompletionException.class)
	public void veryBasicFailingTestWithAdditionalUniqField() throws Exception {
		Persistence<Integer> p = getPersitenceWithField("withAdditionalNonUniqField");
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.wrapConveyor(tc);
	
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").addProperty("ADDON", "A").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt2").addProperty("ADDON", "A").place().join();
	}

	
	@Test
	public void veryBasicTedbstWithIgnoredCart() throws Exception {
		Persistence<Integer> p = getPersitence("veryBasicTestIgnoreNumber");
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.wrapConveyor(tc);
	
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println(p);
		pc.part().id(1).label(TrioPart.NUMBER).value(1).addProperty("~", null).place().join();
		System.out.println(p);
		assertEquals(1, tc.results.size());
	}
	
	
	@Test
	public void simpleAckTest() throws Exception {
		Persistence<Integer> p = getPersitence("simpleAckTest");

		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.getConveyor(()->tc);
		pc.setAutoAcknowledge(false);
		
		AtomicReference<Acknowledge> ref = new AtomicReference<>();
		
		pc.resultConsumer().andThen(bin->{
			System.out.println("ACK:"+bin.acknowledge.isAcknowledged());
			ref.set(bin.acknowledge);
		}).set();
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println(p);
		pc.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(p);
		assertEquals(1, tc.results.size());
		assertNotNull(ref.get());
		assertFalse(ref.get().isAcknowledged());
//		assertFalse(p.isEmpty());
		ref.get().ack();
		assertTrue(ref.get().isAcknowledged());
		Thread.sleep(1000);
		System.out.println(p);
//		assertTrue(p.isEmpty());
	}

	
	@Test
	public void testEnumConverter() {
		EnumConverter<TrioPart>  ec = new EnumConverter<TrioPart>(TrioPart.class);
		String toP = ec.toPersistence(TrioPart.TEXT1);
		TrioPart fromP = ec.fromPersistence(toP);
		System.out.println("toString="+toP);
		System.out.println("fromString="+fromP);
	}
	
	@Test
	public void simpleReplayTest() throws Exception {
		Persistence<Integer> p1 = getPersitence("simpleReplayTest");
		TrioConveyor tc1 = new TrioConveyor();
		tc1.autoAcknowledgeOnStatus(Status.READY);
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc1 = p1.wrapConveyor(tc1);
		pc1.setName("TC1");
		pc1.part().id(1).label(TrioPart.TEXT1).value("txt1").addProperty("ignore_me", "a").addProperty("ignore_me_too", "b").place();
		pc1.part().id(1).label(TrioPart.TEXT2).value("txt2").addProperty("do_not_ignore_me", "x").place().join();
		System.out.println(p1);
		
		pc1.stop();
		TrioConveyor tc2 = new TrioConveyor();
		tc2.autoAcknowledgeOnStatus(Status.READY);
		
		Persistence<Integer> p2 = getPersitence("simpleReplayTest");
		//Must copy state from the previous persistence
		//assertFalse(p2.isEmpty());
		//p1 must be empty after moving data to p1. 
		//assertTrue(p1.isEmpty());
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc2 = p2.wrapConveyor(tc2);
		pc2.setName("TC2");
		pc2.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(tc2);
		assertEquals(0, tc1.results.size());
		assertEquals(1, tc2.results.size());
		System.out.println(tc2);
		System.out.println("P1="+p1);
		System.out.println("P2="+p2);
		//p2 must be empty after completion. 
		Thread.sleep(100);
		//assertTrue(p2.isEmpty());
	}

	@Test(expected=RuntimeException.class)
	public void failingEncryptionReplayTest() throws Exception {
		Persistence<Integer> p1 = persistenceBuilder
				.partTable("failingEncryptionReplayTest")
				.completedLogTable("failingEncryptionReplayTestCompleted")
				.labelConverter(new EnumConverter<TrioPart>(TrioPart.class))
				.encryptionSecret("dfgjksfgkjhd")
				.build();
		TrioConveyor tc1 = new TrioConveyor();
		tc1.autoAcknowledgeOnStatus(Status.READY);
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc1 = p1.wrapConveyor(tc1);
		pc1.setName("TC1");
		pc1.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc1.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println("P1 "+p1);
		
		pc1.stop();
		TrioConveyor tc2 = new TrioConveyor();
		tc2.autoAcknowledgeOnStatus(Status.READY);
		
		Persistence<Integer> p2 = persistenceBuilder
				.partTable("failingEncryptionReplayTest")
				.completedLogTable("failingEncryptionReplayTestCompleted")
				.labelConverter(new EnumConverter<>(TrioPart.class))
				.build();
		Thread.sleep(1000);
		//Must copy state from the previous persistence
		//assertFalse(p2.isEmpty());
		//p1 must be empty after moving data to p1. 
		//assertTrue(p1.isEmpty());
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc2 = p2.wrapConveyor(tc2);
		pc2.setName("TC2");

		System.out.println("------------");
		pc2.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(tc2);
		assertEquals(0, tc1.results.size());
		assertEquals(1, tc2.results.size());
		System.out.println(tc2);
		System.out.println("P1="+tc1);
		System.out.println("P2="+tc2);
		//p2 must be empty after completion. 
		Thread.sleep(100);
		//assertTrue(p2.isEmpty());
	}

	@Test
	public void staticBasicTest() throws Exception {
		Persistence<Integer> p = getPersitence("staticBasicTest");
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.wrapConveyor(tc);
		pc.staticPart().label(TrioPart.NUMBER).value(1).place().join();
	
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt2").place().get();
		System.out.println(tc);
		assertEquals(1, tc.results.size());
		//pc.stop();
	}


	@Test
	public void staticReplayTest() throws Exception {
		Persistence<Integer> p = getPersitence("staticReplayTest");
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.wrapConveyor(tc);
		pc.staticPart().label(TrioPart.NUMBER).value(1).place().join();
		pc.staticPart().label(TrioPart.NUMBER).value(2).place().join();
		pc.staticPart().label(TrioPart.NUMBER).value(3).place().join();

		Persistence<Integer> p2 = persistenceBuilder
				.partTable("staticReplayTest")
				.completedLogTable("staticReplayTestCompleted")
				.labelConverter(TrioPart.class)
				.build();
		TrioConveyor tc2 = new TrioConveyor();
		
		Thread.sleep(1000);
		
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc2 = p2.wrapConveyor(tc2);

		
		pc2.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc2.part().id(1).label(TrioPart.TEXT2).value("txt2").place().get();
		System.out.println(tc2);
		assertEquals(1, tc2.results.size());
		//pc.stop();
	}

	@Test
	public void multiBasicTest() throws Exception {
		Persistence<Integer> p = getPersitence("multiBasicTest");
		TrioConveyor tc = new TrioConveyor();
		tc.setDefaultBuilderTimeout(Duration.ofMillis(1000));
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.wrapConveyor(tc);
	
		pc.part().id(1).label(TrioPart.TEXT1).value("txt11").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt21").place();
		pc.part().id(2).label(TrioPart.TEXT1).value("txt12").place();
		pc.part().id(2).label(TrioPart.TEXT2).value("txt22").place().join();
		pc.part().foreach().label(TrioPart.NUMBER).value(1).place().join();
		Thread.sleep(1500);
		System.out.println(tc);
		assertEquals(2, tc.counter.get());
		//pc.stop();
	}

	@Test
	public void multiReloadTest() throws Exception {
		Persistence<Integer> p = getPersitence("multiBasicTest");
		TrioConveyor tc1 = new TrioConveyor();
		
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc1 = p.wrapConveyor(tc1);
		pc1.setName("TC1");
	
		pc1.part().id(1).label(TrioPart.TEXT1).value("txt11").place();
		pc1.part().id(2).label(TrioPart.TEXT1).value("txt12").place().join();

		pc1.part().foreach().label(TrioPart.NUMBER).value(1).place().join();
		Thread.sleep(1000);
		//pc1.stop();
		System.out.println("------------------------");
		p = getPersitence("multiBasicTest");
		TrioConveyor tc2 = new TrioConveyor();
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc2 = p.wrapConveyor(tc2);
		pc2.setName("TC2");
		pc2.part().id(1).label(TrioPart.TEXT2).value("txt21").place();
		pc2.part().id(2).label(TrioPart.TEXT2).value("txt22").place().join();
		Thread.sleep(1000);
		System.out.println(tc1);
		System.out.println(tc2);
		System.out.println("collector="+pc2.getCollectorSize());
		assertEquals(2, tc2.counter.get());
		//pc.stop();
	}


	@Test
	public void simpleBuilderSupplierTest() throws Exception {
		Persistence<Integer> p1 = getPersitence("simpleBuilderSupplierTest");
		TrioConveyor tc1 = new TrioConveyor();
		
		tc1.setReadinessEvaluator(Conveyor.getTesterFor(tc1).accepted(TrioPart.TEXT2,TrioPart.NUMBER));

		
		tc1.autoAcknowledgeOnStatus(Status.READY);
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc1 = p1.wrapConveyor(tc1);
		pc1.setName("TC1");
//		pc1.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		
		pc1.build().id(1).supplier(()->{
			TrioBuilder tb = new TrioBuilder();
			tb.setText1(tb, "TEXT1_PERSIST");
			return tb;
		}).create();
		
		pc1.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println(p1);
		
		pc1.stop();
		TrioConveyor tc2 = new TrioConveyor();
		tc2.autoAcknowledgeOnStatus(Status.READY);
		tc2.setReadinessEvaluator(Conveyor.getTesterFor(tc1).accepted(TrioPart.TEXT2,TrioPart.NUMBER));
		
		Persistence<Integer> p2 = getPersitence("simpleBuilderSupplierTest");
		//Must copy state from the previous persistence
		//assertFalse(p2.isEmpty());
		//p1 must be empty after moving data to p1. 
		//assertTrue(p1.isEmpty());
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc2 = p2.wrapConveyor(tc2);
		pc2.setName("TC2");
		pc2.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(tc2);
		assertEquals(0, tc1.results.size());
		assertEquals(1, tc2.results.size());
		System.out.println("P1="+p1);
		System.out.println("P2="+p2);
		//p2 must be empty after completion. 
		Thread.sleep(100);
		//assertTrue(p2.isEmpty());
		assertEquals("TEXT1_PERSIST", tc2.results.get(1).getText1());
	}


	@Test
	public void simpleResultConsumerTest() throws Exception {
		Persistence<Integer> p1 = getPersitence("simpleResultConsumerTest");
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p1.wrapConveyor(tc);
	
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").place();

		Persistence<Integer> p2 = getPersitence("simpleResultConsumerTest");
		TrioConveyor tc2 = new TrioConveyor();
		
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc2 = p2.wrapConveyor(tc2);
		pc2.resultConsumer().andThen(bin->{
			System.setProperty("PERSISTENT", "PERSISTENT "+bin);
		}).id(1).set();
		pc2.part().id(1).label(TrioPart.TEXT2).value("txt2").place();
		pc2.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(tc2);
		String sysProperty = System.getProperty("PERSISTENT");
		System.out.println(sysProperty);
		assertNotNull(sysProperty);

	
	}


	@Test
	public void simpleUnloadTest() throws Exception {
		Persistence<Integer> p1 = getPersitenceExp("simpleUnloadTest");
		TrioConveyorExpireable tc1 = new TrioConveyorExpireable();
		PersistentConveyor<Integer, SmartLabel<TrioBuilderExpireable>, Trio> pc1 = p1.wrapConveyor(tc1);
		pc1.unloadOnBuilderTimeout(true);
		pc1.setName("TC1");
		pc1.part().id(1).label(TrioPartExpireable.TEXT1).value("txt1").ttl(Duration.ofSeconds(100)).place();
		pc1.part().id(1).label(TrioPartExpireable.TEXT2).value("txt2").ttl(Duration.ofSeconds(100)).place();

		Thread.sleep(2000);
		
		pc1.part().id(1).label(TrioPartExpireable.NUMBER).value(1).ttl(Duration.ofSeconds(10)).place().join();
		System.out.println(tc1);
		assertEquals(1, tc1.results.size());
		System.out.println(tc1);
		//p2 must be empty after completion. 
		Thread.sleep(100);
		//assertTrue(p2.isEmpty());
	}

	@Test
	public void testByName() throws Exception{
		Persistence<Integer> p1 = getPersitence("nameTest");
		Persistence<Integer> p2 = Persistence.byName("com.aegisql.conveyor.persistence.derby.testConv:type=nameTest");
		Persistence<Integer> p3 = Persistence.lazySupplier("derby.testConv.nameTest").get();
		assertNotNull(p2);
		assertNotNull(p3);
		assertTrue(p1==p2);
		assertTrue(p1==p3);
	}
	
	@Test
	public void simpleCompactTest() throws Exception {
		Persistence<Integer> p1 = getPersitence("simpleCompactTest");
		TrioConveyor tc1 = new TrioConveyor();
		tc1.autoAcknowledgeOnStatus(Status.READY);
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc1 = p1.wrapConveyor(tc1);
		pc1.setName("TC1");
		pc1.part().id(1).label(TrioPart.TEXT1).value("txt1").addProperty("ignore_me", "a").addProperty("ignore_me_too", "b").place();
		pc1.part().id(1).label(TrioPart.TEXT2).value("txt2").addProperty("do_not_ignore_me", "x").place().join();
		Collection<Cart<Integer, ?, Object>> parts1 = p1.copy().getAllParts();
		assertEquals(2, parts1.size());
		pc1.compact(1).join();
		
		pc1.stop();
		
		Collection<Cart<Integer, ?, Object>> parts2 = p1.copy().getAllParts();
		assertEquals(1, parts2.size());
		assertEquals(LoadType.COMMAND, parts2.iterator().next().getLoadType());
		
		TrioConveyor tc2 = new TrioConveyor();
		tc2.autoAcknowledgeOnStatus(Status.READY);
		
		Persistence<Integer> p2 = getPersitence("simpleCompactTest");
		//Must copy state from the previous persistence
		//assertFalse(p2.isEmpty());
		//p1 must be empty after moving data to p1. 
		//assertTrue(p1.isEmpty());
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc2 = p2.wrapConveyor(tc2);
		pc2.setName("TC2");
		pc2.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(tc2);
		assertEquals(0, tc1.results.size());
		assertEquals(1, tc2.results.size());
		System.out.println(tc2);
		System.out.println("P1="+p1);
		System.out.println("P2="+p2);
		//p2 must be empty after completion. 
		Thread.sleep(100);
		//assertTrue(p2.isEmpty());
	}

	@Test
	public void multipleCompactTest() throws Exception {
		Persistence<Integer> p1 = getPersitence("multyCompactTest1");
		TrioConveyor tc1 = new TrioConveyor();
		tc1.autoAcknowledgeOnStatus(Status.READY);
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc1 = p1.wrapConveyor(tc1);
		pc1.setName("TC1");
		pc1.part().id(1).label(TrioPart.TEXT1).value("txt1").addProperty("ignore_me", "a").addProperty("ignore_me_too", "b").place();
		pc1.part().id(1).label(TrioPart.TEXT2).value("txt2").addProperty("do_not_ignore_me", "x").place();
		pc1.part().id(2).label(TrioPart.TEXT1).value("txt3").place();
		pc1.part().id(2).label(TrioPart.TEXT2).value("txt4").place().join();
		Collection<Cart<Integer, ?, Object>> parts1 = p1.copy().getAllParts();
		assertEquals(4, parts1.size());
		pc1.compact().join();
		
		pc1.stop();
		
		Collection<Cart<Integer, ?, Object>> parts2 = p1.copy().getAllParts();
		assertEquals(2, parts2.size());
		assertEquals(LoadType.COMMAND, parts2.iterator().next().getLoadType());
		
		TrioConveyor tc2 = new TrioConveyor();
		tc2.autoAcknowledgeOnStatus(Status.READY);
		
		Persistence<Integer> p2 = getPersitence("multyCompactTest1");
		//Must copy state from the previous persistence
		//assertFalse(p2.isEmpty());
		//p1 must be empty after moving data to p1. 
		//assertTrue(p1.isEmpty());
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc2 = p2.wrapConveyor(tc2);
		pc2.setName("TC2");
		pc2.part().id(1).label(TrioPart.NUMBER).value(1).place();
		pc2.part().id(2).label(TrioPart.NUMBER).value(2).place().join();
		System.out.println(tc2);
		assertEquals(0, tc1.results.size());
		assertEquals(2, tc2.results.size());
		System.out.println(tc2);
		System.out.println("P1="+p1);
		System.out.println("P2="+p2);
		//p2 must be empty after completion. 
		Thread.sleep(100);
		//assertTrue(p2.isEmpty());
	}

	@Test
	public void multipleCompactTestWithPredicate() throws Exception {
		Persistence<Integer> p1 = getPersitence("multyCompactTest2");
		TrioConveyor tc1 = new TrioConveyor();
		tc1.autoAcknowledgeOnStatus(Status.READY);
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc1 = p1.wrapConveyor(tc1);
		pc1.setName("TC1");
		pc1.part().id(1).label(TrioPart.TEXT1).value("txt1").addProperty("ignore_me", "a").addProperty("ignore_me_too", "b").place();
		pc1.part().id(1).label(TrioPart.TEXT2).value("txt2").addProperty("do_not_ignore_me", "x").place();
		pc1.part().id(2).label(TrioPart.TEXT1).value("txt3").place();
		pc1.part().id(2).label(TrioPart.TEXT2).value("txt4").place().join();
		Collection<Cart<Integer, ?, Object>> parts1 = p1.copy().getAllParts();
		assertEquals(4, parts1.size());
		pc1.compact(k->k%2==0).join();
		
		pc1.stop();
		
		List<Cart<Integer, ?, Object>> parts2 = new ArrayList<>(p1.copy().getAllParts());
		assertEquals(3, parts2.size());
		assertEquals(LoadType.COMMAND, parts2.get(2).getLoadType());
		
		TrioConveyor tc2 = new TrioConveyor();
		tc2.autoAcknowledgeOnStatus(Status.READY);
		
		Persistence<Integer> p2 = getPersitence("multyCompactTest2");
		//Must copy state from the previous persistence
		//assertFalse(p2.isEmpty());
		//p1 must be empty after moving data to p1. 
		//assertTrue(p1.isEmpty());
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc2 = p2.wrapConveyor(tc2);
		pc2.setName("TC2");
		pc2.part().id(1).label(TrioPart.NUMBER).value(1).place();
		pc2.part().id(2).label(TrioPart.NUMBER).value(2).place().join();
		System.out.println(tc2);
		assertEquals(0, tc1.results.size());
		assertEquals(2, tc2.results.size());
		System.out.println(tc2);
		System.out.println("P1="+p1);
		System.out.println("P2="+p2);
		//p2 must be empty after completion. 
		Thread.sleep(100);
		//assertTrue(p2.isEmpty());
	}

	@Test
	public void summatorWithAutoCompact() throws InterruptedException {
		Persistence<Integer> p1 = getPersitence("summatorWithAutoCompact");
		LastResultReference<Integer, Long> res = new LastResultReference<>();
		Conveyor<Integer,SummBuilder.SummStep,Long> ac = new AssemblingConveyor<>();
		ac.setBuilderSupplier(SummBuilder::new);
		ac.setName("ACC");
		ac.setReadinessEvaluator(Conveyor.getTesterFor(ac).accepted(SummBuilder.SummStep.DONE));
		ac.resultConsumer(res).set();
		PersistentConveyor<Integer, SummBuilder.SummStep,Long> pc = p1.wrapConveyor(ac);
		pc.setName("CPC1");
		PartLoader pl = pc.part().id(1).label(SummBuilder.SummStep.ADD);
		CompletableFuture<Boolean> f = null;
		for(long i = 1; i < 2001; i++) {
			f = pl.value(i).place();
		}
		f.join();
		Collection<Cart<Integer, ?, Object>> parts1 = p1.copy().getAllParts();
		System.out.println(parts1);
		assertEquals(2, parts1.size());//MEMENTO,i==2000
		pl.label(SummBuilder.SummStep.DONE).place().join();
		System.out.println(res);
		assertEquals(new Long(2001000),res.getCurrent());
	}
	
}
