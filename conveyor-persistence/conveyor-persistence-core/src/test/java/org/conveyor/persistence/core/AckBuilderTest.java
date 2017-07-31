package org.conveyor.persistence.core;

import static org.junit.Assert.*;

import org.conveyor.persistence.ack.AcknowledgeBuilder;
import org.conveyor.persistence.core.harness.PersistTestImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.ShoppingCart;

public class AckBuilderTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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
		
		AcknowledgeBuilder<Integer, Integer> ab = new AcknowledgeBuilder<>(p, null);
		
		ab.processCart(ab, new ShoppingCart<>(1, 1, "A"));
		ab.processCart(ab, new ShoppingCart<>(1, 2, "B"));
		ab.processCart(ab, new ShoppingCart<>(1, 3, "C"));
		ab.processCart(ab, new ShoppingCart<>(1, 4, "D"));
		
		assertFalse(ab.test());
		ab.setAckKey(ab, 1);
		assertFalse(ab.test());
		ab.complete(ab, Status.READY);
		assertTrue(ab.test());
		assertEquals(4, ab.get().size());
		System.out.println(ab.get());
		
	}

}
