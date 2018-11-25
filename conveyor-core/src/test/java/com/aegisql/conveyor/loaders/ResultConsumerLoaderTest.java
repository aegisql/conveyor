package com.aegisql.conveyor.loaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

import com.aegisql.conveyor.consumers.result.ResultConsumer;

public class ResultConsumerLoaderTest {

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

	Integer defaultInteger = -1;
	String  defaultString = "def";

	Integer keyInteger = -1;
	String  keyString = "key";

	private void consumer(ResultConsumer<Integer,String> rc) {
		
		rc.accept(null);
	}
	
	@Test
	public void testResultConsumerPlain() throws InterruptedException, ExecutionException {
		ResultConsumerLoader<Integer, String> rc = new ResultConsumerLoader<>(rcl->{
			System.out.println("For Key "+rcl);
			return null;
		}, this::consumer, bin->{
			System.out.println("Default "+bin);
			defaultInteger = 1;
			defaultString  = "DEFAULT_1";
		});
		
		CompletableFuture<Boolean> f = rc.set();
		
		assertNotNull(f);
		assertTrue(f.get());
		
		assertEquals(Integer.valueOf(1), defaultInteger);
		assertEquals("DEFAULT_1", defaultString);
		
	}

	@Test
	public void testResultConsumer2() throws InterruptedException, ExecutionException {
		ResultConsumerLoader<Integer, String> rc = new ResultConsumerLoader<>(rcl->{
			System.out.println("For Key "+rcl);
			return null;
		}, this::consumer, bin->{
			System.out.println("Default "+bin);
			defaultInteger = 1;
			defaultString  = "DEFAULT_1";
		});
		
		CompletableFuture<Boolean> f = rc.first(bin->{
			System.out.println("First "+bin);
			defaultInteger = 2;
			defaultString  = "DEFAULT_2";
		}).set();
		
		assertNotNull(f);
		assertTrue(f.get());
		
		assertEquals(Integer.valueOf(2), defaultInteger);
		assertEquals("DEFAULT_2", defaultString);
		
	}

	@Test
	public void testResultConsumer3() throws InterruptedException, ExecutionException {
		ResultConsumerLoader<Integer, String> rc = new ResultConsumerLoader<>(rcl->{
			System.out.println("For Key "+rcl);
			return null;
		}, this::consumer, bin->{
			System.out.println("Default "+bin);
			defaultInteger = 1;
			defaultString  = "DEFAULT_1";
		});
		
		CompletableFuture<Boolean> f = rc.andThen(bin->{
			System.out.println("First "+bin);
			defaultInteger = defaultInteger + 2;
			defaultString  = defaultString+"_2";
		}).set();
		
		assertNotNull(f);
		assertTrue(f.get());
		
		assertEquals(Integer.valueOf(3), defaultInteger);
		assertEquals("DEFAULT_1_2", defaultString);
		
	}

	@Test
	public void testResultConsumer4() throws InterruptedException, ExecutionException {
		ResultConsumerLoader<Integer, String> rc = new ResultConsumerLoader<>(rcl->{
			System.out.println("For Key "+rcl);
			return null;
		}, this::consumer, bin->{
			System.out.println("Default "+bin);
			defaultInteger = defaultInteger + 2;
			defaultString  = "DEFAULT_1";
		});
		
		CompletableFuture<Boolean> f = rc.before(bin->{
			System.out.println("First "+bin);
			defaultInteger = 1;
			defaultString  = defaultString+"_2";
		}).set();
		
		assertNotNull(f);
		assertTrue(f.get());
		
		assertEquals(Integer.valueOf(3), defaultInteger);
		assertEquals("DEFAULT_1", defaultString);
		
	}

	
	@Test
	public void testResultConsumerTime() throws InterruptedException, ExecutionException {
		long current = System.currentTimeMillis();
		
		ResultConsumerLoader<Integer, String> bl0 = new ResultConsumerLoader<>(rcl->{
			System.out.println("For Key "+rcl);
			assertEquals(1, rcl.priority);
			return null;
		}, this::consumer, bin->{
			System.out.println("Default "+bin);
			defaultInteger = 1;
			defaultString  = "DEFAULT_1";
		});
		
		
		assertTrue(bl0.creationTime >= current);
		
		current = bl0.creationTime;
		
		ResultConsumerLoader<Integer, String> cl1 = bl0.id(1).priority(1);
		System.out.println(cl1);
		
		assertEquals(Integer.valueOf(1),cl1.key);
		assertEquals(bl0.creationTime,cl1.creationTime);

		ResultConsumerLoader<Integer, String> cl2et = cl1.expirationTime(current+1000);
		ResultConsumerLoader<Integer, String> cl2in = cl1.expirationTime(Instant.ofEpochMilli(current+1000));
		ResultConsumerLoader<Integer, String> cl2ttl = cl1.ttl(1000,TimeUnit.MILLISECONDS);
		ResultConsumerLoader<Integer, String> cl2dur = cl1.ttl(Duration.ofMillis(1000));
		
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

		
		CompletableFuture<Boolean> f = bl0.andThen(bin->{
			System.out.println("First "+bin);
			defaultInteger = defaultInteger + 2;
			defaultString  = defaultString+"_2";
		}).set();
		
		assertNotNull(f);
		assertTrue(f.get());
		
		assertEquals(Integer.valueOf(3), defaultInteger);
		assertEquals("DEFAULT_1_2", defaultString);
		cl1.set();
	}

	@Test
	public void propertiesLoader() {
		long current = System.currentTimeMillis();
		
		ResultConsumerLoader<Integer, String> pl0 = new ResultConsumerLoader<>(rcl->{
			System.out.println("For Key "+rcl);
			return null;
		}, this::consumer, bin->{
			System.out.println("Default "+bin);
			defaultInteger = 1;
			defaultString  = "DEFAULT_1";
		});
		System.out.println(pl0);
		assertEquals(0, pl0.getAllProperties().size());
		ResultConsumerLoader<Integer, String> pl1 = pl0.addProperty("A", 1);
		System.out.println(pl1);
		assertEquals(1, pl1.getAllProperties().size());

		ResultConsumerLoader<Integer, String> pl2 = pl1.addProperty("B", "X");
		System.out.println(pl2);
		assertEquals(0, pl0.getAllProperties().size());
		assertEquals(1, pl1.getAllProperties().size());
		assertEquals(2, pl2.getAllProperties().size());

		ResultConsumerLoader<Integer, String> pl31 = pl2.clearProperties();
		assertEquals(0, pl31.getAllProperties().size());

		ResultConsumerLoader<Integer, String> pl32 = pl2.clearProperty("A");
		assertEquals(1, pl32.getAllProperties().size());

		ResultConsumerLoader<Integer, String> pl33 = pl2.andThen(x->{});
		assertEquals(2, pl33.getAllProperties().size());

	}
	
}
