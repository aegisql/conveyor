package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuilder;
import com.aegisql.conveyor.persistence.core.harness.PersistTestImpl;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AckBuilderTest {

	@BeforeAll
	public static void setUpBeforeClass() {
	}

	@AfterAll
	public static void tearDownAfterClass() {
	}

	@BeforeEach
	public void setUp() {
	}

	@AfterEach
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
