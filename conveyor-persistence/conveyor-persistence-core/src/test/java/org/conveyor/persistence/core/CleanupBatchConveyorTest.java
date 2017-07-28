package org.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;

import org.apache.log4j.BasicConfigurator;
import org.conveyor.persistence.cleanup.PersistenceCleanupBatchConveyor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.ShoppingCart;

public class CleanupBatchConveyorTest {

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
	public void test() {
		Persist<Integer, Integer> p = new PersistTestImpl();
		
		p.saveCart(1, new ShoppingCart<>(1, 2, "A"));
		p.saveCart(2, new ShoppingCart<>(1, 3, "B"));
		p.saveAcknowledge(1);
		
		PersistenceCleanupBatchConveyor<Integer, Integer> pcc = new PersistenceCleanupBatchConveyor<>(p, 3);
		
		CompletableFuture<Runnable> f = pcc.future().get();
		
		pcc.part().label(pcc.CART_ID).value(1).place();
		pcc.part().label(pcc.CART_ID).value(2).place();
		pcc.part().label(pcc.KEY).value(1).place();

		assertEquals(2, p.getAllCartIds(1).size());
		Runnable r = f.join();
		assertNotNull(r);
		assertEquals(0, p.getAllCartIds(1).size());
		
	}

}
