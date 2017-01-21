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

// TODO: Auto-generated Javadoc
/**
 * The Class MapBuillderTest.
 */
public class MapBuillderTest {

	/**
	 * Sets the up before class.
	 *
	 * @throws Exception the exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * Tear down after class.
	 *
	 * @throws Exception the exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Sets the up.
	 *
	 * @throws Exception the exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Tear down.
	 *
	 * @throws Exception the exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test map builder.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testMapBuilder() throws InterruptedException {
		MapConveyor<Integer, String, String> mc = new MapConveyor<>();
		
		mc.setBuilderSupplier( ()-> new MapBuilder<>() );
		
		mc.setResultConsumer(bin->{
			System.out.println(bin);
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<Integer, String, String>(1, "FIRST", "ONE");
		ShoppingCart<Integer, String, String> c2 = new ShoppingCart<Integer, String, String>(1, "SECOND", "TWO");
		ShoppingCart<Integer, String, String> c3 = new ShoppingCart<Integer, String, String>(1, "THIRD", "THREE");
		ShoppingCart<Integer, String, String> c4 = new ShoppingCart<Integer, String, String>(1, null, null);

		mc.place(c1);
		mc.place(c2);
		mc.place(c3);
		mc.place(c4);
		
		Thread.sleep(100);
	}

}
