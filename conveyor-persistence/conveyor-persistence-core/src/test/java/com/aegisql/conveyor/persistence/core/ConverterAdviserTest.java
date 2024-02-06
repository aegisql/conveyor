package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.converters.EnumToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ObjectToJsonBytesConverter;
import com.aegisql.conveyor.persistence.core.harness.Trio;
import com.aegisql.conveyor.persistence.core.harness.TrioPart;
import org.junit.jupiter.api.*;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

public class ConverterAdviserTest implements Serializable {

	private static final long serialVersionUID = 1L;

	@BeforeAll
	public static void setUpBeforeClass() {
	}

	@AfterAll
	public static void tearDownAfterClass() {
	}

	@BeforeEach
	public void setUp() {
	}

	@AfterEach
	public void tearDown() {
	}

	@Test
	public void testAdv() {
		ConverterAdviser<String> ca = new ConverterAdviser<>();
		ObjectConverter<Object, byte[]> oc = ca.getConverter("test", Integer.class);
		ObjectConverter<Object, byte[]> oc2 = ca.getConverter("test", oc.conversionHint());
		byte[] res = oc.toPersistence(Integer.valueOf(1));
		Integer x = (Integer) oc.fromPersistence(res);
		assertEquals(Integer.valueOf(1), x);
		assertEquals(oc.conversionHint(),oc2.conversionHint());
	}

	@Test
	public void testLabel() {
		ConverterAdviser<String> ca = new ConverterAdviser<>();
		ca.addConverter("test", new ObjectConverter<Object, byte[]>() {
			@Override
			public byte[] toPersistence(Object obj) {
				byte[] orig = obj.toString().getBytes();
				byte[] rev = new byte[orig.length];
				for(int i = 0, l = orig.length-1; i< orig.length; i++,l--) {
					rev[l] = orig[i];
				}
				return rev;
			}

			@Override
			public Object fromPersistence(byte[] p) {
				return new String(p);
			}

			@Override
			public String conversionHint() {
				return "Object:byte[]";
			}
		});
		ObjectConverter<Object, byte[]> oc = ca.getConverter("test", String.class);
		byte[] res = oc.toPersistence("abcd");
		Object x = oc.fromPersistence(res);
		System.out.println(x);
		assertEquals("dcba", x);
	}

	
	@Test
	public void testSerial() {
		ConverterAdviser<String> ca = new ConverterAdviser<>();
		ObjectConverter<Object, byte[]> oc = ca.getConverter("test", this.getClass());
		byte[] res = oc.toPersistence(this);
		assertNotNull(res);
		System.out.println(new String(res));
		Object other = oc.fromPersistence(res);
		assertNotNull(other);
		System.out.println(other.getClass().getName());
	}

	@Test
	public void testEnum() {
		ConverterAdviser<String> ca = new ConverterAdviser<>();
		ObjectConverter<Object, byte[]> oc = ca.getConverter("test", TrioPart.class);
		assertTrue(oc instanceof EnumToBytesConverter);
		byte[] res = oc.toPersistence(TrioPart.NUMBER);
		assertNotNull(res);
		System.out.println(new String(res));
		Object other = oc.fromPersistence(res);
		assertNotNull(other);
		assertEquals(TrioPart.NUMBER, other);
		System.out.println(other);
	}	
	
	@Test
	public void testJson() {
		ConverterAdviser<String> ca = new ConverterAdviser<>();
		ca.addConverter(Trio.class, new ObjectToJsonBytesConverter<Trio>(Trio.class));
		ObjectConverter<Object, byte[]> oc = ca.getConverter("test", Trio.class);
		byte[] res = oc.toPersistence(new Trio("one","two",100));
		assertNotNull(res);
		System.out.println(new String(res));
		Object other = oc.fromPersistence(res);
		assertNotNull(other);
		System.out.println(other.getClass().getName());
	}

}
