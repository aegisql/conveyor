package com.aegisql.conveyor.config;

import com.aegisql.conveyor.config.harness.TestBean;
import org.junit.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.aegisql.conveyor.config.ConveyorProperty.ConveyorPropertyType.CONVEYOR;
import static com.aegisql.conveyor.config.ConveyorProperty.ConveyorPropertyType.OTHER;
import static org.junit.Assert.*;

public class ConveyorPropertyTest {

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
		ConveyorProperty cp1 = ConveyorProperty.evalProperty(null,null);
		assertNotNull(cp1);
		assertEquals(OTHER,cp1.getConveyorPropertyType());
		assertFalse(cp1.isDefaultProperty());
		assertNull(cp1.getConveyorName());
		assertNull(cp1.getProperty());
		assertNull(cp1.getValue());
		
		ConveyorProperty cp2 = ConveyorProperty.evalProperty("","");
		assertNotNull(cp2);
		assertEquals(OTHER,cp2.getConveyorPropertyType());
		assertFalse(cp2.isDefaultProperty());
		assertNull(cp2.getConveyorName());
		assertNull(cp2.getProperty());
		assertNull(cp2.getValue());

		ConveyorProperty cp3 = ConveyorProperty.evalProperty("some.other.property","val");
		assertNotNull(cp3);
		assertEquals(OTHER,cp3.getConveyorPropertyType());
		assertFalse(cp3.isDefaultProperty());
		assertNull(cp3.getConveyorName());
		assertNull(cp3.getProperty());
		assertNull(cp3.getValue());
	}

	@Test
	public void testDefaultProperty() {
		ConveyorProperty cp1 = ConveyorProperty.evalProperty("conveyor.property",1);
		assertNotNull(cp1);
		assertEquals(CONVEYOR,cp1.getConveyorPropertyType());
		assertTrue(cp1.isDefaultProperty());
		assertNull(cp1.getConveyorName());
		assertNotNull(cp1.getProperty());
		
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
		
	}

	@Test
	public void testCommonProperty() {
		ConveyorProperty cp1 = ConveyorProperty.evalProperty("conveyor.name.property",1);
		assertNotNull(cp1);
		assertEquals(CONVEYOR,cp1.getConveyorPropertyType());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getConveyorName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getConveyorName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testLongNameProperty() {
		ConveyorProperty cp1 = ConveyorProperty.evalProperty("conveyor.long.name.property",1);
		assertNotNull(cp1);
		assertEquals(CONVEYOR,cp1.getConveyorPropertyType());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getConveyorName());
		assertNotNull(cp1.getProperty());
		assertEquals("long.name", cp1.getConveyorName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testCommonPropertyEval() {
		AtomicReference<ConveyorProperty> acp = new AtomicReference<>();
		ConveyorProperty.eval("conveyor.name.property",1,cp->acp.set(cp));
		ConveyorProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertEquals(CONVEYOR,cp1.getConveyorPropertyType());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getConveyorName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getConveyorName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testMapProperty1Eval() {
		AtomicReference<ConveyorProperty> acp = new AtomicReference<>();
		Map<String,Object> m = new HashMap<>();
		m.put("property", 1);
		ConveyorProperty.eval("conveyor.name",m,cp->acp.set(cp));
		ConveyorProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertEquals(CONVEYOR,cp1.getConveyorPropertyType());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getConveyorName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getConveyorName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testListProperty1Eval() {
		AtomicReference<ConveyorProperty> acp = new AtomicReference<>();
		List<Integer> l = new ArrayList<>();
		l.add(1);
		Map<String,Object> m = new HashMap<>();
		m.put("property", l);
		ConveyorProperty.eval("conveyor.name",m,cp->acp.set(cp));
		ConveyorProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertEquals(CONVEYOR,cp1.getConveyorPropertyType());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getConveyorName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getConveyorName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testMapProperty2Eval() {
		AtomicReference<ConveyorProperty> acp = new AtomicReference<>();
		Map<String,Object> m1 = new HashMap<>();
		m1.put("property", 1);
		Map<String,Object> m2 = new HashMap<>();
		m2.put("name", m1);
		ConveyorProperty.eval("conveyor",m2,cp->acp.set(cp));
		ConveyorProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertEquals(CONVEYOR,cp1.getConveyorPropertyType());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getConveyorName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getConveyorName());
		assertEquals("property", cp1.getProperty());
		assertEquals(1, cp1.getValue());
	}

	@Test
	public void testSimpleJavaPath() throws Exception {
		ConveyorConfiguration.build("jp:com.aegisql.conveyor.config.harness.TestBean testBean");
		AtomicReference<ConveyorProperty> acp = new AtomicReference<>();
		ConveyorProperty.eval("conveyor.name.timeout","javapath:testBean.timeout",cp->acp.set(cp));
		ConveyorProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertEquals(CONVEYOR,cp1.getConveyorPropertyType());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getConveyorName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getConveyorName());
		assertEquals("timeout", cp1.getProperty());
		assertEquals(1000, cp1.getValue());
	}

	@Test
	public void testAutoRegistrationJavaPath() throws Exception {
		AtomicReference<ConveyorProperty> acp = new AtomicReference<>();
		ConveyorProperty.eval("conveyor.name.timeout","javapath:(com.aegisql.conveyor.config.harness.TestBean testBean).timeout",cp->acp.set(cp));
		ConveyorProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertEquals(CONVEYOR,cp1.getConveyorPropertyType());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getConveyorName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getConveyorName());
		assertEquals("timeout", cp1.getProperty());
		assertEquals(1000, cp1.getValue());
	}

	@Test
	public void testJavaPathWithBean() throws Exception {
		ConveyorConfiguration.registerBean(new TestBean(),"theBean");
		AtomicReference<ConveyorProperty> acp = new AtomicReference<>();
		ConveyorProperty.eval("conveyor.name.timeout","javapath:theBean.timeout",cp->acp.set(cp));
		ConveyorProperty cp1 = acp.get();
		assertNotNull(cp1);
		assertEquals(CONVEYOR,cp1.getConveyorPropertyType());
		assertFalse(cp1.isDefaultProperty());
		assertNotNull(cp1.getConveyorName());
		assertNotNull(cp1.getProperty());
		assertEquals("name", cp1.getConveyorName());
		assertEquals("timeout", cp1.getProperty());
		assertEquals(1000, cp1.getValue());
		assertTrue(cp1.isJavaPath());
	}

	@Test
	public void durationParsingTest() {
		ConveyorConfiguration.registerPath("java.time.Duration");
		AtomicReference<ConveyorProperty> acp = new AtomicReference<>();
		Duration.ofMillis(1000).plusMillis(0);
		ConveyorProperty.eval("conveyor.name.timeout","javapath:(Duration millisec(1000))",cp->acp.set(cp));
		ConveyorProperty cp1 = acp.get();
		assertNotNull(cp1);
	}
}
