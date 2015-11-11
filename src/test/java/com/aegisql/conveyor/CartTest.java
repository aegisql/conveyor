package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CartTest {

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
	public void test() throws InterruptedException {
		Cart<String,String,String> c = new Cart<>("k","v1","l",100,TimeUnit.MILLISECONDS);
		
		assertFalse(c.expired());
		
		Cart<String,String,String> c2 = c.nextCart("v2");
		assertFalse(c2.expired());
		assertEquals(c.getKey(),c2.getKey());
		assertEquals(c.getLabel(),c2.getLabel());
		assertNotEquals(c.getValue(), c2.getValue());
		
		Cart<String,String,String> c3 = c.nextCart("v2","l2");
		assertFalse(c3.expired());
		assertEquals(c.getKey(),c3.getKey());
		assertNotEquals(c.getLabel(),c3.getLabel());
		assertNotEquals(c.getValue(), c3.getValue());
		
		long delay = c.getDelay(TimeUnit.MILLISECONDS);
		
		assertTrue(delay > 0);
		assertTrue(delay <= 100);
		
		Thread.sleep(110);
		
		assertTrue(c.expired());
		assertTrue(c2.expired());
		assertTrue(c3.expired());
		
		delay = c.getDelay(TimeUnit.MILLISECONDS);
		
		assertTrue(delay < 0);

	}

	@Test
	public void unexpireableTest() {
		Cart<String,String,String> c = new Cart<>("k","v1","l");
		
		assertFalse(c.expired());
		long delay = c.getDelay(TimeUnit.MILLISECONDS);
		assertTrue(delay > 0);

	}
	
}
