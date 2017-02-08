package com.aegisql.conveyor.loaders;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.user.UserBuilder;

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
		fail("Unimplemented");
	}

}
