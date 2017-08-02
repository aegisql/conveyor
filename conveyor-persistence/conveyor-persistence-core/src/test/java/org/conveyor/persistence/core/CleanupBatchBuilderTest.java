package org.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.conveyor.persistence.cleanup.CleaunupBatchBuilder;
import org.conveyor.persistence.core.harness.PersistTestImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CleanupBatchBuilderTest {

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
	public void testTimeoutReady() {
		Persist<Integer> p = new PersistTestImpl();
		CleaunupBatchBuilder<Integer> cb = new CleaunupBatchBuilder<>(p, 3);
		
		assertFalse(cb.test());
		cb.onTimeout();
		assertTrue(cb.test());
		
	}

	@Test
	public void testSizeReady1() {
		Persist<Integer> p = new PersistTestImpl();
		CleaunupBatchBuilder<Integer> cb = new CleaunupBatchBuilder<>(p, 3);
		
		assertFalse(cb.test());
		cb.addCartId(cb, 1L);
		cb.addCartId(cb, 2L);
		cb.addCartId(cb, 3L);
		assertTrue(cb.test());
	}

	@Test
	public void testSizeReady2() {
		Persist<Integer> p = new PersistTestImpl();
		CleaunupBatchBuilder<Integer> cb = new CleaunupBatchBuilder<>(p, 3);
		List<Long> l = new ArrayList<>();
		l.add(1L);
		l.add(2L);
		assertFalse(cb.test());
		cb.addCartIds(cb, l);
		cb.addKey(cb, 2);
		assertTrue(cb.test());
	}

}
