package com.aegisql.conveyor.persistence.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
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
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.core.harness.Trio;
import com.aegisql.conveyor.persistence.core.harness.TrioBuilder;
import com.aegisql.conveyor.persistence.core.harness.TrioConveyor;
import com.aegisql.conveyor.persistence.core.harness.TrioPart;
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

	@Test
	public void veryBasicTest() throws Exception {
		Persistence<Integer> p = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
				.partTable("veryBasicTest")
				.completedLogTable("veryBasicTestCompleted")
				.labelConverter(TrioPart.class)
				.build();
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc, 3);
	
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println(p);
		pc.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(p);
		assertEquals(1, tc.results.size());
	}

	
	@Test
	public void simpleAckTest() throws Exception {
		Persistence<Integer> p = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
				.partTable("simpleAckTest")
				.completedLogTable("simpleAckTestCompleted")
				.labelConverter(new EnumConverter<>(TrioPart.class))
				.build();
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, ()->tc, 3);
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
		Persistence<Integer> p1 = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
				.partTable("simpleReplayTest")
				.completedLogTable("simpleReplayTestCompleted")
				.labelConverter(new EnumConverter<TrioPart>(TrioPart.class))
				.build();
		TrioConveyor tc1 = new TrioConveyor();
		tc1.autoAcknowledgeOnStatus(Status.READY);
		PersistentConveyor<Integer, TrioPart, Trio> pc1 = new PersistentConveyor(p1, tc1, 3);
		pc1.setName("TC1");
		pc1.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc1.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println(p1);
		
		pc1.stop();
		TrioConveyor tc2 = new TrioConveyor();
		tc2.autoAcknowledgeOnStatus(Status.READY);
		
		Persistence<Integer> p2 = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
				.partTable("simpleReplayTest")
				.completedLogTable("simpleReplayTestCompleted")
				.labelConverter(new EnumConverter<>(TrioPart.class))
				.build();
		//Must copy state from the previous persistence
		//assertFalse(p2.isEmpty());
		//p1 must be empty after moving data to p1. 
		//assertTrue(p1.isEmpty());
		PersistentConveyor<Integer, TrioPart, Trio> pc2 = new PersistentConveyor(p2, tc2, 3);
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
		PersistentConveyor<Integer, TrioPart, Trio> pc1 = new PersistentConveyor(p1, tc1, 3);
		pc1.setName("TC1");
		pc1.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc1.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println(p1);
		
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
		//Must copy state from the previous persistence
		//assertFalse(p2.isEmpty());
		//p1 must be empty after moving data to p1. 
		//assertTrue(p1.isEmpty());
		PersistentConveyor<Integer, TrioPart, Trio> pc2 = new PersistentConveyor(p2, tc2, 3);
		pc2.setName("TC2");
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
		Persistence<Integer> p = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
				.partTable("staticBasicTest")
				.completedLogTable("staticBasicTestCompleted")
				.labelConverter(TrioPart.class)
				.build();
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc, 3);
		pc.staticPart().label(TrioPart.NUMBER).value(1).place().join();
	
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt2").place().get();
		System.out.println(tc);
		assertEquals(1, tc.results.size());
		//pc.stop();
	}


	@Test
	public void staticReplayTest() throws Exception {
		Persistence<Integer> p = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
				.partTable("staticReplayTest")
				.completedLogTable("staticReplayTestCompleted")
				.labelConverter(TrioPart.class)
				.build();
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc, 3);
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
		
		PersistentConveyor<Integer, TrioPart, Trio> pc2 = new PersistentConveyor(p2, tc2, 3);

		
		pc2.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc2.part().id(1).label(TrioPart.TEXT2).value("txt2").place().get();
		System.out.println(tc2);
		assertEquals(1, tc2.results.size());
		//pc.stop();
	}

	@Test
	public void multiBasicTest() throws Exception {
		Persistence<Integer> p = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
				.partTable("multiBasicTest")
				.completedLogTable("multiBasicTestCompleted")
				.labelConverter(TrioPart.class)
				.build();
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc, 3);
	
		pc.part().id(1).label(TrioPart.TEXT1).value("txt11").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt21").place();
		pc.part().id(2).label(TrioPart.TEXT1).value("txt12").place();
		pc.part().id(2).label(TrioPart.TEXT2).value("txt22").place();
		pc.part().foreach().label(TrioPart.NUMBER).value(1).place().join();
		Thread.sleep(1000);

		System.out.println(tc);

		//pc.stop();
	}


	@Test
	public void simpleBuilderSupplierTest() throws Exception {
		Persistence<Integer> p1 = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
				.partTable("simpleBuilderSupplierTest")
				.completedLogTable("simpleBuilderSupplierTestCompleted")
				.labelConverter(new EnumConverter<TrioPart>(TrioPart.class))
				.build();
		TrioConveyor tc1 = new TrioConveyor();
		
		tc1.setReadinessEvaluator(Conveyor.getTesterFor(tc1).accepted(TrioPart.TEXT2,TrioPart.NUMBER));

		
		tc1.autoAcknowledgeOnStatus(Status.READY);
		PersistentConveyor<Integer, TrioPart, Trio> pc1 = new PersistentConveyor(p1, tc1, 3);
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
		
		Persistence<Integer> p2 = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
				.partTable("simpleBuilderSupplierTest")
				.completedLogTable("simpleBuilderSupplierTestCompleted")
				.labelConverter(new EnumConverter<>(TrioPart.class))
				.build();
		//Must copy state from the previous persistence
		//assertFalse(p2.isEmpty());
		//p1 must be empty after moving data to p1. 
		//assertTrue(p1.isEmpty());
		PersistentConveyor<Integer, TrioPart, Trio> pc2 = new PersistentConveyor(p2, tc2, 3);
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
		Persistence<Integer> p = DerbyPersistence
				.forKeyClass(Integer.class)
				.schema("testConv")
				.partTable("simpleResultConsumerTest")
				.completedLogTable("simpleResultConsumerTestCompleted")
				.labelConverter(TrioPart.class)
				.build();
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc, 3);
	
		pc.resultConsumer().andThen(bin->{
			System.out.println("PERSISTENT "+bin);
		}).id(1).set();
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println(p);
		pc.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(tc);
		assertEquals(1, tc.results.size());
	}

	
}
