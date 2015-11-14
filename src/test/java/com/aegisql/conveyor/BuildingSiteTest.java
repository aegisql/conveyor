/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;
import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.BuildingSite;
import com.aegisql.conveyor.BuildingSite.Status;
import com.aegisql.conveyor.Cart;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import com.aegisql.conveyor.user.UserBuilderDelayed;
import com.aegisql.conveyor.user.UserBuilderSmart;

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
		Cart<Integer,String,String> c = new Cart<>(1,"v1","l",100,TimeUnit.MILLISECONDS);

		BuildingSite<Integer, String, Cart<Integer,?,String>, User> bs = new BuildingSite<>
		(
				c, 
				() -> { return new UserBuilder();},
				(label,value,builder)-> { }, 
				(lot,builder)->{return false;}, 
				100, TimeUnit.MILLISECONDS);
		assertNull(bs.getLastError());
		User u = bs.build();
	}

	/**
	 * Test ready.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test()
	public void testReady() throws InterruptedException {
		Cart<Integer,String,String> c = new Cart<>(1,"v1","l",100,TimeUnit.MILLISECONDS);

		BuildingSite<Integer, String, Cart<Integer,?,String>, User> bs = new BuildingSite<>
		(
				c, 
				() -> { return new UserBuilder();},
				(label,value,builder)-> { }, 
				(lot,builder)->{return true;}, 
				100, TimeUnit.MILLISECONDS);
		assertEquals(0, bs.getAcceptCount());
		assertEquals(Status.WAITING_DATA, bs.getStatus());
		bs.accept(c.nextCart("XXX"));
		User u = bs.build();
		assertNotNull(u);
		assertEquals(1, bs.getAcceptCount());
		assertFalse(bs.expired());
		Thread.sleep(110);
		assertTrue(bs.expired());
		assertEquals(Status.READY, bs.getStatus());
	}

	/**
	 * Test ready delayed.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test()
	public void testReadyDelayed() throws InterruptedException {
		Cart<Integer,String,String> c = new Cart<>(1,"v1","l",100,TimeUnit.MILLISECONDS);

		BuildingSite<Integer, String, Cart<Integer,?,String>, User> bs = new BuildingSite<>
		(
				c, 
				() -> { return new UserBuilderDelayed(100);},
				(label,value,builder)-> { }, 
				(lot,builder)->{return true;}, 
				100, TimeUnit.MILLISECONDS);
		assertEquals(0, bs.getAcceptCount());
		assertEquals(Status.WAITING_DATA, bs.getStatus());
		bs.accept(c.nextCart("XXX"));
		User u = bs.build();
		assertNotNull(u);
		assertEquals(1, bs.getAcceptCount());
		assertFalse(bs.expired());
		Thread.sleep(110);
		assertTrue(bs.expired());
		assertEquals(Status.READY, bs.getStatus());
	}

}
