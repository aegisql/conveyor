package com.aegisql.conveyor.config;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PersistencePropertyTest {

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
		PersistenceProperty cp1 = PersistenceProperty.evalProperty(null,null);
		assertNotNull(cp1);
		assertFalse(cp1.isPersistenceProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNull(cp1.getName());
		assertNull(cp1.getProperty());
		assertNull(cp1.getValue());
		
		PersistenceProperty cp2 = PersistenceProperty.evalProperty("","");
		assertNotNull(cp2);
		assertFalse(cp2.isPersistenceProperty());
		assertFalse(cp2.isDefaultProperty());
		assertNull(cp2.getName());
		assertNull(cp2.getProperty());
		assertNull(cp2.getValue());

		PersistenceProperty cp3 = PersistenceProperty.evalProperty("some.other.property","val");
		assertNotNull(cp3);
		assertFalse(cp3.isPersistenceProperty());
		assertFalse(cp3.isDefaultProperty());
		assertNull(cp3.getName());
		assertNull(cp3.getProperty());
		assertNull(cp3.getValue());

	}

	@Test
	public void testDefaultProperty() {
		PersistenceProperty cp1 = PersistenceProperty.evalProperty("persistence.derby.property",1);
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertTrue(cp1.isDefaultProperty());
		assertNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
		
	}

	@Test
	public void testCommonProperty() {
		PersistenceProperty cp1 = PersistenceProperty.evalProperty("persistence.derby.schema.name.property",1);
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		assertEquals("derby", cp1.getType());
		assertEquals("name", cp1.getName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testLongNameProperty() {
		PersistenceProperty cp1 = PersistenceProperty.evalProperty("persistence.derby.schema.long.name.property",1);
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		assertEquals("long.name", cp1.getName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testCommonPropertyEval() {
		AtomicReference<PersistenceProperty> acp = new AtomicReference<>();
		PersistenceProperty.eval("persistence.derby.schema.name.property",1,cp->acp.set(cp));
		PersistenceProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		assertEquals("schema", cp1.getSchema());
		assertEquals("name", cp1.getName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testArchiveStrategy() {
		AtomicReference<PersistenceProperty> acp = new AtomicReference<>();
		PersistenceProperty.eval("persistence.derby.schema.name.archiveStrategy.path","/test",cp->acp.set(cp));
		PersistenceProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getName());
		assertEquals("archiveStrategy.path", cp1.getProperty());
		assertEquals("/test", cp1.getValue());
	}

	@Test
	public void testArchiveLongNameStrategy() {
		AtomicReference<PersistenceProperty> acp = new AtomicReference<>();
		PersistenceProperty.eval("persistence.derby.schema.long.name.archiveStrategy.path","/test",cp->acp.set(cp));
		PersistenceProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		assertEquals("long.name", cp1.getName());
		assertEquals("archiveStrategy.path", cp1.getProperty());
		assertEquals("/test", cp1.getValue());
	}

	@Test
	public void testArchiveStrategyShort() {
		AtomicReference<PersistenceProperty> acp = new AtomicReference<>();
		PersistenceProperty.eval("persistence.derby.schema.name.archiveStrategy","ARCHIVE",cp->acp.set(cp));
		PersistenceProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getName());
		assertEquals("archiveStrategy", cp1.getProperty());
		assertEquals("ARCHIVE", cp1.getValue());
	}


	@Test
	public void testMapProperty1Eval() {
		AtomicReference<PersistenceProperty> acp = new AtomicReference<>();
		Map<String,Object> m = new HashMap<>();
		m.put("property", 1);
		PersistenceProperty.eval("persistence.derby.schema.name",m,cp->acp.set(cp));
		PersistenceProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testListProperty1Eval() {
		AtomicReference<PersistenceProperty> acp = new AtomicReference<>();
		List<Integer> l = new ArrayList<>();
		l.add(1);
		Map<String,Object> m = new HashMap<>();
		m.put("property", l);
		PersistenceProperty.eval("persistence.derby.schema.name",m,cp->acp.set(cp));
		PersistenceProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testMapProperty2Eval() {
		AtomicReference<PersistenceProperty> acp = new AtomicReference<>();
		Map<String,Object> m1 = new HashMap<>();
		m1.put("property", 1);
		Map<String,Object> m2 = new HashMap<>();
		m2.put("name", m1);
		PersistenceProperty.eval("persistence.derby.schema",m2,cp->acp.set(cp));
		PersistenceProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}
	
}
