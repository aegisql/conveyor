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

public class BuilderLoaderTest {

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
	public void test() throws InterruptedException, ExecutionException {
		
		long current = System.currentTimeMillis();

		BuilderLoader<Integer,String,Boolean> bl0 = new BuilderLoader<>(p->{
			CompletableFuture<Boolean> cf = new CompletableFuture<Boolean>();
			cf.complete(true);
			return cf;
		}, fp->{
			CompletableFuture<String> cf = new CompletableFuture<String>();
			cf.complete("test");
			return cf;

		});
		System.out.println(bl0);
		assertTrue(bl0.creationTime >= current);
		
		current = bl0.creationTime;
		
		BuilderLoader cl1 = bl0.id(1);
		System.out.println(cl1);
		
		assertEquals(1,cl1.key);
		assertEquals(bl0.creationTime,cl1.creationTime);

		BuilderLoader cl2et = cl1.expirationTime(current+1000);
		BuilderLoader cl2in = cl1.expirationTime(Instant.ofEpochMilli(current+1000));
		BuilderLoader cl2ttl = cl1.ttl(1000,TimeUnit.MILLISECONDS);
		BuilderLoader cl2dur = cl1.ttl(Duration.ofMillis(1000));
		
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

		CompletableFuture<Boolean> cf1 = cl2et.create();
		assertNotNull(cf1);
		assertTrue(cf1.get());
		CompletableFuture<String> cf2 = cl2et.createFuture();
		assertNotNull(cf2);
		assertEquals("test",cf2.get());
	}

}
