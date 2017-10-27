package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.converters.EnumToBytesConverter;
import com.aegisql.conveyor.persistence.utils.CartInputStream;

public class CartCOnverterTest {

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
	public void enumConverterTest() {
		EnumToBytesConverter<TimeUnit> uc = new EnumToBytesConverter<>(TimeUnit.class);
		
		byte[] bytes = uc.toPersistence(TimeUnit.MINUTES);
		assertNotNull(bytes);
		
		TimeUnit tu = uc.fromPersistence(bytes);
		assertNotNull(tu);
		assertEquals(tu, TimeUnit.MINUTES);
		
	}

	@Test
	public void testSimpleCartWithProperty() {
		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(100, "value", "label");
		c1.addProperty("testProp", "propVal");
		ConverterAdviser<String> ca = new ConverterAdviser<>();
		
		CartToBytesConverter<Integer, String, String> cc = new CartToBytesConverter<>(ca);
		
		byte[] bytes = cc.toPersistence(c1);
		assertNotNull(bytes);
		System.out.println("length: "+bytes.length);
		
		Cart<Integer,String,String> c2 = cc.fromPersistence(bytes);
		assertNotNull(c2);
		System.out.println("cart: "+c1);
		System.out.println("cart: "+c2);
		assertEquals(c1.getKey(), c2.getKey());
		assertEquals(c1.getValue(), c2.getValue());
		assertEquals(c1.getLabel(), c2.getLabel());
		assertEquals(c1.getCreationTime(), c2.getCreationTime());
		assertEquals(c1.getExpirationTime(), c2.getExpirationTime());
		assertEquals(LoadType.PART, c2.getLoadType());
		assertEquals("propVal", c2.getProperty("testProp", String.class));
	}

	@Test
	public void testMultyCartWithProperty() {
		Cart c1 = new MultiKeyCart<Integer,String,String>(k->true, "value", "label", System.currentTimeMillis()-1000, System.currentTimeMillis()+1000);
		c1.addProperty("testProp", "propVal");
		ConverterAdviser<String> ca = new ConverterAdviser<>();
		
		CartToBytesConverter<Integer, ?, String> cc = new CartToBytesConverter<>(ca);
		
		byte[] bytes = cc.toPersistence(c1);
		assertNotNull(bytes);
		System.out.println("length: "+bytes.length);
		
		Cart c2 = cc.fromPersistence(bytes);
		assertNotNull(c2);
		System.out.println("cart: "+c1);
		System.out.println("cart: "+c2);
		assertEquals(c1.getKey(), c2.getKey());
		assertEquals(c1.getValue(), c2.getValue());
		assertEquals(c1.getLabel(), c2.getLabel());
		assertEquals(c1.getCreationTime(), c2.getCreationTime());
		assertEquals(c1.getExpirationTime(), c2.getExpirationTime());
		assertEquals(LoadType.MULTI_KEY_PART, c2.getLoadType());
		assertEquals("propVal", c2.getProperty("testProp", String.class));
	}

	@Test
	public void testStaticCartWithProperty() {
		
		long time = System.currentTimeMillis();
		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(null, "value", "label", time-1000, time+1000,null,LoadType.STATIC_PART);
		c1.addProperty("testProp", "propVal");
		ConverterAdviser<String> ca = new ConverterAdviser<>();
		
		CartToBytesConverter<Integer, String, String> cc = new CartToBytesConverter<>(ca);
		
		byte[] bytes = cc.toPersistence(c1);
		assertNotNull(bytes);
		System.out.println("length: "+bytes.length);
		
		Cart<Integer,String,String> c2 = cc.fromPersistence(bytes);
		assertNotNull(c2);
		System.out.println("cart: "+c1);
		System.out.println("cart: "+c2);
		assertEquals(c1.getKey(), c2.getKey());
		assertEquals(c1.getValue(), c2.getValue());
		assertEquals(c1.getLabel(), c2.getLabel());
		assertEquals(c1.getCreationTime(), c2.getCreationTime());
		assertEquals(0, c2.getExpirationTime());
		assertEquals(LoadType.STATIC_PART, c2.getLoadType());
		assertEquals("propVal", c2.getProperty("testProp", String.class));
	}

	@Test
	public void testCartInputStream() throws IOException {
		
		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(100, "value", "label");
		c1.addProperty("testProp", "propVal");
		ConverterAdviser<String> ca = new ConverterAdviser<>();
		
		CartToBytesConverter<Integer, String, String> cc = new CartToBytesConverter<>(ca);
		
		byte[] bytes = cc.toPersistence(c1);
		assertNotNull(bytes);
		System.out.println("length: "+bytes.length);

		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		
		CartInputStream<Integer, String> cis = new CartInputStream<>(cc, is);
		
		Cart<Integer,?,String> c2 = cis.getCart();
		assertNotNull(c2);
		System.out.println(c2);
	}

	@Test
	public void testManyCartInputStream() throws IOException {
		
		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(100, "value", "label");
		c1.addProperty("testProp", "propVal");
		ConverterAdviser<String> ca = new ConverterAdviser<>();
		
		CartToBytesConverter<Integer, String, String> cc = new CartToBytesConverter<>(ca);
		
		byte[] bytes1 = cc.toPersistence(c1);
		assertNotNull(bytes1);

		Cart<Integer,String,String> c2 = new ShoppingCart<Integer, String, String>(101, "valueMore", "otherLabel");
		c1.addProperty("testPropToo", "propValOther");
				
		byte[] bytes2 = cc.toPersistence(c2);
		assertNotNull(bytes2);

		byte[] bytes = new byte[bytes1.length+bytes2.length];
		
		int pos = 0;
		for(int i = 0; i<bytes1.length;i++) {
			bytes[pos++] = bytes1[i];
		}
		for(int i = 0; i<bytes2.length;i++) {
			bytes[pos++] = bytes2[i];
		}
		
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		
		CartInputStream<Integer, String> cis = new CartInputStream<>(cc, is);
		
		Cart<Integer,?,String> c3 = cis.getCart();
		assertNotNull(c3);
		System.out.println(c3);
		Cart<Integer,?,String> c4 = cis.getCart();
		assertNotNull(c4);
		System.out.println(c4);
	}

	
}
