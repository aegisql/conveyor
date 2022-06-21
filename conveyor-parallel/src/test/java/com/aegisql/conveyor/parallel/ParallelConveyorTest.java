/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.parallel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.loaders.CommandLoader;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingBuilder;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class ParallelConveyorTest.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class ParallelConveyorTest {
	
	/** The size. */
	public static int SIZE = 10000;
	
	/** The in user. */
	public static User[] inUser  = new User[SIZE];
	
	/** The out user. */
	public static User[] outUser = new User[SIZE];

	/** The conveyor. */
	public static 		ParallelConveyor<String, String, User> 
	conveyor = new KBalancedParallelConveyor<>(4);

	
	/**
	 * Sets the up before class.
	 *
	 */
	@BeforeClass
	public static void setUpBeforeClass() {

		conveyor.setBuilderSupplier( UserBuilder::new );
	    conveyor.setDefaultCartConsumer( (label, value, builder) -> {
		UserBuilder userBuilder = (UserBuilder) builder;
		if(label == null) {
			userBuilder.setReady(true);
			return;
		}
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
	    } );
	    conveyor.setReadinessEvaluator( (state, builder) -> {
		UserBuilder userBuilder = (UserBuilder) builder;
		return state.previouslyAccepted == 3 || userBuilder.ready();
	    });
   
		conveyor.setName("Parallel User Builder");
		
		assertTrue(conveyor.isRunning());
		assertTrue(conveyor.isRunning(0));
		assertTrue(conveyor.isRunning(1));
		assertTrue(conveyor.isRunning(2));
		assertTrue(conveyor.isRunning(3));
		assertFalse(conveyor.isRunning(-1));
		assertFalse(conveyor.isRunning(4));
		
	}

	/**
	 * Tear down after class.
	 *
	 * @throws Exception the exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		conveyor.stop();
		Thread.sleep(100);
		assertFalse(conveyor.isRunning());
		assertFalse(conveyor.isRunning(0));
		assertFalse(conveyor.isRunning(1));
		assertFalse(conveyor.isRunning(2));
		assertFalse(conveyor.isRunning(3));

	}

	/**
	 * Sets the up.
	 *
	 */
	@Before
	public void setUp() {
	}

	/**
	 * Tear down.
	 *
	 */
	@After
	public void tearDown() {
	}
	
	/**
	 * Gets the random ints.
	 *
	 * @return the random ints
	 */
	public static int[] getRandomInts() {
		List<Integer> ids = new ArrayList<>(SIZE);
		for(int i = 0; i < SIZE; i++) {
			ids.add(i);
		}
		Collections.shuffle(ids);
		int[] idInt = new int[SIZE];
		for(int i = 0; i < SIZE; i++) {
			idInt[i] = ids.get(i);
		}
		return idInt;
	}
	
	/**
	 * Test exception.
	 */
	@Test(expected=NullPointerException.class)
	public void testException() {
		conveyor.place(null);
	}

	/**
	 * Test parallel command.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void testParallelCommand() throws InterruptedException, ExecutionException {
		
		CommandLoader<String, ?> cmd = conveyor.command().ttl(100,TimeUnit.MILLISECONDS);
		CompletableFuture<Boolean> f1 = cmd.id(""+1).create();
		CompletableFuture<Boolean> f2 = cmd.id(""+2).create();
		CompletableFuture<Boolean> f3 = cmd.id(""+3).create();
		CompletableFuture<Boolean> f4 = cmd.id(""+4).create();

		System.out.println("F1 "+f1);
		System.out.println("F2 "+f2);
		System.out.println("F3 "+f3);
		System.out.println("F4 "+f4);
		assertTrue(f1.get());
		assertTrue(f2.get());
		assertTrue(f3.get());
		assertTrue(f4.get());
		System.out.println("F1 "+f1);
		System.out.println("F2 "+f2);
		System.out.println("F3 "+f3);
		System.out.println("F4 "+f4);
	}
	
	/**
	 * Test parallel.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testParallel() throws InterruptedException {

	conveyor.setIdleHeartBeat(1000, TimeUnit.MILLISECONDS);
	conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
	assertFalse(conveyor.isOnTimeoutAction());
	conveyor.setOnTimeoutAction((builder)->{
		System.out.println("timeout "+builder.get());
	});
	assertTrue(conveyor.isOnTimeoutAction());
	/** The out queue. */
	ResultQueue<String,User> outQueue = new ResultQueue<>();

	conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		
	Thread runFirst = new Thread(()->{
		Random r = new Random();
		int[] ids = getRandomInts();
		for(int i = 0; i < SIZE; i++) {
			User u = inUser[ids[i]];
			Cart<String, String, String> cart = new ShoppingCart<>(""+ids[i], "First_"+ids[i], "setFirst",1,TimeUnit.SECONDS);
			if(r.nextInt(100) == 22) continue;
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			conveyor.place(cart);
		}
	});	

	Thread runLast = new Thread(()->{
		Random r = new Random();
		int[] ids = getRandomInts();
		for(int i = 0; i < SIZE; i++) {
			User u = inUser[ids[i]];
			Cart<String, String, String> cart = new ShoppingCart<>(""+ids[i], "Last_"+ids[i], "setLast",1,TimeUnit.SECONDS);
			if(r.nextInt(100) == 22) continue;
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			conveyor.place(cart);
		}
	});	

	Thread runYear = new Thread(()->{
		Random r = new Random();
		int[] ids = getRandomInts();
		for(int i = 0; i < SIZE; i++) {
			User u = inUser[ids[i]];
			Cart<String, Integer, String> cart = new ShoppingCart<>(""+ids[i], 1900+r.nextInt(100), "setYearOfBirth",1,TimeUnit.SECONDS);
			if(r.nextInt(100) == 22) continue;
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			conveyor.place(cart);
		}
	});	
	
	runFirst.start();
	runLast.start();
	runYear.start();
		
	while(	runFirst.isAlive() || runLast.isAlive() || runYear.isAlive() ) {
		Thread.sleep(1000);
	}

	System.out.println("Good data "+outQueue.size());
	while(!outQueue.isEmpty()) {
		System.out.println(outQueue.poll());
	}
	

	System.out.println("Left: "+conveyor.getCollectorSize(0));
	Thread.sleep(3000);

	System.out.println("Incomplete data "+outQueue.size());
	while(!outQueue.isEmpty()) {
		System.out.println(outQueue.poll());
	}

	assertEquals(0,conveyor.getInputQueueSize(0));
	assertEquals(0,conveyor.getCollectorSize(0));
	assertEquals(0,conveyor.getDelayedQueueSize(0));
	assertEquals(0,conveyor.getInputQueueSize(1));
	assertEquals(0,conveyor.getCollectorSize(1));
	assertEquals(0,conveyor.getDelayedQueueSize(1));

	System.out.println("total "+conveyor.getCartCounter());
	System.out.println("1 "+conveyor.getCartCounter(0));
	System.out.println("2 "+conveyor.getCartCounter(1));
	System.out.println("3 "+conveyor.getCartCounter(2));
	System.out.println("4 "+conveyor.getCartCounter(3));
	assertTrue(conveyor.getCartCounter(0) > 100);
	assertTrue(conveyor.getCartCounter(1) > 100);
	assertTrue(conveyor.getCartCounter(2) > 100);
	assertTrue(conveyor.getCartCounter(3) > 100);
	//Thread.sleep(10000000);
	
	}

	/**
	 * The Class StringToUserBuulder.
	 */
	static class StringToUserBuulder extends ScalarConvertingBuilder<String,User> {
		
		/* (non-Javadoc)
		 * @see java.util.function.Supplier#get()
		 */
		@Override
		public User get() {
			String[] fields = scalar.split(",");
			return new User(fields[0], fields[1], Integer.parseInt(fields[2]));
		}
		
	}

	/**
	 * Other conveyor test.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void otherConveyorTest() throws InterruptedException {
		KBalancedParallelConveyor<String, String, User>
		conveyor = new KBalancedParallelConveyor<>(ScalarConvertingConveyor::new,4);
		conveyor.setBuilderSupplier(StringToUserBuulder::new);
		LastResultReference<String,User> usr = LastResultReference.of(conveyor);
		conveyor.resultConsumer().first(usr).andThen(LogResult.stdOut(conveyor)).set();

		conveyor.part().id("1").value("John,Dow1,1990").place();
		conveyor.part().id("2").value("John,Dow1,1991").place();
		conveyor.part().id("2").value("John,Dow1,1992").place();
		conveyor.part().id("2").value("John,Dow1,1993").place();
		Thread.sleep(20);
		assertNotNull(usr.getCurrent());

	}

	@Test
	public void waitUntilCompleteConveyorTest() throws InterruptedException, ExecutionException {
		KBalancedParallelConveyor<String, String, User>
		conveyor = new KBalancedParallelConveyor<>(ScalarConvertingConveyor::new,4);
		conveyor.setBuilderSupplier(StringToUserBuulder::new);
		/** The out queue. */
		ResultQueue<String,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		for(int i =0;i<1000;i++) {
			conveyor.part().id(""+i).value("John,Dow1,"+i).place();
		}
		CompletableFuture<Boolean> f = conveyor.completeAndStop();
		f.get();
		assertEquals(1000, outQueue.size());
	}

	
	/**
	 * Future conveyor test.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 * @throws TimeoutException the timeout exception
	 */
	@Test(expected=TimeoutException.class)
	public void futureConveyorTest() throws InterruptedException, ExecutionException, TimeoutException {
		KBalancedParallelConveyor<String, String, User>
		conveyor = new KBalancedParallelConveyor<>(ScalarConvertingConveyor::new,4);
		conveyor.setBuilderSupplier(StringToUserBuulder::new);
		LastResultReference<String,User> usr = LastResultReference.of(conveyor);
		conveyor.resultConsumer().first(usr).andThen(LogResult.stdOut(conveyor)).set();

		CompletableFuture<User> uf1 = conveyor.future().id("1").get();
		CompletableFuture<User> uf2 = conveyor.future().id("2").get();
		CompletableFuture<User> uf3 = conveyor.future().id("3").get();

		CompletableFuture<Boolean> f1 = conveyor.part().id("1").value("John,Dow1,1990").place();
		CompletableFuture<Boolean> f2 = conveyor.part().id("2").value("John,Dow1,1991").place();
		CompletableFuture<Boolean> f3 = conveyor.part().id("2").value("John,Dow1,1992").place();
		CompletableFuture<Boolean> f4 = conveyor.part().id("2").value("John,Dow1,1993").place();
		
		assertTrue(f1.get());
		assertTrue(f2.get());
		assertTrue(f3.get());
		assertTrue(f4.get());
		assertNotNull(usr.getCurrent());
		assertNotNull(uf1.get());
		assertNotNull(uf2.get());
		assertNotNull(uf3.get(1,TimeUnit.SECONDS));

	}

	/**
	 * Creates the future conveyor test.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void createFutureConveyorTest() throws InterruptedException, ExecutionException {
		KBalancedParallelConveyor<String, String, User>
		conveyor = new KBalancedParallelConveyor<>(ScalarConvertingConveyor::new,4);
		LastResultReference<String,User> usr = LastResultReference.of(conveyor);
		conveyor.resultConsumer().first(usr).andThen(LogResult.stdOut(conveyor)).set();

		CompletableFuture<User> uf1 = conveyor.build().id("1").supplier(StringToUserBuulder::new).createFuture();
		CompletableFuture<User> uf2 = conveyor.build().id("2").supplier(StringToUserBuulder::new).createFuture();

		
		CompletableFuture<Boolean> f1 = conveyor.part().id("1").value("John,Dow1,1990").place();
		CompletableFuture<Boolean> f2 = conveyor.part().id("2").value("John,Dow1,1991").place();
		
		assertTrue(f1.get());
		assertTrue(f2.get());
		assertNotNull(usr.getCurrent());
		assertNotNull(uf1.get());
		assertNotNull(uf2.get());

	}

	/**
	 * Creates the future conveyor test2.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void createFutureConveyorTest2() throws InterruptedException, ExecutionException {
		KBalancedParallelConveyor<String, String, User>
		conveyor = new KBalancedParallelConveyor<>(ScalarConvertingConveyor::new,4);
		conveyor.setBuilderSupplier(StringToUserBuulder::new);
		LastResultReference<String,User> usr = LastResultReference.of(conveyor);
		conveyor.resultConsumer().first(usr).andThen(LogResult.stdOut(conveyor)).set();
		CompletableFuture<User> uf1 = conveyor.build().id("1").createFuture();
		CompletableFuture<User> uf2 = conveyor.build().id("2").createFuture();

		
		CompletableFuture<Boolean> f1 = conveyor.part().id("1").value("John,Dow1,1990").place();
		CompletableFuture<Boolean> f2 = conveyor.part().id("2").value("John,Dow1,1991").place();
		
		assertTrue(f1.get());
		assertTrue(f2.get());
		assertNotNull(usr.getCurrent());
		assertNotNull(uf1.get());
		assertNotNull(uf2.get());

	}

	@Test
	public void createSuspendedConveyorTest()  {
		KBalancedParallelConveyor<String, String, User>
		conveyor = new KBalancedParallelConveyor<>(ScalarConvertingConveyor::new,4);
		LastResultReference<String,User> usr = LastResultReference.of(conveyor);
		conveyor.resultConsumer().first(usr).andThen(LogResult.stdOut(conveyor)).set();

		CompletableFuture<User> uf1 = conveyor.build().id("1").supplier(StringToUserBuulder::new).createFuture();
		CompletableFuture<User> uf2 = conveyor.build().id("2").supplier(StringToUserBuulder::new).createFuture();
		conveyor.suspend();
		
		CompletableFuture<Boolean> f1 = conveyor.part().id("1").value("John,Dow1,1990").place();
		CompletableFuture<Boolean> f2 = conveyor.part().id("2").value("John,Dow1,1991").place();

		assertNull(usr.getCurrent());
		try {
			assertNotNull(f1.get(1,TimeUnit.SECONDS));
			assertNotNull(f2.get(1,TimeUnit.SECONDS));
			fail("Must not reach this line in suspended test");
		} catch (Exception e) {
		} finally {
			conveyor.resume();
		}
		conveyor.completeAndStop().join();
		assertNotNull(usr.getCurrent());

	}

	@Test
	public void testAccessByName() {
		KBalancedParallelConveyor<String, String, User>
		ac1 = new KBalancedParallelConveyor<>(ScalarConvertingConveyor::new,4);
		
		ac1.setName("test_name");
		
		Conveyor<String,String,User> ac2 = Conveyor.byName("test_name");
		
		assertTrue(ac1==ac2);
	}
}
