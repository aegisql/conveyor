package com.aegisql.conveyor;

import org.junit.*;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ExpireableTest {

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
		Expireable e3 = e1.addTime(Duration.ofMillis(200));
		assertEquals(e2.getExpirationTime(),e3.getExpirationTime());
		Thread.sleep(101);
		assertTrue(e1.expired());
		assertNotNull(e1.toDelayed());
		assertTrue(e1.toDelayed().getDelay(TimeUnit.MILLISECONDS) < 0);

		assertEquals(expireAt+200, e2.getExpirationTime());
		assertFalse(e2.expired());
		assertNotNull(e2.toDelayed());
		assertFalse(e2.toDelayed().getDelay(TimeUnit.MILLISECONDS) < 0);

		Delayed d2 = e2.toDelayed();
		Delayed d3 = e3.toDelayed();

		Delayed d4 = new Delayed() {
			@Override
			public long getDelay(TimeUnit unit) {
				return d2.getDelay(TimeUnit.MILLISECONDS);
			}
			@Override
			public int compareTo(Delayed o) {
				return 0;
			}
		};

		assertEquals(0,d2.compareTo(d2));
		assertEquals(0,d2.compareTo(d3));
		assertEquals(0,d2.compareTo(d4));



	}

	
	
}
