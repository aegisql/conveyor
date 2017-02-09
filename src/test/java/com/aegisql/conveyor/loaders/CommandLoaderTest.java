package com.aegisql.conveyor.loaders;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.multichannel.UserBuilderEvents;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import com.aegisql.conveyor.utils.parallel.KBalancedParallelConveyor;
import com.aegisql.conveyor.utils.parallel.ParallelConveyor;

public class CommandLoaderTest {

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
	public void testSingleKey() {
		long current = System.currentTimeMillis();

		CommandLoader cl0 = new CommandLoader<>(c->{
			System.out.println("Final: "+c);
			return new CompletableFuture();
		});
		
		System.out.println(cl0);

		assertTrue(cl0.creationTime >= current);
		
		current = cl0.creationTime;

		CommandLoader cl1 = cl0.id(1);
		System.out.println(cl1);
		
		assertEquals(1,cl1.key);
		assertEquals(cl0.creationTime,cl1.creationTime);

		CommandLoader cl2et = cl1.expirationTime(current+1000);
		CommandLoader cl2in = cl1.expirationTime(Instant.ofEpochMilli(current+1000));
		CommandLoader cl2ttl = cl1.ttl(1000,TimeUnit.MILLISECONDS);
		CommandLoader cl2dur = cl1.ttl(Duration.ofMillis(1000));
		
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

		CompletableFuture cancel = cl2in.cancel();
		assertNotNull(cancel);
		CompletableFuture create = cl2in.create();
		assertNotNull(create);
		CompletableFuture create2 = cl2in.create(BuilderSupplier.of(UserBuilder::new));
		assertNotNull(create2);
		CompletableFuture check = cl2in.check();
		assertNotNull(check);
		CompletableFuture reschedule = cl2in.reschedule();
		assertNotNull(reschedule);
		CompletableFuture timeout = cl2in.timeout();
		assertNotNull(timeout);
	}

	@Test
	public void testMultiKey() {
		long current = System.currentTimeMillis();

		MultiKeyCommandLoader cl0 = new MultiKeyCommandLoader<>(c->{
			System.out.println("Final: "+c);
			return new CompletableFuture();
		});
		
		System.out.println(cl0);

		assertTrue(cl0.creationTime >= current);
		
		current = cl0.creationTime;
		
		MultiKeyCommandLoader cl1 = cl0;
		System.out.println(cl1);
		
		MultiKeyCommandLoader cl2et = cl1.expirationTime(current+1000);
		MultiKeyCommandLoader cl2in = cl1.expirationTime(Instant.ofEpochMilli(current+1000));
		MultiKeyCommandLoader cl2ttl = cl1.ttl(1000,TimeUnit.MILLISECONDS);
		MultiKeyCommandLoader cl2dur = cl1.ttl(Duration.ofMillis(1000));
		
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

		CompletableFuture cancel = cl2in.cancel();
		assertNotNull(cancel);
		CompletableFuture reschedule = cl2in.reschedule();
		assertNotNull(reschedule);
		CompletableFuture timeout = cl2in.timeout();
		assertNotNull(timeout);

	}

	@Test
	public void testMultiKeyCancelCommand() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setBuilderSupplier(UserBuilder::new);
		c.setResultConsumer(bin->{
			System.out.println(bin);
		});
		CompletableFuture<Boolean> cf1 = c.build().id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().id(3).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		
		CompletableFuture<User> f1 = c.future().id(1).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f3 = c.future().id(3).get();
		
		assertEquals(3,c.getCollectorSize());
		
		c.multiKeyCommand().foreach().cancel();
		
		try {
			Thread.sleep(10);
			f3.get();
			fail("Not expected future");
		} catch(Exception e) {
			assertEquals(0,c.getCollectorSize());
		}
		
	}

	@Test
	public void testMultiKeyCancelParallelCommand() throws InterruptedException, ExecutionException {
		ParallelConveyor<Integer, UserBuilderEvents, User> c = new KBalancedParallelConveyor<>(2);
		c.setBuilderSupplier(UserBuilder::new);
		c.setResultConsumer(bin->{
			System.out.println(bin);
		});
		CompletableFuture<Boolean> cf1 = c.build().id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().id(3).create();
		CompletableFuture<Boolean> cf4 = c.build().id(4).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		assertTrue(cf4.get());
		
		CompletableFuture<User> f1 = c.future().id(1).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f3 = c.future().id(3).get();
		CompletableFuture<User> f4 = c.future().id(4).get();
		
		assertEquals(2,c.getCollectorSize(0));
		assertEquals(2,c.getCollectorSize(1));
		
		c.multiKeyCommand().foreach().cancel();
		
		try {
			Thread.sleep(10);
			f4.get();
			fail("Not expected future");
		} catch(Exception e) {
			assertEquals(0,c.getCollectorSize(0));
			assertEquals(0,c.getCollectorSize(1));
		}
	}

	
}
