package com.aegisql.conveyor.persistence.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.Acknowledge;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.core.harness.Trio;
import com.aegisql.conveyor.persistence.core.harness.TrioBuilder;
import com.aegisql.conveyor.persistence.core.harness.TrioBuilderExpireable;
import com.aegisql.conveyor.persistence.core.harness.TrioConveyor;
import com.aegisql.conveyor.persistence.core.harness.TrioConveyorExpireable;
import com.aegisql.conveyor.persistence.core.harness.TrioPart;
import com.aegisql.conveyor.persistence.core.harness.TrioPartExpireable;
import com.aegisql.conveyor.persistence.jdbc.converters.EnumConverter;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.DerbyPersistence;

public class PersistentConveyorTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
		String conveyor_db_path = "testConv";
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
	
	Persistence<Integer> getPersitence(String table) {
		try {
			Thread.sleep(1000);
			return DerbyPersistence
					.forKeyClass(Integer.class)
					.schema("testConv")
					.partTable(table)
					.completedLogTable(table+"Completed")
					.labelConverter(TrioPart.class)
					.whenArchiveRecords().markArchived()
					.maxBatchSize(3)
					.doNotSaveProperties("ignore_me","ignore_me_too")
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	Persistence<Integer> getPersitenceExp(String table) {
		try {
			Thread.sleep(1000);
			return DerbyPersistence
					.forKeyClass(Integer.class)
					.schema("testConv")
					.partTable(table)
					.completedLogTable(table+"Completed")
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
		Persistence<Integer> p1 = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
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
		
		Persistence<Integer> p2 = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
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

		Persistence<Integer> p2 = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
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
	
		pc.resultConsumer().andThen(bin->{
			System.setProperty("PERSISTENT", "PERSISTENT "+bin);
		}).id(1).set();
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").place();

		Persistence<Integer> p2 = getPersitence("simpleResultConsumerTest");
		TrioConveyor tc2 = new TrioConveyor();
		
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc2 = p2.wrapConveyor(tc2);
		pc2.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
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

	
}
