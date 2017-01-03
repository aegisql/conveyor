package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExpireableTest {

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
	public void test1() {
		Expireable e = new Expireable() {
			@Override
			public long getExpirationTime() {
				return 0;
			}
		};
		assertEquals(0, e.getExpirationTime());
		assertFalse(e.isExpireable());
		assertFalse(e.expired());
	}

	@Test
	public void test2() {
		Expireable e = new Expireable() {
			@Override
			public long getExpirationTime() {
				return 1;
			}
		};
		assertEquals(1, e.getExpirationTime());
		assertTrue(e.isExpireable());
		assertTrue(e.expired());
	}

	@Test
	public void test3() {
		Expireable e = new Expireable() {
			@Override
			public long getExpirationTime() {
				return Long.MAX_VALUE;
			}
		};
		assertEquals(Long.MAX_VALUE, e.getExpirationTime());
		assertTrue(e.isExpireable());
		assertFalse(e.expired());
	}


	@Test
	public void test4() throws InterruptedException {
		long expireAt = System.currentTimeMillis() + 100;
		Expireable e = new Expireable() {
			@Override
			public long getExpirationTime() {
				return expireAt;
			}
		};
		assertEquals(expireAt, e.getExpirationTime());
		assertTrue(e.isExpireable());
		assertFalse(e.expired());
		Thread.sleep(101);
		assertTrue(e.expired());
		assertNotNull(e.toDelayed());
		assertTrue(e.toDelayed().getDelay(TimeUnit.MILLISECONDS) < 0);
	}

	@Test
	public void test5() throws InterruptedException {
		long expireAt = System.currentTimeMillis() + 100;
		Expireable e1 = new Expireable() {
			@Override
			public long getExpirationTime() {
				return expireAt;
			}
		};
		assertEquals(expireAt, e1.getExpirationTime());
		assertTrue(e1.isExpireable());
		assertFalse(e1.expired());
		
		Expireable e2 = e1.addTime(200,TimeUnit.MILLISECONDS);
		
		Thread.sleep(101);
		assertTrue(e1.expired());
		assertNotNull(e1.toDelayed());
		assertTrue(e1.toDelayed().getDelay(TimeUnit.MILLISECONDS) < 0);

		assertEquals(expireAt+200, e2.getExpirationTime());
		assertFalse(e2.expired());
		assertNotNull(e2.toDelayed());
		assertFalse(e2.toDelayed().getDelay(TimeUnit.MILLISECONDS) < 0);

	}

	
	
}
