package org.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.conveyor.persistence.ack.AcknowledgeBuildingConveyor;
import org.conveyor.persistence.core.harness.PersistTestImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.cart.ShoppingCart;

public class AckBuilderConveyorTest {

	private final static Logger LOG = LoggerFactory.getLogger(AcknowledgeBuildingConveyor.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		LOG.debug("TEST");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws InterruptedException {
		
		Persist<Integer, Integer> p = new PersistTestImpl();

		AcknowledgeBuildingConveyor<Integer, Integer> abc = new AcknowledgeBuildingConveyor<>(p, null, null); //no forward, no cleaning
		abc.setIdleHeartBeat(10,TimeUnit.MILLISECONDS);
		
		CompletableFuture<List<Integer>> f = abc.future().id(1).get();
		
		abc.part().id(1).label(abc.CART).value(new ShoppingCart<Integer, Integer, String>(1, 1, "A")).place();
		abc.part().id(1).label(abc.CART).value(new ShoppingCart<Integer, Integer, String>(1, 2, "B")).place();
		abc.part().id(1).label(abc.CART).value(new ShoppingCart<Integer, Integer, String>(1, 3, "C")).place();
		
		abc.part().id(1).label(abc.ACK).value(1).place();
		abc.part().id(1).label(abc.COMPLETE).value(1).place();
		List<Integer> ids = f.join();
		System.out.println(ids);
	
	}

}
