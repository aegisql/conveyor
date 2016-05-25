package com.aegisql.conveyor.cart;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.LabeledValue;

public class LabeledValueCartTest {

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
	public void testLabeledValue() {
		LabeledValue<String> lv = new LabeledValue<String>("LABEL", "VALUE");
		System.out.println(lv);
	}

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
