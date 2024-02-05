package com.aegisql.conveyor.config;

import com.aegisql.conveyor.config.harness.TestBean;
import org.junit.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class PersistencePropertyTest {

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
	public void testDefaultLevel1Property() {
		PersistenceProperty cp1 = PersistenceProperty.evalProperty("persistence.derby.property",1);
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertTrue(cp1.isDefaultProperty());
		assertNotNull(cp1.getType());
		assertNull(cp1.getSchema());
		assertNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
		
	}

	@Test
	public void testDefaultLevel0Property() {
		PersistenceProperty cp1 = PersistenceProperty.evalProperty("persistence.property",1);
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertTrue(cp1.isDefaultProperty());
		assertNull(cp1.getType());
		assertNull(cp1.getSchema());
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
		assertEquals("schema", cp1.getSchema());
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
		assertEquals("derby", cp1.getType());
		assertEquals("schema", cp1.getSchema());
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
		assertEquals("derby", cp1.getType());
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
		assertEquals("derby", cp1.getType());
		assertEquals("schema", cp1.getSchema());
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
		assertEquals("derby", cp1.getType());
		assertEquals("schema", cp1.getSchema());
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
		assertEquals("derby", cp1.getType());
		assertEquals("schema", cp1.getSchema());
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
		assertEquals("derby", cp1.getType());
		assertEquals("schema", cp1.getSchema());
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
		assertEquals("derby", cp1.getType());
		assertEquals("schema", cp1.getSchema());
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
		assertEquals("derby", cp1.getType());
		assertEquals("schema", cp1.getSchema());
		assertEquals("name", cp1.getName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testJavaPathResolution() {
		ConveyorConfiguration.registerBean(new TestBean(),"theBean");
		AtomicReference<PersistenceProperty> acp = new AtomicReference<>();
		PersistenceProperty.eval("persistence.derby.schema.name.archiveStrategy","javapath:theBean.type",cp->acp.set(cp));
		PersistenceProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertTrue(cp1.isPersistenceProperty());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getName());
		assertNotNull(cp1.getProperty());
		assertEquals("derby", cp1.getType());
		assertEquals("schema", cp1.getSchema());
		assertEquals("name", cp1.getName());
		assertEquals("archiveStrategy", cp1.getProperty());
		assertEquals("value", cp1.getValue());
		assertTrue(cp1.isJavaPath());
	}


}
