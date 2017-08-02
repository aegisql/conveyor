package org.conveyor.persistence.core;

import static org.junit.Assert.*;

import org.apache.log4j.BasicConfigurator;
import org.conveyor.persistence.core.harness.PersistTestImpl;
import org.conveyor.persistence.core.harness.Trio;
import org.conveyor.persistence.core.harness.TrioConveyor;
import org.conveyor.persistence.core.harness.TrioPart;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PersistentConveyorTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
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
	public void veryBasicTest() throws InterruptedException {
		Persist<Integer> p = new PersistTestImpl();
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
	public void simpleReplayTest() throws InterruptedException {
		PersistTestImpl p1 = new PersistTestImpl();
		TrioConveyor tc1 = new TrioConveyor();
		
		PersistentConveyor<Integer, TrioPart, Trio> pc1 = new PersistentConveyor(p1, tc1, 3);
		pc1.setName("TC1");
		pc1.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc1.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println(p1);
		
		pc1.stop();
		TrioConveyor tc2 = new TrioConveyor();
		
		PersistTestImpl p2 = new PersistTestImpl(p1);
		//Must copy state from the previous persistence
		assertFalse(p2.isEmpty());
		//p1 must be empty after moving data to p1. 
		assertTrue(p1.isEmpty());
		PersistentConveyor<Integer, TrioPart, Trio> pc2 = new PersistentConveyor(p2, tc2, 3);
		pc2.setName("TC2");
		pc2.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(p1);
		assertEquals(0, tc1.results.size());
		assertEquals(1, tc2.results.size());
		System.out.println(tc2);
		System.out.println("P1="+p1);
		System.out.println("P2="+p2);
		//p2 must be empty after completion. 
		Thread.sleep(100);
		assertTrue(p2.isEmpty());
	}

	
	
}
