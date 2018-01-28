package com.aegisql.conveyor.config;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConveyorPropertyTest {

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
	public void testVoidProperty() {
		ConveyorProperty cp1 = ConveyorProperty.evalProperty(null,null);
		assertNotNull(cp1);
		assertFalse(cp1.isConveyorProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNull(cp1.getName());
		assertNull(cp1.getProperty());
		assertNull(cp1.getValue());
		
		ConveyorProperty cp2 = ConveyorProperty.evalProperty("","");
		assertNotNull(cp2);
		assertFalse(cp2.isConveyorProperty());
		assertFalse(cp2.isDefaultProperty());
		assertNull(cp2.getName());
		assertNull(cp2.getProperty());
		assertNull(cp2.getValue());

		ConveyorProperty cp3 = ConveyorProperty.evalProperty("some.other.property","val");
		assertNotNull(cp3);
		assertFalse(cp3.isConveyorProperty());
		assertFalse(cp3.isDefaultProperty());
		assertNull(cp3.getName());
		assertNull(cp3.getProperty());
		assertNull(cp3.getValue());

	}

	@Test
	public void testDefaultProperty() {
		ConveyorProperty cp1 = ConveyorProperty.evalProperty("conveyor.property",1);
		assertNotNull(cp1);
		assertTrue(cp1.isConveyorProperty());
		assertTrue(cp1.isDefaultProperty());
		assertNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
		
	}

	@Test
	public void testCommonProperty() {
		ConveyorProperty cp1 = ConveyorProperty.evalProperty("conveyor.name.property",1);
		assertNotNull(cp1);
		assertTrue(cp1.isConveyorProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testLongNameProperty() {
		ConveyorProperty cp1 = ConveyorProperty.evalProperty("conveyor.long.name.property",1);
		assertNotNull(cp1);
		assertTrue(cp1.isConveyorProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		assertEquals("long.name", cp1.getName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}
	
}
