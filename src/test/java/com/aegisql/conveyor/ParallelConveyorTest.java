/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.AbstractCommand;
import com.aegisql.conveyor.cart.command.CreateCommand;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import com.aegisql.conveyor.utils.parallel.ParallelConveyor;
import com.aegisql.conveyor.utils.scalar.ScalarCart;
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
	public static 		ParallelConveyor<Integer, String, User> 
	conveyor = new ParallelConveyor<>(4);

	
	/**
	 * Sets the up before class.
	 *
	 * @throws Exception the exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

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
	
	/** The out queue. */
	public static Queue<User> outQueue = new ConcurrentLinkedQueue<>();

	
	
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
		conveyor.add(null);
	}

	/**
	 * Test parallel command.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testParallelCommand() throws InterruptedException {
		
		AbstractCommand<Integer,?> c1 = new CreateCommand<>(1,UserBuilder::new,10,TimeUnit.MILLISECONDS );
		AbstractCommand<Integer,?> c2 = new CreateCommand<>(2,UserBuilder::new,10,TimeUnit.MILLISECONDS );
		AbstractCommand<Integer,?> c3 = new CreateCommand<>(3,UserBuilder::new,10,TimeUnit.MILLISECONDS );
		AbstractCommand<Integer,?> c4 = new CreateCommand<>(4,UserBuilder::new,10,TimeUnit.MILLISECONDS );

		
		conveyor.addCommand(c1);
		conveyor.addCommand(c2);
		conveyor.addCommand(c3);
		conveyor.addCommand(c4);
		
		Thread.sleep(50);
		
		
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
		System.out.println("---");
	});
	assertTrue(conveyor.isOnTimeoutAction());
	conveyor.setResultConsumer( res->{
		    	outQueue.add(res.product);
		    });
		
	Thread runFirst = new Thread(()->{
		Random r = new Random();
		int[] ids = getRandomInts();
		for(int i = 0; i < SIZE; i++) {
			User u = inUser[ids[i]];
			Cart<Integer, String, String> cart = new ShoppingCart<>(ids[i], "First_"+ids[i], "setFirst",1,TimeUnit.SECONDS);
			if(r.nextInt(100) == 22) continue;
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			conveyor.offer(cart);
		}
	});	

	Thread runLast = new Thread(()->{
		Random r = new Random();
		int[] ids = getRandomInts();
		for(int i = 0; i < SIZE; i++) {
			User u = inUser[ids[i]];
			Cart<Integer, String, String> cart = new ShoppingCart<>(ids[i], "Last_"+ids[i], "setLast",1,TimeUnit.SECONDS);
			if(r.nextInt(100) == 22) continue;
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			conveyor.offer(cart);
		}
	});	

	Thread runYear = new Thread(()->{
		Random r = new Random();
		int[] ids = getRandomInts();
		for(int i = 0; i < SIZE; i++) {
			User u = inUser[ids[i]];
			Cart<Integer, Integer, String> cart = new ShoppingCart<>(ids[i], 1900+r.nextInt(100), "setYearOfBirth",1,TimeUnit.SECONDS);
			if(r.nextInt(100) == 22) continue;
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			conveyor.offer(cart);
		}
	});	
	
	runFirst.start();
	runLast.start();
	runYear.start();
		
	while(	runFirst.isAlive() || runLast.isAlive() || runYear.isAlive() ) {
		Thread.sleep(1000);
	}

	System.out.println("Good data");
	while(!outQueue.isEmpty()) {
		System.out.println(outQueue.poll());
	}
	

	System.out.println("Left: "+conveyor.getCollectorSize(0));
	Thread.sleep(3000);

	System.out.println("Incomplete data");
	while(!outQueue.isEmpty()) {
		System.out.println(outQueue.poll());
	}

	assertEquals(0,conveyor.getInputQueueSize(0));
	assertEquals(0,conveyor.getCollectorSize(0));
	assertEquals(0,conveyor.getDelayedQueueSize(0));
	assertEquals(0,conveyor.getInputQueueSize(1));
	assertEquals(0,conveyor.getCollectorSize(1));
	assertEquals(0,conveyor.getDelayedQueueSize(1));

	}

	static class StringToUserBuulder extends ScalarConvertingBuilder<String,User> {
		@Override
		public User get() {
			String[] fields = scalar.split(",");
			return new User(fields[0], fields[1], Integer.parseInt(fields[2]));
		}
		
	}

	@Test
	public void otherConveyorTest() throws InterruptedException {
		Conveyor<String, SmartLabel<ScalarConvertingBuilder<String, ?>>, User>
		conveyor = new ParallelConveyor<>(ScalarConvertingConveyor::new,4);
		conveyor.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		conveyor.setResultConsumer(u->{
			System.out.println("RESULT: "+u);
			usr.set(u.product);
		});

		ScalarCart<String, String> c1 = new ScalarCart<>("1", "John,Dow,1990");
		ScalarCart<String, String> c2 = new ScalarCart<>("2", "Jane,Dow,1990");
		conveyor.add(c1);
		conveyor.add(c2);
		Thread.sleep(20);
		assertNotNull(usr.get());

	}
	 
	@Test
	public void testArrayConstructor() throws InterruptedException {
		ScalarConvertingConveyor<String, SmartLabel<ScalarConvertingBuilder<String, ?>>, User> ac1 = new ScalarConvertingConveyor<>(); 
		ScalarConvertingConveyor<String, SmartLabel<ScalarConvertingBuilder<String, ?>>, User> ac2 = new ScalarConvertingConveyor<>(); 
		ScalarConvertingConveyor<String, SmartLabel<ScalarConvertingBuilder<String, ?>>, User> ac3 = new ScalarConvertingConveyor<>(); 
		ScalarConvertingConveyor<String, SmartLabel<ScalarConvertingBuilder<String, ?>>, User> ac4 = new ScalarConvertingConveyor<>(); 
		ac1.setBuilderSupplier(StringToUserBuulder::new);
		ac2.setBuilderSupplier(StringToUserBuulder::new);
		ac3.setBuilderSupplier(StringToUserBuulder::new);
		ac4.setBuilderSupplier(StringToUserBuulder::new);
		ParallelConveyor<String, SmartLabel<ScalarConvertingBuilder<String, ?>>, User>
		conveyor = new ParallelConveyor<>(new ScalarConvertingConveyor[]{ac1,ac2,ac3,ac3});
		
		AtomicReference<User> usr = new AtomicReference<User>(null);
		conveyor.setResultConsumer(u->{
			System.out.println("RESULT: "+u);
			usr.set(u.product);
		});

		ScalarCart<String, String> c1 = new ScalarCart<>("1", "John,Dow,1990");
		ScalarCart<String, String> c2 = new ScalarCart<>("2", "Jane,Dow,1990");
		ScalarCart<String, String> c3 = new ScalarCart<>("3", "Piter,Pan,1890");
		ScalarCart<String, String> c4 = new ScalarCart<>("4", "John,Silvers,1890");
		conveyor.add(c1);
		conveyor.add(c2);
		conveyor.add(c3);
		conveyor.add(c4);
		Thread.sleep(20);
		assertNotNull(usr.get());
		assertFalse(conveyor.isLBalanced());

	}
	
}
