package com.aegisql.conveyor.loaders;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.multichannel.UserBuilder;
import com.aegisql.conveyor.multichannel.UserBuilderEvents;
import com.aegisql.conveyor.user.User;

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
	public void testResultConsumerTime() throws InterruptedException, ExecutionException {
		long current = System.currentTimeMillis();
		
		ResultConsumerLoader<Integer, String> bl0 = new ResultConsumerLoader<>(rcl->{
			System.out.println("For Key "+rcl);
			return null;
		}, this::consumer, bin->{
			System.out.println("Default "+bin);
			defaultInteger = 1;
			defaultString  = "DEFAULT_1";
		});
		
		
		assertTrue(bl0.creationTime >= current);
		
		current = bl0.creationTime;
		
		ResultConsumerLoader<Integer, String> cl1 = bl0.id(1);
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
		
	}

	@Test
	public void testWithConveyor() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("multyKeyParts");
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).andThen(bin->{
			System.out.println("-----");
		}).set();
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		
		
		c.part().id(1).label(UserBuilderEvents.SET_FIRST).value("A").place();
		c.part().id(1).label(UserBuilderEvents.SET_LAST).value("B").place();
		c.part().id(1).label(UserBuilderEvents.SET_YEAR).value(2017).place();
		c.completeAndStop().get();
		
		
	}

	@Test
	public void testWithConveyorAndId() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("multyKeyParts");
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).andThen(bin->{
			System.out.println("-----");
		}).set();
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		
		CompletableFuture<User> f = c.build().id(1).createFuture();
		
		c.resultConsumer().id(1).first(LogResult.stdErr(c)).andThen(bin->{
			System.err.println("+++++");
		}).set();
		
		c.part().id(1).label(UserBuilderEvents.SET_FIRST).value("A").place();
		c.part().id(2).label(UserBuilderEvents.SET_FIRST).value("A2").place();
		c.part().id(1).label(UserBuilderEvents.SET_LAST).value("B").place();
		c.part().id(2).label(UserBuilderEvents.SET_LAST).value("B2").place();
		c.part().id(1).label(UserBuilderEvents.SET_YEAR).value(2017).place();
		c.part().id(2).label(UserBuilderEvents.SET_YEAR).value(2007).place();
		
		User u = f.get();
		assertNotNull(u);
		
		c.completeAndStop().get();
		
		
	}

	@Test
	public void testWithConveyorForeach() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("multyKeyParts");
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).andThen(bin->{
			System.out.println("-----");
		}).set();
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		
		CompletableFuture<User> f = c.build().id(1).createFuture();
		
		
		c.part().id(1).label(UserBuilderEvents.SET_FIRST).value("A").place();
		c.part().id(2).label(UserBuilderEvents.SET_FIRST).value("A2").place();

		c.resultConsumer().foreach().first(LogResult.stdErr(c)).andThen(bin->{
			System.err.println("+++++");
		}).set();

		c.part().id(1).label(UserBuilderEvents.SET_LAST).value("B").place();
		c.part().id(2).label(UserBuilderEvents.SET_LAST).value("B2").place();
		c.part().id(1).label(UserBuilderEvents.SET_YEAR).value(2017).place();
		c.part().id(2).label(UserBuilderEvents.SET_YEAR).value(2007).place();
		
		User u = f.get();
		assertNotNull(u);
		
		c.completeAndStop().get();
		
	}

	@Test
	public void testWithConveyorFilter() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("multyKeyParts");
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).andThen(bin->{
			System.out.println("-----");
		}).set();
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		
		CompletableFuture<User> f = c.build().id(1).createFuture();
		
		
		c.part().id(1).label(UserBuilderEvents.SET_FIRST).value("A").place();
		c.part().id(2).label(UserBuilderEvents.SET_FIRST).value("A2").place();

		c.resultConsumer().foreach(k->k%2==0).first(LogResult.stdErr(c)).andThen(bin->{
			System.err.println("+++++");
		}).set();

		c.part().id(1).label(UserBuilderEvents.SET_LAST).value("B").place();
		c.part().id(2).label(UserBuilderEvents.SET_LAST).value("B2").place();
		c.part().id(1).label(UserBuilderEvents.SET_YEAR).value(2017).place();
		c.part().id(2).label(UserBuilderEvents.SET_YEAR).value(2007).place();
		
		User u = f.get();
		assertNotNull(u);
		
		c.completeAndStop().get();
		
	}

	
}
