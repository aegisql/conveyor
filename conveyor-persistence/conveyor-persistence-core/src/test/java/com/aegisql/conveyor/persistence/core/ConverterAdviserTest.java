package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.converters.ObjectToJsonBytesConverter;
import com.aegisql.conveyor.persistence.core.harness.Trio;

public class ConverterAdviserTest implements Serializable {

	private static final long serialVersionUID = 1L;

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
	public void testAdv() {
		ConverterAdviser<String> ca = new ConverterAdviser<>();
		ObjectConverter<Object, byte[]> oc = ca.getConverter("test", Integer.class);
		ObjectConverter<Object, byte[]> oc2 = ca.getConverter("test", oc.conversionHint());
		byte[] res = oc.toPersistence(new Integer(1));
		Integer x = (Integer) oc.fromPersistence(res);
		assertEquals(new Integer(1), x);
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
