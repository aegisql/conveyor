package com.aegisql.conveyor.cart;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.LabeledValue;

// TODO: Auto-generated Javadoc
/**
 * The Class LabeledValueCartTest.
 */
public class LabeledValueCartTest {

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
	 * Test labeled value.
	 */
	@Test
	public void testLabeledValue() {
		LabeledValue<String> lv = new LabeledValue<String>("LABEL", "VALUE");
		System.out.println(lv);
	}

	/**
	 * Test labeled value cart.
	 */
	@Test
	public void testLabeledValueCart() {
		LabeledValueCart<String,String,String> c = new LabeledValueCart<>("K","V","L");
		assertNull(c.getLabel());
		assertNotNull(c.getKey());
		assertNotNull(c.getValue());
		assertEquals("K", c.getKey());
		assertEquals("V", c.getValue().value);
		assertEquals("L", c.getValue().label);
	}
}
