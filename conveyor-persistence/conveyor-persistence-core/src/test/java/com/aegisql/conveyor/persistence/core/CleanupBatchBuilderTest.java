package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.persistence.cleanup.CleaunupBatchBuilder;
import com.aegisql.conveyor.persistence.core.harness.PersistTestImpl;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CleanupBatchBuilderTest {

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
	public void testTimeoutReady() {
		Persistence<Integer> p = new PersistTestImpl();
		CleaunupBatchBuilder<Integer> cb = new CleaunupBatchBuilder<>(p);
		
		assertFalse(cb.test());
		cb.onTimeout();
		assertTrue(cb.test());
		
	}

	@Test
	public void testSizeReady1() {
		Persistence<Integer> p = new PersistTestImpl();
		CleaunupBatchBuilder<Integer> cb = new CleaunupBatchBuilder<>(p);
		
		assertFalse(cb.test());
		CleaunupBatchBuilder.addCartId(cb, 1L);
		CleaunupBatchBuilder.addCartId(cb, 2L);
		CleaunupBatchBuilder.addCartId(cb, 3L);
		assertTrue(cb.test());
	}

	@Test
	public void testSizeReady2() {
		Persistence<Integer> p = new PersistTestImpl();
		CleaunupBatchBuilder<Integer> cb = new CleaunupBatchBuilder<>(p);
		List<Long> l = new ArrayList<>();
		l.add(1L);
		l.add(2L);
		assertFalse(cb.test());
		CleaunupBatchBuilder.addCartIds(cb, l);
		CleaunupBatchBuilder.addKey(cb, 2);
		assertTrue(cb.test());
	}

}
