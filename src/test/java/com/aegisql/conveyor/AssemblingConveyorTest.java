/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;

// TODO: Auto-generated Javadoc
/**
 * The Class AssemblingConveyorTest.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class AssemblingConveyorTest {

	/** The out queue. */
	Queue<User> outQueue = new ConcurrentLinkedQueue<>();
	
	/**
	 * Sets the up before class.
	 *
	 * @throws Exception the exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * Tear down after class.
	 *
	 * @throws Exception the exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
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
	
	/**
	 * Test unconfigured builder supplier.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void testUnconfiguredBuilderSupplier() throws InterruptedException, ExecutionException {
		AtomicBoolean visited = new AtomicBoolean(false);
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Cart Processor Failed"));
			assertTrue(o.scrap instanceof Cart);
			visited.set(true);
		});
		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		CompletableFuture<Boolean> f = conveyor.place(c1);
		assertFalse(f.isCompletedExceptionally());
		Thread.sleep(100);
		assertTrue(f.isCompletedExceptionally());
		assertTrue(visited.get());
	}

	/**
	 * Test offer stopped.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void testOfferStopped() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Conveyor is not running"));
			assertTrue(o.scrap instanceof Cart);
		});
		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		conveyor.stop();
		assertTrue(conveyor.place(c1).isCompletedExceptionally());
	}

	/**
	 * Test command stopped.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test(expected=IllegalStateException.class)
	public void testCommandStopped() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Conveyor is not running"));
			assertTrue(o.scrap instanceof Cart);
		});
		GeneralCommand<Integer,String> c1 = new GeneralCommand<>(1, "", CommandLabel.TIMEOUT_BUILD,0L);
		conveyor.stop();
		conveyor.placeCommand(c1);
	}

	/**
	 * Test command expired.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test(expected=IllegalStateException.class)
	public void testCommandExpired() throws InterruptedException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Command has already expired"));
			assertTrue(o.scrap instanceof Cart);
		});
		GeneralCommand<Integer,?> c1 = new GeneralCommand<>(1,"",CommandLabel.TIMEOUT_BUILD,1,TimeUnit.MILLISECONDS);
		Thread.sleep(10);
		conveyor.placeCommand(c1);
	}

	/**
	 * Test command too old.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test(expected=IllegalStateException.class)
	public void testCommandTooOld() throws InterruptedException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.rejectUnexpireableCartsOlderThan(10, TimeUnit.MILLISECONDS);
		conveyor.setScrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Command is too old"));
			assertTrue(o.scrap instanceof Cart);
		});
		GeneralCommand<Integer,?> c1 = new GeneralCommand<>(1,"",CommandLabel.TIMEOUT_BUILD,100,TimeUnit.MILLISECONDS);
		Thread.sleep(20);
		conveyor.placeCommand(c1);
	}

	/**
	 * Test add stopped.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException 
	 */
	@Test(expected=ExecutionException.class)
	public void testAddStopped() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Conveyor is not running"));
			assertTrue(o.scrap instanceof Cart);
		});
		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		conveyor.stop();
		conveyor.place(c1).get();
	}

	/**
	 * Test null cart content stopped.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException 
	 */
	@Test(expected=ExecutionException.class)
	public void testNullCartContentStopped() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("#####"));
			assertTrue(o.scrap instanceof Cart);
		});
		conveyor.addCartBeforePlacementValidator(cart->{
			if(cart.getValue()==null) {
				throw new NullPointerException("#####");
			}
		});
		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, null, "setFirst");
		conveyor.place(c1).get();
	}

	/**
	 * Test add expired stopped.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException 
	 */
	@Test(expected=ExecutionException.class)
	public void testAddExpiredStopped() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Cart has already expired"));
			assertTrue(o.scrap instanceof Cart);
		});
		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst",1,TimeUnit.MILLISECONDS);
		Thread.sleep(10);
		conveyor.place(c1).get();
	}

	/**
	 * Test offer expired stopped.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test()
	public void testOfferExpiredStopped() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Cart has already expired"));
			assertTrue(o.scrap instanceof Cart);
		});
		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst",1,TimeUnit.MILLISECONDS);
		Thread.sleep(10);
		assertTrue(conveyor.place(c1).isCompletedExceptionally());

	
		Cart<Integer, String, String> c2 = new ShoppingCart<>(1, "John", "setFirst",System.currentTimeMillis()-1);
		assertTrue(conveyor.place(c2).isCompletedExceptionally());

		assertTrue(conveyor.part().id(1).value("John").label("Silver").expirationTime(1).place().isCompletedExceptionally());
		
	}

	/**
	 * Test basics.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testSimpleConveyor() throws InterruptedException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(()->{
			System.out.println("Default builder supplier called.");
			return new UserBuilder();});
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		assertEquals(1000, conveyor.getDefaultBuilderTimeout());
		conveyor.setIdleHeartBeat(500, TimeUnit.MILLISECONDS);
		assertEquals(500,conveyor.getExpirationCollectionIdleInterval());
		conveyor.setDefaultCartConsumer((label, value, builder) -> {
			UserBuilder userBuilder = (UserBuilder) builder;
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
		});
		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		
		List<Integer> evictedKeys = new ArrayList<>();
		conveyor.addBeforeKeyEvictionAction(key->{evictedKeys.add(key);});
		conveyor.addBeforeKeyEvictionAction(key->{System.out.println("REMOVING! "+key);});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = c1.nextCart("Doe", "setLast");
		Cart<Integer, String, String> c3 = new ShoppingCart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = c1.nextCart(1999, "setYearOfBirth");

		Cart<Integer, Integer, String> c5 = new ShoppingCart<>(3, 1999, "setBlah");

		Cart<Integer, String, String> c6 = new ShoppingCart<>(6, "Ann", "setFirst");
		Cart<Integer, String, String> c7 = new ShoppingCart<>(7, "Nik", "setLast", 1, TimeUnit.HOURS);

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);
		conveyor.place(c6);
		Thread.sleep(100);
		conveyor.setIdleHeartBeat(1000, TimeUnit.MILLISECONDS);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		conveyor.place(c7);
		conveyor.command().id(8).ttl(1,TimeUnit.SECONDS).create();
		conveyor.command().id(9).ttl(1,TimeUnit.SECONDS).create(()->{
			System.out.println("Command builder supplier called.");
			return new UserBuilder();});
		Thread.sleep(100);
		conveyor.command().id(6).cancel();
		conveyor.command().id(7).cancel();

		conveyor.place(c5);
		Thread.sleep(2000);
		System.out.println("COL:"+conveyor.getCollectorSize());
		System.out.println("DEL:"+conveyor.getDelayedQueueSize());
		System.out.println("IN :"+conveyor.getInputQueueSize());
		conveyor.stop();
		Thread.sleep(1000);
		assertTrue(evictedKeys.size() > 0);
		System.out.println("Evicted :"+evictedKeys);
	}

	/**
	 * Test simple conveyor created by message.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testSimpleConveyorCreatedByMessage() throws InterruptedException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		conveyor.setIdleHeartBeat(500, TimeUnit.MILLISECONDS);
		conveyor.setDefaultCartConsumer((label, value, builder) -> {
			UserBuilder userBuilder = (UserBuilder) builder;
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
		});
		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = c1.nextCart("Doe", "setLast");
		Cart<Integer, Integer, String> c3 = c1.nextCart(1999, "setYearOfBirth");

		conveyor.command().id(1).ttl(1,TimeUnit.SECONDS).create(UserBuilder::new);
		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		conveyor.place(c3);
		Thread.sleep(100);
		conveyor.setIdleHeartBeat(1000, TimeUnit.MILLISECONDS);
		Thread.sleep(1100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		Thread.sleep(100);
		System.out.println("COL:"+conveyor.getCollectorSize());
		System.out.println("DEL:"+conveyor.getDelayedQueueSize());
		System.out.println("IN :"+conveyor.getInputQueueSize());
		conveyor.stop();
		Thread.sleep(1000);
	}

	/**
	 * Test simple conveyor not created by message.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testSimpleConveyorNotCreatedByMessage() throws InterruptedException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(null);
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		conveyor.setIdleHeartBeat(500, TimeUnit.MILLISECONDS);
		conveyor.setDefaultCartConsumer((label, value, builder) -> {
			UserBuilder userBuilder = (UserBuilder) builder;
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
		});
		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = c1.nextCart("Doe", "setLast");
		Cart<Integer, Integer, String> c3 = c1.nextCart(1999, "setYearOfBirth");

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		conveyor.place(c3);
		Thread.sleep(100);
		conveyor.setIdleHeartBeat(1000, TimeUnit.MILLISECONDS);
		User u2 = outQueue.poll();
		assertNull(u2);
		Thread.sleep(100);
		System.out.println("COL:"+conveyor.getCollectorSize());
		System.out.println("DEL:"+conveyor.getDelayedQueueSize());
		System.out.println("IN :"+conveyor.getInputQueueSize());
		conveyor.stop();
		Thread.sleep(1000);
	}
	
	
	/**
	 * Test basics.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testSimpleConveyor2() throws InterruptedException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilder::new);
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		assertEquals(1000, conveyor.getDefaultBuilderTimeout());
		conveyor.setIdleHeartBeat(500, TimeUnit.MILLISECONDS);
		assertEquals(500,conveyor.getExpirationCollectionIdleInterval());
		conveyor.setDefaultCartConsumer((label, value, builder) -> {
			UserBuilder userBuilder = (UserBuilder) builder;
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
		});
		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setReadinessEvaluator((builder) -> {
			UserBuilder ub = (UserBuilder)builder;
			return ub.getFirst() != null && ub.getLast() != null && ub.getYearOfBirth() != null;
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = c1.nextCart("Doe", "setLast");
		Cart<Integer, String, String> c3 = new ShoppingCart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = c1.nextCart(1999, "setYearOfBirth");

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);
		Thread.sleep(100);
		conveyor.setIdleHeartBeat(1000, TimeUnit.MILLISECONDS);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		Thread.sleep(100);
		conveyor.command().id(6).cancel();
		conveyor.command().id(7).cancel();

		Thread.sleep(2000);
		System.out.println("COL:"+conveyor.getCollectorSize());
		System.out.println("DEL:"+conveyor.getDelayedQueueSize());
		System.out.println("IN :"+conveyor.getInputQueueSize());
		conveyor.stop();
		Thread.sleep(1000);
	}
	
	/**
	 * Test simple conveyor blocking.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test(expected=ExecutionException.class)
	public void testSimpleConveyorBlocking() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>( ()->new ArrayBlockingQueue(3) );
		conveyor.setBuilderSupplier(UserBuilder::new);
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		assertEquals(1000, conveyor.getDefaultBuilderTimeout());
		conveyor.setIdleHeartBeat(500, TimeUnit.MILLISECONDS);
		assertEquals(500,conveyor.getExpirationCollectionIdleInterval());
		conveyor.setDefaultCartConsumer((label, value, builder) -> {
			UserBuilder userBuilder = (UserBuilder) builder;
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		});
		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setReadinessEvaluator((builder) -> {
			UserBuilder ub = (UserBuilder)builder;
			return ub.getFirst() != null && ub.getLast() != null && ub.getYearOfBirth() != null;
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = c1.nextCart("Doe", "setLast");
		Cart<Integer, String, String> c3 = new ShoppingCart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = c1.nextCart(1999, "setYearOfBirth");

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);
		CompletableFuture<Boolean> f4 = conveyor.place(c4);
		Boolean res = f4.get();
		System.out.println("Unexpected: "+res);
	}

	
	
	/**
	 * Test error.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testError() throws InterruptedException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilder::new);
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		assertEquals(1000, conveyor.getDefaultBuilderTimeout());
		conveyor.setIdleHeartBeat(500, TimeUnit.MILLISECONDS);
		assertEquals(500,conveyor.getExpirationCollectionIdleInterval());
		conveyor.setDefaultCartConsumer((label, value, builder) -> {
			throw new Error("TEST ERROR");
		});
		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setScrapConsumer((o)->{
			System.out.println(o);
//			assertTrue(ex.startsWith("Cart expired"));
//			assertTrue(o instanceof Cart);
		});

		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		assertTrue(conveyor.isRunning());
		conveyor.place(c1);
		Thread.sleep(100);
		assertFalse(conveyor.isRunning());
	}

	
	/**
	 * Test delayed.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testDelayed() throws InterruptedException {
		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "A", "setFirst",1,TimeUnit.SECONDS);
		Cart<Integer, String, String> c2 = new ShoppingCart<>(1, "B", "setLast",c1.getExpirationTime());

		assertFalse(c1.expired());
		assertFalse(c2.expired());
		System.out.println(c1);
		System.out.println(c2);
		assertEquals(1000, c1.getExpirationTime() - c1.getCreationTime());
		System.out.println(c1.toDelayed().getDelay(TimeUnit.MILLISECONDS));
		
		BlockingQueue q = new DelayQueue();
		q.add(c1.toDelayed());
		assertNull(q.poll());
		Thread.sleep(1000);
		assertNotNull(q.poll());
		
	}

	/**
	 * The Class MyDelayed.
	 */
	public static class MyDelayed implements Delayed {
		
		/** The delay. */
		public long delay = 1;
		
		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Delayed o) {
			return 0;
		}
		
		/* (non-Javadoc)
		 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
		 */
		public long getDelay(TimeUnit unit) {
			return delay;
		}
		
	}
	
	/**
	 * Test managed delayed.
	 */
	@Test
	public void testManagedDelayed() {
		MyDelayed d1 = new MyDelayed();
		MyDelayed d2 = new MyDelayed();
		BlockingQueue<MyDelayed> q = new DelayQueue<>();
		q.add(d1);
		q.add(d2);
		
		assertNull(q.poll());
		d1.delay = -1;
		assertNotNull(q.poll());
		assertNull(q.poll());
		d2.delay = -1;
		assertNotNull(q.poll());
		assertNull(q.poll());
		
	}
	
}
