package com.aegisql.conveyor.delay;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// TODO: Auto-generated Javadoc
/**
 * The Class DelayProviderTest.
 */
public class DelayProviderTest {

	/**
	 * Sets the up before class.
	 *
	 */
	@BeforeAll
	public static void setUpBeforeClass() {
	}

	/**
	 * Tear down after class.
	 *
	 */
	@AfterAll
	public static void tearDownAfterClass() {
	}

	/**
	 * Sets the up.
	 *
	 */
	@BeforeEach
	public void setUp() {
	}

	/**
	 * Tear down.
	 *
	 */
	@AfterEach
	public void tearDown() {
	}

	/**
	 * Test.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void test() throws InterruptedException {
		DelayProvider<String> p = new DelayProvider<>();
		long now = System.currentTimeMillis();
		
		DelayBox<String> box = p.getBox(now+100);
		assertNotNull(box);
		p.getBox(now+100).add("A");
		p.getBox(now+100).add("B");
		p.getBox(now+100).add("C");
		
		assertEquals(1, p.delayedSize());
		p.getBox(now+200).add("D");
		p.getBox(now+200).add("E");
		p.getBox(now+200).add("F");
		assertEquals(2, p.delayedSize());
		
		List<String> e = p.getAllExpiredKeys();
		assertEquals(0, e.size());
		
		Thread.sleep(101);
		e = p.getAllExpiredKeys();
		assertEquals(3, e.size());
		System.out.println(e);
		Thread.sleep(101);
		e = p.getAllExpiredKeys();
		assertEquals(3, e.size());
		System.out.println(e);

		p.getBox(now+100).add("A");
		p.getBox(now+100).add("B");
		p.getBox(now+100).add("C");
		
		assertEquals(1, p.delayedSize());
		p.getBox(now+200).add("D");
		p.getBox(now+200).add("E");
		p.getBox(now+200).add("F");
		assertEquals(2, p.delayedSize());

		Thread.sleep(201);
		e = p.getAllExpiredKeys();
		assertEquals(6, e.size());
		System.out.println(e);

		p.getBox(now).add("A");
		p.getBox(now).add("B");
		p.getBox(now).add("C");
		assertEquals(1, p.delayedSize());
		e = p.getAllExpiredKeys();
		assertEquals(3, e.size());
		System.out.println(e);
		

		
	}

}
