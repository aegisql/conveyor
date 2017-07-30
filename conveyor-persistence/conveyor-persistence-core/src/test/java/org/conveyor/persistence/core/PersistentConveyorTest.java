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
	public void test() throws InterruptedException {
		Persist<Integer, Integer> p = new PersistTestImpl();
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc, 3);
	
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt2").place();
		pc.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(p);
		Thread.sleep(1000);
		System.out.println(p);
	}

}
