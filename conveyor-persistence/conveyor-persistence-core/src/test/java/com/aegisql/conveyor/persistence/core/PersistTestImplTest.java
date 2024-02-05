package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.core.harness.PersistTestImpl;
import org.junit.*;

import static org.junit.Assert.*;

public class PersistTestImplTest {

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
	public void test() {

		PersistTestImpl p = new PersistTestImpl();
		assertEquals(1L, p.nextUniquePartId());
		
		p.savePart(p.nextUniquePartId(),new ShoppingCart<Integer, Integer, Integer>(1, 1, 1));
		p.savePartId(1, 1);
		p.savePart(p.nextUniquePartId(),new ShoppingCart<Integer, Integer, Integer>(1, 2, 2));
		p.savePartId(1, 2);
		p.savePart(p.nextUniquePartId(),new ShoppingCart<Integer, Integer, Integer>(2, 1, 1));
		p.savePartId(2, 3);
		
		assertEquals(2, p.getAllPartIds(1).size());
		assertEquals(1, p.getAllPartIds(2).size());
		assertEquals(0, p.getAllPartIds(3).size());
		
		assertNotNull(p.getPart(2));
		assertNotNull(p.getPart(3));
		assertNotNull(p.getPart(4));
		assertNull(p.getPart(5));

		p.saveCompletedBuildKey(1);
		
	}

}
