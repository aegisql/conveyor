package com.aegisql.conveyor.loaders;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;

public class FutureLoaderTest {

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
	public void test() {
		long current = System.currentTimeMillis();

		FutureLoader cl0 = new FutureLoader<>(c->{
			System.out.println("Final: "+c);
			return new CompletableFuture();
		});
		
		System.out.println(cl0);

		assertTrue(cl0.creationTime >= current);
		
		current = cl0.creationTime;

		FutureLoader cl1 = cl0.id(1);
		System.out.println(cl1);
		
		assertEquals(1,cl1.key);
		assertEquals(cl0.creationTime,cl1.creationTime);

		FutureLoader cl2et = cl1.expirationTime(current+1000);
		FutureLoader cl2in = cl1.expirationTime(Instant.ofEpochMilli(current+1000));
		FutureLoader cl2ttl = cl1.ttl(1000,TimeUnit.MILLISECONDS);
		FutureLoader cl2dur = cl1.ttl(Duration.ofMillis(1000));
		
		System.out.println(cl2et);
		System.out.println(cl2in);
		System.out.println(cl2ttl);
		System.out.println(cl2dur);
		assertEquals(cl2et.creationTime,cl2in.creationTime);
		assertEquals(cl2et.creationTime,cl2ttl.creationTime);
		assertEquals(cl2et.creationTime,cl2dur.creationTime);

		assertEquals(0,cl2in.ttlMsec);
		assertEquals(0,cl2et.ttlMsec);
		assertEquals(1000,cl2ttl.ttlMsec);
		assertEquals(1000,cl2dur.ttlMsec);

		assertNotNull(cl2et.get());
		
	}
	
	@Test(expected=ExecutionException.class)
	public void testFailingFuture() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> c = new AssemblingConveyor<>();
		CompletableFuture<User> f = c.future().id(1).get();
		f.get();
	}

	@Test
	public void testCanceledFuture() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> c = new AssemblingConveyor<>();
		CompletableFuture<Boolean> b = c.build().id(1).supplier(UserBuilder::new).create();
		assertTrue(b.get());
		CompletableFuture<User> f = c.future().id(1).get();
		c.command().id(1).cancel();
		try{
			f.get();
			fail("unexpected result");
		} catch (Exception e) {
		}
	}

	@Test
	public void testFuture() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> c = new AssemblingConveyor<>();
		c.setReadinessEvaluator(x->true);
		c.setDefaultCartConsumer(Conveyor.getConsumerFor(c).filter(l->true, v->{}));
		c.setResultConsumer(bin->{
			System.out.println(bin);
		});
		CompletableFuture<Boolean> b = c.build().id(1).supplier(UserBuilder::new).create();
		assertTrue(b.get());
		CompletableFuture<User> f = c.future().id(1).get();
		c.part().id(1).place();
		User u = f.get();
		assertNotNull(u);
	}

}
