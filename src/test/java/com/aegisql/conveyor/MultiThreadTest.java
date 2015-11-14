package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;

public class MultiThreadTest {
	
	public static int SIZE = 1000;
	
	public static User[] inUser  = new User[SIZE];
	public static User[] outUser = new User[SIZE];

	public static 		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> 
	conveyor = new AssemblingConveyor<>();

	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		conveyor.setBuilderSupplier( UserBuilder::new );
	    conveyor.setCartConsumer( (label, value, builder) -> {
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
	    conveyor.setReadinessEvaluator( (lot, builder) -> {
		UserBuilder userBuilder = (UserBuilder) builder;
		return lot.previouslyAccepted == 3 || userBuilder.ready();
	    });
   
	    conveyor.setName("Multithread User Builder");
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		conveyor.stop();
		Thread.sleep(100);
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	public static Queue<User> outQueue = new ConcurrentLinkedQueue<>();

	
	
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
	
	@Test(expected=NullPointerException.class)
	public void testException() {
		conveyor.add(null);
	}
	
	@Test
	public void test() throws InterruptedException {

	conveyor.setExpirationCollectionInterval(1000, TimeUnit.MILLISECONDS);
	conveyor.setBuilderTimeout(1, TimeUnit.SECONDS);
	assertFalse(conveyor.isOnTimeoutAction());
	conveyor.setOnTimeoutAction(true);
	assertTrue(conveyor.isOnTimeoutAction());
	conveyor.setResultConsumer( res->{
		    	outQueue.add(res);
		    });
		
	Thread runFirst = new Thread(()->{
		Random r = new Random();
		int[] ids = getRandomInts();
		for(int i = 0; i < SIZE; i++) {
			User u = inUser[ids[i]];
			Cart<Integer, String, String> cart = new Cart<>(ids[i], "First_"+ids[i], "setFirst",1,TimeUnit.SECONDS);
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
			Cart<Integer, String, String> cart = new Cart<>(ids[i], "Last_"+ids[i], "setLast",1,TimeUnit.SECONDS);
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
			Cart<Integer, Integer, String> cart = new Cart<>(ids[i], 1900+r.nextInt(100), "setYearOfBirth",1,TimeUnit.SECONDS);
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
	

	System.out.println("Left: "+conveyor.getCollectorSize());
	Thread.sleep(3000);

	System.out.println("Incomplete data");
	while(!outQueue.isEmpty()) {
		System.out.println(outQueue.poll());
	}

	assertEquals(0,conveyor.getInputQueueSize());
	assertEquals(0,conveyor.getCollectorSize());
	assertEquals(0,conveyor.getDelayedQueueSize());

	}

}
