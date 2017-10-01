package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.converters.EnumToBytesConverter;

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

}
