package com.aegisql.conveyor.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import com.aegisql.conveyor.utils.caching.CachingConveyor;
import com.aegisql.conveyor.utils.caching.ImmutableReference;
import com.aegisql.conveyor.utils.caching.ImmutableValueConsumer;
import com.aegisql.conveyor.utils.caching.MutableReference;
import com.aegisql.conveyor.utils.caching.MutableValueConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Class CachingConveyorTest.
 */
public class CachingConveyorTest {

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
	 * Test big cache.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testBigCache() throws InterruptedException {
		
		int BIG = 1000000;
		
		CachingConveyor<Integer, String, User> conveyor = new CachingConveyor<>();
		conveyor.setSynchronizeBuilder(true);
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

		long exp = System.currentTimeMillis() + BIG;
		for(int i = 1; i<=BIG;i++) {
			ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(i, "TestFirst"+i, "setFirst", exp);
			Cart<Integer, String, String> c2 = c1.nextCart("TestLast"+i, "setLast");
			Cart<Integer, Integer, String> c3 = c1.nextCart(1900+i%100, "setYearOfBirth");
			conveyor.add(c1);
			conveyor.add(c2);
			conveyor.add(c3);
		}
		while( conveyor.getCollectorSize() < BIG) {
			Thread.sleep(100);
		}
		System.out.println("DelayQueue after adding 1 mln: "+conveyor.getDelayedQueueSize());
		
		long ac1 = 0;
		Random r = new Random();
		for(int i = 1; i<=BIG;i++) {
			long t1 = System.nanoTime();
			
			conveyor.getProductSupplier(r.nextInt(BIG)+1).get();
			long t2 = System.nanoTime();
			ac1 += t2-t1;
		}
		double av1 = ac1/BIG;
		System.out.println("Supplier access time: "+av1);

		long ac2 = 0;
		
		UserBuilder[] uba = new UserBuilder[BIG];
		for(int i = 0; i<BIG; i++) {
			UserBuilder b = new UserBuilder();
		
			b.setFirst("TestFirst"+i);
			b.setLast("TestLast"+i);
			b.setYearOfBirth(1900+i%100);
			uba[i] = b;
		}
		for(int i = 1; i<=BIG;i++) {
			long t1 = System.nanoTime();
			uba[r.nextInt(BIG)].get();
			long t2 = System.nanoTime();
			ac2 += t2-t1;
		}
		double av2 = ac2/BIG;
		System.out.println("Builder array access time: "+av2);

		Map<Integer,UserBuilder> m = new HashMap<>();
		for(int i = 0; i < BIG; i++) {
			m.put(i+1, uba[i]);
		}
		long ac3 = 0;
		for(int i = 1; i<=BIG;i++) {
			long t1 = System.nanoTime();
			m.get(r.nextInt(BIG)+1).get();
			long t2 = System.nanoTime();
			ac3 += t2-t1;
		}
		double av3 = ac3/BIG;
		System.out.println("Builder map access time: "+av3);
		
	}

	
	/**
	 * Test simple cache.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
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
	
	/**
	 * Test timing out cache.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
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
		conveyor.setIdleHeartBeat(500, TimeUnit.MILLISECONDS);
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

	/**
	 * Test timing out cache with ttl extension.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test // NOT EXPECTED (expected=IllegalStateException.class)
	public void testTimingOutCacheWithTTLExtension() throws InterruptedException {
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
		conveyor.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		conveyor.enablePostponeExpiration(true);
		conveyor.setExpirationPostponeTime(1, TimeUnit.SECONDS);
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
		
		u = supplier.get(); //should be still there
		
	}


	/**
	 * Test simple cache.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test(expected=IllegalStateException.class)
	public void testImmutableScalarCache() throws InterruptedException, ExecutionException {
		CachingConveyor<Integer, String, String> conveyor = new CachingConveyor<>();
		conveyor.setDefaultCartConsumer(new ImmutableValueConsumer());
		
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		CompletableFuture<Boolean> cf = conveyor.createBuild(1,ImmutableReference.newInstance("TEST"));
		assertTrue(cf.get());
		
		Supplier<? extends String> s = conveyor.getProductSupplier(1);
		assertEquals("TEST", s.get());
		conveyor.createBuild(1,ImmutableReference.newInstance("TEST"));
		
		Thread.sleep(1200);
		s.get();
	}

	@Test(expected=IllegalStateException.class)
	public void testImmutableScalarExpireableCache() throws InterruptedException, ExecutionException {
		CachingConveyor<Integer, String, String> conveyor = new CachingConveyor<>();
		conveyor.setDefaultCartConsumer(new ImmutableValueConsumer());
		
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		CompletableFuture<Boolean> cf = conveyor.createBuild(1,ImmutableReference.newInstance("TEST").expire(1,TimeUnit.SECONDS));
		assertTrue(cf.get());
		
		Supplier<? extends String> s = conveyor.getProductSupplier(1);
		assertEquals("TEST", s.get());
		conveyor.createBuild(1,ImmutableReference.newInstance("TEST"));
		
		Thread.sleep(1200);
		s.get();
	}

	
	@Test(expected=IllegalStateException.class)
	public void testMutableScalarCache() throws InterruptedException, ExecutionException {
		CachingConveyor<Integer, String, String> conveyor = new CachingConveyor<>();
		conveyor.setDefaultCartConsumer(new MutableValueConsumer());
		
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		CompletableFuture<Boolean> cf = conveyor.createBuild(1,MutableReference.newInstance("TEST"));
		CompletableFuture<Boolean> cf2 = conveyor.createBuild(1,MutableReference.newInstance("BEST"));
		assertTrue(cf.get());
		assertTrue(cf2.get());
		Supplier<? extends String> s = conveyor.getProductSupplier(1);
		assertEquals("BEST", s.get());

		CompletableFuture<Boolean> cf3 = conveyor.add(1,"GUEST","update");
		assertTrue(cf3.get());
		s = conveyor.getProductSupplier(1);
		assertEquals("GUEST", s.get());

		
		Thread.sleep(1200);
		s.get();
		
	}

	@Test(expected=IllegalStateException.class)
	public void testMutableExpireableScalarCache() throws InterruptedException, ExecutionException {
		CachingConveyor<Integer, String, String> conveyor = new CachingConveyor<>();
		conveyor.setDefaultCartConsumer(new MutableValueConsumer());
		//conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		CompletableFuture<Boolean> cf  = conveyor.createBuild(1,MutableReference.newInstance("TEST").expire(1,TimeUnit.SECONDS));
		CompletableFuture<Boolean> cf2 = conveyor.createBuild(1,MutableReference.newInstance("BEST"));
		assertTrue(cf.get());
		assertTrue(cf2.get());
		Supplier<? extends String> s = conveyor.getProductSupplier(1);
		assertEquals("BEST", s.get());

		CompletableFuture<Boolean> cf3 = conveyor.add(1,"GUEST","update");
		assertTrue(cf3.get());
		s = conveyor.getProductSupplier(1);
		assertEquals("GUEST", s.get());

		
		Thread.sleep(1200);
		s.get();
		
	}

	
}
