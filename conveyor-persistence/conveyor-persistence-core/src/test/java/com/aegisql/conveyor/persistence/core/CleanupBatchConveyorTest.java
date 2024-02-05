package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.cleanup.PersistenceCleanupBatchConveyor;
import com.aegisql.conveyor.persistence.core.harness.PersistTestImpl;
import org.apache.log4j.BasicConfigurator;
import org.junit.*;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CleanupBatchConveyorTest {

	@BeforeClass
	public static void setUpBeforeClass() {
		BasicConfigurator.configure();
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void test() {
		Persistence<Integer> p = new PersistTestImpl();
		
		p.savePart(1, new ShoppingCart<>(1, 2, "A"));
		p.savePartId(1, 1);
		p.savePart(2, new ShoppingCart<>(1, 3, "B"));
		p.savePartId(1, 2);
		p.saveCompletedBuildKey(1);
		
		PersistenceCleanupBatchConveyor<Integer> pcc = new PersistenceCleanupBatchConveyor<>(p);
		
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
