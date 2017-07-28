package org.conveyor.persistence.core;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.ShoppingCart;

public class PersistTestImplTest {

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

		PersistTestImpl p = new PersistTestImpl();
		assertEquals(Integer.valueOf(1), p.getUniqueId());
		
		p.saveCart(p.getUniqueId(),new ShoppingCart<Integer, Integer, Integer>(1, 1, 1));
		p.saveCart(p.getUniqueId(),new ShoppingCart<Integer, Integer, Integer>(1, 2, 2));
		p.saveCart(p.getUniqueId(),new ShoppingCart<Integer, Integer, Integer>(2, 1, 1));
		
		assertEquals(2, p.getAllCartIds(1).size());
		assertEquals(1, p.getAllCartIds(2).size());
		assertEquals(0, p.getAllCartIds(3).size());
		
		assertNotNull(p.getCart(2));
		assertNotNull(p.getCart(3));
		assertNotNull(p.getCart(4));
		assertNull(p.getCart(5));

		p.saveAcknowledge(1);
		
	}

}
