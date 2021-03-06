/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import org.junit.*;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

// TODO: Auto-generated Javadoc
/**
 * The Class BuildingSiteTest.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class BuildingSiteTest {

	/**
	 * Sets the up before class.
	 *
	 * @throws Exception the exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * Tear down after class.
	 *
	 * @throws Exception the exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Sets the up.
	 *
	 * @throws Exception the exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Tear down.
	 *
	 * @throws Exception the exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test not ready.
	 */
	@Test(expected=IllegalStateException.class)
	public void testNotReady() {
		Cart<Integer,String,String> c = new ShoppingCart<>(1,"v1","l",100,TimeUnit.MILLISECONDS);

		BuildingSite<Integer, String, Cart<Integer,?,String>, User> bs = new BuildingSite<>
		(
				c, 
				() -> { return new UserBuilder();},
				(label,value,builder)-> { }, 
				(state,builder)->{return false;}, 
				null,
				100, TimeUnit.MILLISECONDS,false,false,false,0,false,null,null,null,null);
		assertNull(bs.getLastError());
		BuildingSite.Memento memento = bs.getMemento();
		memento.getTimestamp();
		memento.getExpirationTime();
		memento.getCreationTime();
		BuildingSite.NON_LOCKING_LOCK.lock();
		try {
			BuildingSite.NON_LOCKING_LOCK.lockInterruptibly();
			assertTrue(BuildingSite.NON_LOCKING_LOCK.tryLock(1,TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			fail("not expected");
		}
		assertTrue(BuildingSite.NON_LOCKING_LOCK.tryLock());
		BuildingSite.NON_LOCKING_LOCK.newCondition();

		User u = bs.build();
	}

	/**
	 * Test ready.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test()
	public void testReady() throws InterruptedException {
		ShoppingCart<Integer,String,String> c = new ShoppingCart<>(1,"v1","l",100,TimeUnit.MILLISECONDS);

		BuildingSite<Integer, String, Cart<Integer,?,String>, User> bs = new BuildingSite<>
		(
				c, 
				() -> { return new UserBuilder();},
				(label,value,builder)-> { }, 
				(state,builder)->{return true;}, 
				null,
				100, TimeUnit.MILLISECONDS,false,false,false,0,false,null,null,(a)->{}, null);
		assertEquals(0, bs.getAcceptCount());
		assertEquals(Status.WAITING_DATA, bs.getStatus());

		bs.accept(new ShoppingCart<>(1,"XXX","l",100,TimeUnit.MILLISECONDS));
		assertNotNull(bs.getLastCart());
		User u = bs.build();
		assertNotNull(u);
		assertEquals(1, bs.getAcceptCount());
		Thread.sleep(110);
		assertEquals(Status.READY, bs.getStatus());
		assertEquals(0, bs.getAcceptedCarts().size());
		assertFalse(bs.getAcknowledge().isAcknowledged());
		bs.getAcknowledge().ack();
		assertNotNull(bs.getBuilder());
		assertNotNull(bs.getLock());
		assertNotNull(bs.getCreatingCart());

	}

	@Test()
	public void testProperties() throws InterruptedException {
		ShoppingCart<Integer,String,String> c = new ShoppingCart<>(1,"v1","l",100,TimeUnit.MILLISECONDS);
		c.addProperty("A", 1);
		c.addProperty("B", "X");
		BuildingSite<Integer, String, Cart<Integer,?,String>, User> bs = new BuildingSite<>
		(
				c, 
				() -> { return new UserBuilder();},
				(label,value,builder)-> { }, 
				(state,builder)->{return true;}, 
				null,
				100, TimeUnit.MILLISECONDS,false,false,false,0,false,null,null,null, null);

		bs.addProperties(c.getAllProperties());
		
		assertTrue(bs.getProperties().size() == 2);
		assertTrue(bs.getProperties().containsKey("A"));
		assertTrue(bs.getProperties().containsKey("B"));
		
	}

}
