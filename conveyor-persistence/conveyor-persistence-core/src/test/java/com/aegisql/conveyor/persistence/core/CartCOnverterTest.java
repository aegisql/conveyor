package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
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
	public void test() {
		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(100, "value", "label");
		
		ConverterAdviser<String> ca = new ConverterAdviser<>();
		
		CartToBytesConverter<Integer, String, String> cc = new CartToBytesConverter<>(ca);
		
		byte[] bytes = cc.toPersistence(c1);
		assertNotNull(bytes);
		
		Cart<Integer,String,String> c2 = cc.fromPersistence(bytes);
		assertNotNull(c2);
		
	}

}
