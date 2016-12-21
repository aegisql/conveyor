package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.multichannel.UserBuilder;
import com.aegisql.conveyor.user.User;

public class BuilderSupplierFunctionalTest {

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
	public void testExpireableTimestamp() {
		BuilderSupplier<User> bs = UserBuilder::new;
		assertFalse(bs instanceof Expireable);
		assertEquals(bs,bs.identity());
		bs = bs.expire(1000);
		assertNotNull(bs);
		assertTrue(bs instanceof BuilderSupplier);
		Supplier<? extends User> s = bs.get();
		assertNotNull(s);
		assertTrue(s instanceof Expireable);
		User u = bs.get().get();
		assertNotNull(u);
		Expireable ex = (Expireable)s;
		assertEquals(1000, ex.getExpirationTime());
	}

	@Test
	public void testExpireableOther() {
		BuilderSupplier<User> bs = UserBuilder::new;
		assertFalse(bs instanceof Expireable);
		bs = bs.expire(()->1000);
		assertNotNull(bs);
		assertTrue(bs instanceof BuilderSupplier);
		Supplier<? extends User> s = bs.get();
		assertNotNull(s);
		assertTrue(s instanceof Expireable);
		User u = bs.get().get();
		assertNotNull(u);
		Expireable ex = (Expireable)s;
		assertEquals(1000,ex.getExpirationTime());
	}

	
	@Test
	public void testExpireableTimeUnit() {
		BuilderSupplier<User> bs = UserBuilder::new;
		assertFalse(bs instanceof Expireable);
		bs = bs.expire(1000,TimeUnit.MILLISECONDS);
		assertNotNull(bs);
		assertTrue(bs instanceof BuilderSupplier);
		Supplier<? extends User> s = bs.get();
		assertNotNull(s);
		assertTrue(s instanceof Expireable);
		User u = bs.get().get();
		assertNotNull(u);
		Expireable ex = (Expireable)s;
		assertTrue(ex.getExpirationTime()-System.currentTimeMillis() > 500);
	}

	@Test
	public void testExpireableDuration() {
		BuilderSupplier<User> bs = UserBuilder::new;
		assertFalse(bs instanceof Expireable);
		bs = bs.expire(Duration.ofSeconds(1));
		assertNotNull(bs);
		assertTrue(bs instanceof BuilderSupplier);
		Supplier<? extends User> s = bs.get();
		assertNotNull(s);
		assertTrue(s instanceof Expireable);
		User u = bs.get().get();
		assertNotNull(u);
		Expireable ex = (Expireable)s;
		assertTrue(ex.getExpirationTime()-System.currentTimeMillis() > 500);
	}

	@Test
	public void testExpireableInstant() {
		BuilderSupplier<User> bs = UserBuilder::new;
		assertFalse(bs instanceof Expireable);
		bs = bs.expire(Instant.now().plusSeconds(1));
		assertNotNull(bs);
		assertTrue(bs instanceof BuilderSupplier);
		Supplier<? extends User> s = bs.get();
		assertNotNull(s);
		assertTrue(s instanceof Expireable);
		User u = bs.get().get();
		assertNotNull(u);
		Expireable ex = (Expireable)s;
		assertTrue(ex.getExpirationTime()-System.currentTimeMillis() > 500);
	}

	@Test
	public void testDoubleExpireable() {
		BuilderSupplier<User> bs = UserBuilder::new;
		bs = bs.expire(1000).expire(2000);
		assertNotNull(bs);
		assertTrue(bs instanceof BuilderSupplier);
		Supplier<? extends User> s = bs.get();
		assertNotNull(s);
		assertTrue(s instanceof Expireable);
		User u = bs.get().get();
		assertNotNull(u);
		Expireable ex = (Expireable)s;
		assertEquals(2000, ex.getExpirationTime());
	}
	
	@Test(expected=CancellationException.class)
	public void testConvWithExpire() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> c = new AssemblingConveyor<>();
		c.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		c.setScrapConsumer(bin->{
			System.out.println(bin);
		});
		c.setResultConsumer(res->{
			System.out.println(res);			
		});
		c.setDefaultCartConsumer((l,v,b)->{
			System.out.println(l+" "+v+" "+b.get());						
		});
		c.setReadinessEvaluator((b)->false);
		
		BuilderSupplier<User> bs = ((BuilderSupplier<User>)UserBuilder::new).expire(200, TimeUnit.MILLISECONDS);
		System.out.println("BS="+bs.get());						
		
		CompletableFuture<User> f = c.createBuildFuture(1,bs);
		c.add(1, "VALUE", "LABEL");
		f.get();
		
	}

	@Test
	public void testTestPredicate() {
		BuilderSupplier<User> bs = UserBuilder::new;
		assertFalse(bs instanceof Testing);
		bs = bs.readyAlgorithm(b->{
			return true;
		});
		assertNotNull(bs);
		assertTrue(bs instanceof BuilderSupplier);
		Supplier<? extends User> s = bs.get();
		assertNotNull(s);
		assertTrue(s instanceof Testing);
		User u = bs.get().get();
		assertNotNull(u);
		Testing t = (Testing)s;
		assertTrue(t.test());
	}

	@Test
	public void testTestExpireablePredicate() {
		BuilderSupplier<User> bs = UserBuilder::new;
		assertFalse(bs instanceof Testing);
		bs = bs.readyAlgorithm(b->{
			return true;
		}).expire(1000);
		assertNotNull(bs);
		assertTrue(bs instanceof BuilderSupplier);
		Supplier<? extends User> s = bs.get();
		assertNotNull(s);
		assertTrue(s instanceof Expireable);
		assertTrue(s instanceof Testing);
		User u = bs.get().get();
		assertNotNull(u);
		Testing t = (Testing)s;
		assertTrue(t.test());
	}

	
	@Test
	public void test() {
		BuilderSupplier<User> bs = BuilderSupplier
				.of(UserBuilder::new)
				.expire(1000)
				//.test((b)->true)
				.withFuture(new CompletableFuture<User>());
		assertNotNull(bs);
		Expireable ex = (Expireable)bs.get();
		assertEquals(1000, ex.getExpirationTime());
		FutureSupplier<User> fs = (FutureSupplier<User>)bs;
		assertNotNull(fs.getFuture());
		bs.get();
		User u = bs.get().get();
		bs.get().get();
	}
}
