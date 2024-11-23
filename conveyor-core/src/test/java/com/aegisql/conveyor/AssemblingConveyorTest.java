/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import com.aegisql.conveyor.loaders.*;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

// TODO: Auto-generated Javadoc

/**
 * The Class AssemblingConveyorTest.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class AssemblingConveyorTest {


	/**
	 * Sets the up before class.
	 *
	 */
	@BeforeAll
	public static void setUpBeforeClass() {
	}

	/**
	 * Tear down after class.
	 *
	 */
	@AfterAll
	public static void tearDownAfterClass() {
	}

	/**
	 * Sets the up.
	 *
	 */
	@BeforeEach
	public void setUp() {
	}

	/**
	 * Tear down.
	 *
	 */
	@AfterEach
	public void tearDown() {
	}

	/**
	 * Test unconfigured builder supplier.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testUnconfiguredBuilderSupplier() throws InterruptedException {
		AtomicBoolean visited = new AtomicBoolean(false);
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setIdleHeartBeat(Duration.ofMillis(99));
		conveyor.scrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Cart Processor Failed"));
			assertTrue(o.scrap instanceof Cart);
			visited.set(true);
		}).set();
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
	 */
	@Test
	public void testOfferStopped() {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setName("a");
		conveyor.scrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Conveyor a is not running"));
			assertTrue(o.scrap instanceof Cart);
		}).set();
		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		conveyor.stop();
		assertTrue(conveyor.place(c1).isCompletedExceptionally());
	}

	/**
	 * Test command stopped.
	 *
	 */
	@Test
	public void testCommandStopped() {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setName("a");
		conveyor.scrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Conveyor a is not running"));
			assertTrue(o.scrap instanceof Cart);
		}).set();
		GeneralCommand<Integer,String> c1 = new GeneralCommand<>(1, "", CommandLabel.TIMEOUT_BUILD,0L);
		conveyor.stop();
		assertThrows(IllegalStateException.class,()->conveyor.command(c1));
	}

	/**
	 * Test command expired.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testCommandExpired() throws InterruptedException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.scrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Command has already expired"));
			assertTrue(o.scrap instanceof Cart);
		}).set();
		CommandLoader<Integer, User> cl = conveyor.command().id(1).ttl(1,TimeUnit.MILLISECONDS);
		Thread.sleep(10);
		assertThrows(IllegalStateException.class,()->cl.timeout());
	}

	/**
	 * Test command too old.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testCommandTooOld() throws InterruptedException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.rejectUnexpireableCartsOlderThan(Duration.ofMillis(10));
		conveyor.scrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Command is too old"));
			assertTrue(o.scrap instanceof Cart);
		}).set();
		
		GeneralCommand<Integer,?> c1 = new GeneralCommand<>(1,"",CommandLabel.TIMEOUT_BUILD,System.currentTimeMillis()+100);
		Thread.sleep(20);
		assertThrows(IllegalStateException.class,()->conveyor.command(c1));
	}

	/**
	 * Test add stopped.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testAddStopped() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setName("a");
		conveyor.scrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Conveyor a is not running"));
			assertTrue(o.scrap instanceof Cart);
		}).set();
		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		conveyor.stop();
		assertThrows(ExecutionException.class,()->conveyor.place(c1).get());
	}

	/**
	 * Test null cart content stopped.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testNullCartContentStopped() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.scrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("#####"));
			assertTrue(o.scrap instanceof Cart);
		}).set();
		conveyor.addCartBeforePlacementValidator(cart->{
			if(cart.getValue()==null) {
				throw new NullPointerException("#####");
			}
		});
		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, null, "setFirst");
		assertThrows(ExecutionException.class,()->conveyor.place(c1).get());
	}

	/**
	 * Test add expired stopped.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testAddExpiredStopped() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.scrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Cart has already expired"));
			assertTrue(o.scrap instanceof Cart);
		}).set();
		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst",1,TimeUnit.MILLISECONDS);
		Thread.sleep(10);
		assertThrows(ExecutionException.class,()->conveyor.place(c1).get());
	}

	/**
	 * Test offer expired stopped.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test()
	public void testOfferExpiredStopped() throws InterruptedException {
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.scrapConsumer((o)->{
			System.out.println(o);
			assertTrue(o.comment.startsWith("Cart has already expired"));
			assertTrue(o.scrap instanceof Cart);
		}).set();
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
		/* The out queue. */
		ResultQueue<Integer,User> outQueue = ResultQueue.of(conveyor);
		conveyor.resultConsumer().first(outQueue).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		
		List<Integer> evictedKeys = new ArrayList<>();
		conveyor.addBeforeKeyEvictionAction(status->{evictedKeys.add(status.getKey());});
		conveyor.addBeforeKeyEvictionAction(status->{System.out.println("REMOVING! "+status.getKey());});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = new ShoppingCart<>(1,"Doe", "setLast");
		Cart<Integer, String, String> c3 = new ShoppingCart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = new ShoppingCart<>(1,1999, "setYearOfBirth");

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
		conveyor.command().id(7).cancel(new RuntimeException("CANCELED EXCEPTION"));

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
		ResultQueue<Integer,User> outQueue = ResultQueue.of(conveyor);
		conveyor.resultConsumer().first(outQueue).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = new ShoppingCart<>(1,"Doe", "setLast");
		Cart<Integer, Integer, String> c3 = new ShoppingCart<>(1,1999, "setYearOfBirth");

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
		ResultQueue<Integer,User> outQueue = ResultQueue.of(conveyor);
		conveyor.resultConsumer().first(outQueue).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = new ShoppingCart<>(1,"Doe", "setLast");
		Cart<Integer, Integer, String> c3 = new ShoppingCart<>(1,1999, "setYearOfBirth");

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
		ResultQueue<Integer,User> outQueue = ResultQueue.of(conveyor);
		conveyor.resultConsumer().first(outQueue).set();
		conveyor.setReadinessEvaluator((builder) -> {
			UserBuilder ub = (UserBuilder)builder;
			return ub.getFirst() != null && ub.getLast() != null && ub.getYearOfBirth() != null;
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = new ShoppingCart<>(1,"Doe", "setLast");
		Cart<Integer, String, String> c3 = new ShoppingCart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = new ShoppingCart<>(1,1999, "setYearOfBirth");

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
	 * @throws ExecutionException   the execution exception
	 */
	@Test
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
		ResultQueue<Integer,User> outQueue = ResultQueue.of(conveyor);
		conveyor.resultConsumer().first(outQueue).set();
		conveyor.setReadinessEvaluator((builder) -> {
			UserBuilder ub = (UserBuilder)builder;
			return ub.getFirst() != null && ub.getLast() != null && ub.getYearOfBirth() != null;
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = new ShoppingCart<>(1,"Doe", "setLast");
		Cart<Integer, String, String> c3 = new ShoppingCart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = new ShoppingCart<>(1,1999, "setYearOfBirth");

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);
		CompletableFuture<Boolean> f4 = conveyor.place(c4);
		assertThrows(ExecutionException.class,()->f4.get());
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
		ResultQueue<Integer,User> outQueue = ResultQueue.of(conveyor);
		conveyor.resultConsumer().first(outQueue).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.scrapConsumer(LogScrap.error(conveyor)).set();

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

		/**
		 * The delay.
		 */
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

	/**
	 * Test access by name.
	 */
	@Test
	public void testAccessByName() {
		AssemblingConveyor<Integer,String,User> ac1 = new AssemblingConveyor<>();
		
		ac1.setName("test_name");
		
		Conveyor<Integer,String,User> ac2 = Conveyor.byName("test_name");
		
		Conveyor<Integer,String,User> ac3 = Conveyor.lazySupplier("test_name").get();
		assertNotNull(ac2);
		assertNotNull(ac3);
		assertTrue(ac1==ac2);
		assertTrue(ac1==ac3);
		
		assertNotNull(PartLoader.byConveyorName("test_name"));
		assertNotNull(PartLoader.lazySupplier("test_name"));
		assertNotNull(PartLoader.lazySupplier("test_name").get());
		assertTrue(PartLoader.lazySupplier("test_name").get() instanceof PartLoader);

		assertNotNull(StaticPartLoader.byConveyorName("test_name"));
		assertNotNull(StaticPartLoader.lazySupplier("test_name"));
		assertNotNull(StaticPartLoader.lazySupplier("test_name").get());
		assertTrue(StaticPartLoader.lazySupplier("test_name").get() instanceof StaticPartLoader);

		assertNotNull(BuilderLoader.byConveyorName("test_name"));
		assertNotNull(BuilderLoader.lazySupplier("test_name"));
		assertNotNull(BuilderLoader.lazySupplier("test_name").get());
		assertTrue(BuilderLoader.lazySupplier("test_name").get() instanceof BuilderLoader);

		assertNotNull(CommandLoader.byConveyorName("test_name"));
		assertNotNull(CommandLoader.lazySupplier("test_name"));
		assertNotNull(CommandLoader.lazySupplier("test_name").get());
		assertTrue(CommandLoader.lazySupplier("test_name").get() instanceof CommandLoader);

		assertNotNull(FutureLoader.byConveyorName("test_name"));
		assertNotNull(FutureLoader.lazySupplier("test_name"));
		assertNotNull(FutureLoader.lazySupplier("test_name").get());
		assertTrue(FutureLoader.lazySupplier("test_name").get() instanceof FutureLoader);

		assertNotNull(ResultConsumerLoader.byConveyorName("test_name"));
		assertNotNull(ResultConsumerLoader.lazySupplier("test_name"));
		assertNotNull(ResultConsumerLoader.lazySupplier("test_name").get());
		assertTrue(ResultConsumerLoader.lazySupplier("test_name").get() instanceof ResultConsumerLoader);

		assertNotNull(ScrapConsumerLoader.byConveyorName("test_name"));
		assertNotNull(ScrapConsumerLoader.lazySupplier("test_name"));
		assertNotNull(ScrapConsumerLoader.lazySupplier("test_name").get());
		assertTrue(ScrapConsumerLoader.lazySupplier("test_name").get() instanceof ScrapConsumerLoader);

		PartLoader<Integer, String> pl = PartLoader.byConveyorName("test_name");
		PartLoader<Integer, String> pl2 = PartLoader.<Integer, String>lazySupplier("test_name").get();
		pl.id(1).label("label").value("value").place();
		pl2.id(2).label("label").value("value2").place();

		System.out.println("Known Names: "+Conveyor.getKnownConveyorNames());
		System.out.println("Loaded Names: "+Conveyor.getLoadedConveyorNames());

	}

	/**
	 * Test unregister access by name.
	 */
	@Test
	public void testUnregisterAccessByName() {
		AssemblingConveyor<Integer,String,User> ac1 = new AssemblingConveyor<>();

		Supplier<Conveyor> acs = Conveyor.lazySupplier("test_name_2");

		ac1.setName("test_name_2");
		
		Conveyor<Integer,String,User> ac2 = Conveyor.byName("test_name_2");
		Conveyor<Integer,String,User> ac3 = acs.get();
		
		assertTrue(ac1==ac2);
		assertTrue(ac1==ac3);
		
		Conveyor.unRegister("test_name_2");
		
		assertNotNull(acs.get());
		
		assertThrows(RuntimeException.class,()->Conveyor.byName("test_name_2"));
	}


	/**
	 * Test access by wrong name.
	 */
	@Test
	public void testAccessByWrongName() {
		AssemblingConveyor<Integer,String,User> ac1 = new AssemblingConveyor<>();
		
		ac1.setName("test_name");
		
		assertThrows(RuntimeException.class,()->Conveyor.byName("bad_name"));

	}

	@Test
	public void unimplementedMetadataExceptionTest() {
		AssemblingConveyor<Integer,String,User> ac1 = new AssemblingConveyor<>();
		assertThrows(ConveyorRuntimeException.class,()->ac1.getMetaInfo());
	}

	@Test
	public void autoShutdownTest() throws InterruptedException {
		AssemblingConveyor<Integer,String,User> ac1 = new AssemblingConveyor<>();
		ac1.setAutoShutDownTime(Duration.ofSeconds(3));
		Thread.sleep(4000);
		assertThrows(CompletionException.class,()->ac1.part().id(1).label("label").value("value").place().join());
	}

	@Test
	public void noAutoShutdownTest() throws InterruptedException {
		AssemblingConveyor<Integer,String,User> ac1 = new AssemblingConveyor<>();
		ac1.setBuilderSupplier(UserBuilder::new);
		ac1.setDefaultCartConsumer((a,b,c)->{});
		ac1.setReadinessEvaluator(Conveyor.getTesterFor(ac1).accepted("done"));
		ac1.resultConsumer(LogResult.debug(ac1)).set();
		ac1.setAutoShutDownTime(Duration.ofSeconds(3));
		ac1.part().id(1).label("label").value("value").place();
		Thread.sleep(4000);
		assertDoesNotThrow(()->ac1.part().id(1).label("done").value("value").place().join());
		Thread.sleep(4000);
		assertThrows(CompletionException.class,()->ac1.part().id(1).label("label").value("value").place().join());
	}

}
