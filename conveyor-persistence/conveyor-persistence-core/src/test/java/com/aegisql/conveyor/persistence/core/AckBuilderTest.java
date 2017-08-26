package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuilder;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.harness.PersistTestImpl;

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

		Persistence<Integer> p = new PersistTestImpl();
		
		AcknowledgeBuilder<Integer> ab = new AcknowledgeBuilder<>(p, null,null);
		
		ab.processCart(ab, new ShoppingCart<>(1, 1, "A"));
		ab.processCart(ab, new ShoppingCart<>(1, 2, "B"));
		ab.processCart(ab, new ShoppingCart<>(1, 3, "C"));
		ab.processCart(ab, new ShoppingCart<>(1, 4, "D"));
		
		assertFalse(ab.test());
		ab.keyReady(ab, 1);
		assertFalse(ab.test());
		ab.complete(ab, new AcknowledgeStatus<>(1, Status.READY, null));
		assertTrue(ab.test());
		assertEquals(4, ab.get().size());
		System.out.println(ab.get());
		
	}

}
