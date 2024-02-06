package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuildingConveyor;
import com.aegisql.conveyor.persistence.core.harness.PersistTestImpl;
import org.apache.log4j.BasicConfigurator;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AckBuilderConveyorTest {

	private final static Logger LOG = LoggerFactory.getLogger(AcknowledgeBuildingConveyor.class);
	
	@BeforeAll
	public static void setUpBeforeClass() {
		BasicConfigurator.configure();
	}

	@AfterAll
	public static void tearDownAfterClass() {
	}

	@BeforeEach
	public void setUp() {
		LOG.debug("TEST");
	}

	@AfterEach
	public void tearDown() {
	}

	@Test
	public void test() {
		
		Persistence<Integer> p = new PersistTestImpl();

		AcknowledgeBuildingConveyor<Integer> abc = new AcknowledgeBuildingConveyor<>(p, null); //no forward, no cleaning
		abc.setIdleHeartBeat(10,TimeUnit.MILLISECONDS);
		
		CompletableFuture<Boolean> f = abc.future().id(1).get();
		
		abc.part().id(1).label(abc.CART).value(new ShoppingCart<Integer, Integer, String>(1, 1, "A")).place();
		abc.part().id(1).label(abc.CART).value(new ShoppingCart<Integer, Integer, String>(1, 2, "B")).place();
		abc.part().id(1).label(abc.CART).value(new ShoppingCart<Integer, Integer, String>(1, 3, "C")).place();
		
		abc.part().id(1).label(abc.COMPLETE).value(new AcknowledgeStatus<>(1, Status.READY,null)).place();
		Boolean ids = f.join();
		System.out.println(ids);
	
	}

}
