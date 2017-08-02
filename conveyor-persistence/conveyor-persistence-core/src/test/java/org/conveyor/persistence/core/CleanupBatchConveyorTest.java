package org.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;

import org.apache.log4j.BasicConfigurator;
import org.conveyor.persistence.cleanup.PersistenceCleanupBatchConveyor;
import org.conveyor.persistence.core.harness.PersistTestImpl;
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
		Persistence<Integer> p = new PersistTestImpl();
		
		p.savePart(1, new ShoppingCart<>(1, 2, "A"));
		p.savePartId(1, 1);
		p.savePart(2, new ShoppingCart<>(1, 3, "B"));
		p.savePartId(1, 2);
		p.saveCompletedBuildKey(1);
		
		PersistenceCleanupBatchConveyor<Integer> pcc = new PersistenceCleanupBatchConveyor<>(p, 3);
		
		CompletableFuture<Runnable> f = pcc.future().get();
		
		pcc.part().label(pcc.CART_ID).value(1L).place();
		pcc.part().label(pcc.CART_ID).value(2L).place();
		pcc.part().label(pcc.KEY).value(1).place();

		assertEquals(2, p.getAllPartIds(1).size());
		Runnable r = f.join();
		assertNotNull(r);
		assertEquals(0, p.getAllPartIds(1).size());
		
	}

}
