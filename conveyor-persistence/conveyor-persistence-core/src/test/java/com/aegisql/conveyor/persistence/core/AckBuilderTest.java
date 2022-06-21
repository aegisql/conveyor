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
	public static void setUpBeforeClass() {
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
		
		AcknowledgeBuilder<Integer> ab = new AcknowledgeBuilder<>(p, null,null);
		
		AcknowledgeBuilder.processCart(ab, new ShoppingCart<>(1, 1, "A"));
		AcknowledgeBuilder.processCart(ab, new ShoppingCart<>(1, 2, "B"));
		AcknowledgeBuilder.processCart(ab, new ShoppingCart<>(1, 3, "C"));
		AcknowledgeBuilder.processCart(ab, new ShoppingCart<>(1, 4, "D"));
		
		assertFalse(ab.test());
		assertFalse(ab.test());
		AcknowledgeBuilder.complete(ab, new AcknowledgeStatus<>(1, Status.READY, null));
		assertTrue(ab.test());
		assertTrue(ab.get());
		System.out.println(ab.get());
		
	}

}
