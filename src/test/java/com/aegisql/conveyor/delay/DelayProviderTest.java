package com.aegisql.conveyor.delay;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DelayProviderTest {

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

	}

}
