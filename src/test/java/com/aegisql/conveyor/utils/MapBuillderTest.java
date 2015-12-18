package com.aegisql.conveyor.utils;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.utils.map.MapBuilder;
import com.aegisql.conveyor.utils.map.MapConveyor;

public class MapBuillderTest {

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
	public void testMapBuilder() throws InterruptedException {
		MapConveyor<Integer, String, String> mc = new MapConveyor<>();
		
		mc.setBuilderSupplier( ()-> new MapBuilder<>() );
		
		mc.setResultConsumer(bin->{
			System.out.println(bin.product);
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<Integer, String, String>(1, "FIRST", "ONE");
		ShoppingCart<Integer, String, String> c2 = new ShoppingCart<Integer, String, String>(1, "SECOND", "TWO");
		ShoppingCart<Integer, String, String> c3 = new ShoppingCart<Integer, String, String>(1, "THIRD", "THREE");
		ShoppingCart<Integer, String, String> c4 = new ShoppingCart<Integer, String, String>(1, null, null);

		mc.add(c1);
		mc.add(c2);
		mc.add(c3);
		mc.add(c4);
		
		Thread.sleep(100);
	}

}
