package com.aegisql.conveyor.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import com.aegisql.conveyor.utils.caching.CachingConveyor;

public class CachingConveyorTest {

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
	public void testSimpleCache() throws InterruptedException {
		CachingConveyor<Integer, String, User> conveyor = new CachingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilder::new);
		conveyor.setDefaultCartConsumer((label, value, builder) -> {
			UserBuilder userBuilder = (UserBuilder) builder;
			switch (label) {
			case "setFirst":
				userBuilder.setFirst((String) value);
				break;
			case "setLast":
				userBuilder.setLast((String) value);
				break;
			case "setYearOfBirth":
				userBuilder.setYearOfBirth((Integer) value);
				break;
			default:
				throw new RuntimeException("Unknown label " + label);
			}
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = c1.nextCart("Doe", "setLast");
		Cart<Integer, Integer, String> c3 = c1.nextCart(1999, "setYearOfBirth");

		Supplier<? extends User> supplier = conveyor.getProductSupplier(1);
		assertNull(supplier);
		
		conveyor.add(c1);
		Thread.sleep(50);
		supplier = conveyor.getProductSupplier(1);
		assertNotNull(supplier);
		
		User u = supplier.get();
		assertNotNull(u);
		System.out.println(u);
		conveyor.add(c2);
		conveyor.add(c3);
		Thread.sleep(50);
		
		u = supplier.get();
		assertNotNull(u);
		System.out.println(u);
		assertEquals(1999,u.getYearOfBirth());
		Cart<Integer, Integer, String> c4 = c1.nextCart(2001, "setYearOfBirth");
		conveyor.add(c4);
		Thread.sleep(50);
		u = supplier.get();
		assertNotNull(u);
		System.out.println(u);
		assertEquals(2001,u.getYearOfBirth());
		
		
	}
	
	@Test(expected=IllegalStateException.class)
	public void testTimingOutCache() throws InterruptedException {
		CachingConveyor<Integer, String, User> conveyor = new CachingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilder::new);
		conveyor.setDefaultCartConsumer((label, value, builder) -> {
			UserBuilder userBuilder = (UserBuilder) builder;
			switch (label) {
			case "setFirst":
				userBuilder.setFirst((String) value);
				break;
			case "setLast":
				userBuilder.setLast((String) value);
				break;
			case "setYearOfBirth":
				userBuilder.setYearOfBirth((Integer) value);
				break;
			default:
				throw new RuntimeException("Unknown label " + label);
			}
		});
		conveyor.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		conveyor.setExpirationCollectionInterval(500, TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = c1.nextCart("Doe", "setLast");
		Cart<Integer, Integer, String> c3 = c1.nextCart(1999, "setYearOfBirth");

		Supplier<? extends User> supplier = conveyor.getProductSupplier(1);
		assertNull(supplier);
		
		conveyor.add(c1);
		Thread.sleep(50);
		supplier = conveyor.getProductSupplier(1);
		assertNotNull(supplier);
		
		User u = supplier.get();
		assertNotNull(u);
		System.out.println(u);
		conveyor.add(c2);
		conveyor.add(c3);
		Thread.sleep(50);
		
		u = supplier.get();
		assertNotNull(u);
		System.out.println(u);
		assertEquals(1999,u.getYearOfBirth());
		Cart<Integer, Integer, String> c4 = c1.nextCart(2001, "setYearOfBirth");
		conveyor.add(c4);
		Thread.sleep(50);
		u = supplier.get();
		assertNotNull(u);
		System.out.println(u);
		assertEquals(2001,u.getYearOfBirth());
		Thread.sleep(500);
		
		u = supplier.get();
		
	}

	
}
