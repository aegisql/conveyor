/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.aegisql.conveyor.validation.CommonValidators;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.ShoppingCart;

import static org.junit.Assert.*;

// TODO: Auto-generated Javadoc
/**
 * The Class CartTest.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class CartTest {

	/** The running. */
	private boolean running = true;
	
	/**
	 * Sets the up before class.
	 *
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
	}

	/**
	 * Tear down after class.
	 *
	 */
	@AfterClass
	public static void tearDownAfterClass() {
	}

	/**
	 * Sets the up.
	 *
	 */
	@Before
	public void setUp() {
	}

	/**
	 * Tear down.
	 *
	 */
	@After
	public void tearDown() {
	}

	/**
	 * Test.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void test() throws InterruptedException {
		ShoppingCart<String,String,String> c = new ShoppingCart<>("k","v1","l",100,TimeUnit.MILLISECONDS);
		
		assertFalse(c.expired());
		
		long delay = c.toDelayed().getDelay(TimeUnit.MILLISECONDS);
		
		assertTrue(delay > 0);
		assertTrue(delay <= 100);
		
		Thread.sleep(110);
		
		assertTrue(c.expired());
		
		delay = c.toDelayed().getDelay(TimeUnit.MILLISECONDS);
		
		assertTrue(delay < 0);

	}

	/**
	 * Unexpireable test.
	 */
	@Test
	public void unexpireableTest() {
		Cart<String,String,String> c = new ShoppingCart<>("k","v1","l");
		
		assertFalse(c.expired());
		long delay = c.toDelayed().getDelay(TimeUnit.MILLISECONDS);
		assertTrue(delay > 0);

	}

	/**
	 * Test closure.
	 */
	@Test
	public void testClosure() {
		Consumer<String> c = s->{
			System.out.println(s+running);
		};
		
		c.accept("running before=");
		running=false;
		c.accept("running after=");
		
	}
	@Test
	public void testProperties() {
		Cart<String,String,String> c = new ShoppingCart<>("k","v1","l");
		c.addProperty("A", 1);
		assertTrue(c.getAllProperties().size() > 0);
		assertEquals(Integer.valueOf(1), c.getProperty("A", Integer.class));
		
		Cart<String,String,String> n = c.copy();

		assertTrue(n.getAllProperties().size() > 0);
		assertEquals(Integer.valueOf(1), n.getProperty("A", Integer.class));

	}
	
	@Test
	public void testCartExplicitPriority() {
		Cart<String,String,String> c1 = new ShoppingCart<>("k","v1","l1",0,0,null,LoadType.PART,0);
		Cart<String,String,String> c2 = new ShoppingCart<>("k","v2","l2",0,0,null,LoadType.PART,1);
		
		Queue<Cart<String,?,?>> q1 = new PriorityQueue<>();
		q1.add(c1);
		q1.add(c2);
		
		assertEquals(c2, q1.poll());
		assertEquals(c1, q1.poll());

		q1.add(c2);
		q1.add(c1);

		assertEquals(c2, q1.poll());
		assertEquals(c1, q1.poll());
		
	}

	@Test
	public void testCartDefaultPriorityOrderInQueue() {		
		List<Cart<Integer,String,String>> carts = new ArrayList<>();
		Queue<Cart<Integer,?,?>> q = new PriorityQueue<>();
		for(int i = 0; i < 100; i++) {
			Cart<Integer,String,String> c = new ShoppingCart<>(i,"v"+i,"l",0,0,null,LoadType.PART,0);
			q.add(c);
			if(i%5==0) {
				q.add(q.poll());
			}
		}
		for(int i = 0; i < 100; i++) {
			Cart<Integer,?,?> c = q.poll();
			assertEquals(Integer.valueOf(i), c.getKey());
		}
	}

	@Test
	public void nullValueCastingShouldWorkTest() {
		Cart<String,String,String> c1 = new ShoppingCart<>("k",null,"l1",0,0,null,LoadType.PART,0);
		assertNull(c1.getValue(String.class));
	}

	@Test(expected = NullPointerException.class)
	public void cartNullValueValidationTests() {
		Cart c1 = new ShoppingCart<>("k",null,"l1",0,0,null,LoadType.PART,0);
		CommonValidators.CART_VALUE_NOT_NULL().accept(c1);
	}
}
