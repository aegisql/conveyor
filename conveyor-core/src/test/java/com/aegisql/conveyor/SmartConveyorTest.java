/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.result.ResultMap;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.user.*;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;


/**
 * The Class SmartConveyorTest.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class SmartConveyorTest {


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
	 * Test basics smart.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testBasicsSmart() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		Conveyor.getTesterFor(conveyor).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR).set();
		conveyor.setName("testBasicsSmart");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE);
		Cart<Integer, Integer, UserBuilderEvents> c4 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.part().foreach().value("BEFORE").label(UserBuilderEvents.PRINT).place();

		conveyor.place(c4);
		
		
		Thread.sleep(100);
		conveyor.part().foreach().value("AFTER").label(UserBuilderEvents.PRINT).place();
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		Thread.sleep(100);

		conveyor.stop();
	}

	/**
	 * Test reschedule smart.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testRescheduleSmart() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		/** The out queue. */
		ResultMap<Integer,User> outMap = new ResultMap<>();

		conveyor.resultConsumer().first(outMap).andThen(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			System.out.println(state);
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("testRescheduleSmart");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST,
				5, TimeUnit.SECONDS);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1, 1999, UserBuilderEvents.SET_YEAR);

		conveyor.place(c1);
		conveyor.place(c2);
		User u0 = outMap.get(1);
		assertNull(u0);
		conveyor.command().id(1).ttl(10, TimeUnit.SECONDS).reschedule();
		Thread.sleep(5001);
		conveyor.place(c3).get();
		User u1 = outMap.get(1);
		assertNotNull(u1);
		System.out.println(outMap);

		conveyor.completeAndStop().get();
	}

	/**
	 * Test basics testing.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testBasicsTesting() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents2, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTesting::new);

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setName("testBasicsTesting");
		ShoppingCart<Integer, String, UserBuilderEvents2> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents2.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents2> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents2.SET_LAST);
		Cart<Integer, String, UserBuilderEvents2> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents2.SET_FIRST);
		Cart<Integer, Integer, UserBuilderEvents2> c4 = new ShoppingCart<>(1,1999, UserBuilderEvents2.SET_YEAR);

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);
		Thread.sleep(100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		Thread.sleep(100);

		conveyor.stop();
	}

	/**
	 * Test basics testing with internal offer interfaces.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testBasicsTestingWithInternalOfferInterfaces() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents2, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTesting::new);

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setName("testBasicsTestingWithInternalOfferInterfaces");
		conveyor.part().id(1).value("John").label(UserBuilderEvents2.SET_FIRST).place();
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.part().id(1).value("Doe").label(UserBuilderEvents2.SET_LAST).expirationTime(System.currentTimeMillis() + 10).place();
		conveyor.part().id(2).value("Mike").label(UserBuilderEvents2.SET_FIRST).ttl(10, TimeUnit.MILLISECONDS).place();
		conveyor.part().id(1).value(1999).label(UserBuilderEvents2.SET_YEAR).place();
		Thread.sleep(100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		Thread.sleep(100);

		conveyor.stop();
	}

	/**
	 * Test basics testing with internal add interfaces.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testBasicsTestingWithInternalAddInterfaces() throws InterruptedException {
		
		AtomicReference<String> ref = new AtomicReference<String>();
		AssemblingConveyor<Integer, UserBuilderEvents2, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTesting::new);

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setName("testBasicsTestingWithInternalAddInterfaces");
		conveyor.part().value("John").id(1).label(UserBuilderEvents2.SET_FIRST).place();
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.part().id(1).value("Doe").label(UserBuilderEvents2.SET_LAST).ttl(10, TimeUnit.MILLISECONDS).place();
		conveyor.part().id(2).value("Mike").label(UserBuilderEvents2.SET_FIRST).expirationTime(System.currentTimeMillis() + 10).place();
		conveyor.resultConsumer().id(1).addProperty("B", "Y").andThen(bin->{
			System.out.println("FINAL: "+bin.properties);
			assertEquals(2,bin.properties.size());
			assertTrue(bin.properties.containsKey("A"));
			assertTrue(bin.properties.containsKey("B"));
		}).set();
		conveyor.part().value(1999).id(1).label(UserBuilderEvents2.SET_YEAR).addProperty("A","X").place();
		Thread.sleep(100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		Thread.sleep(100);

		conveyor.stop();
	}

	/**
	 * Test basics testing creating interface.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testBasicsTestingCreatingInterface() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents2, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(() -> {
			System.out.println("Default Supplier");
			return new UserBuilderTesting();
		});
	
		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
	
		BuilderSupplier<User> sup = () -> {
			System.out.println("Cart Supplier");
			return new UserBuilderTesting();
		};
	
		BuilderSupplier<User> sup2 = () -> {
			System.out.println("Cmd Supplier");
			return new UserBuilderTesting();
		};
	
		CompletableFuture<Boolean> cf1 = conveyor.build().id(1).supplier(sup).create();
		CompletableFuture<Boolean> cf2 = conveyor.build().id(2).supplier(sup).create();
		CompletableFuture<Boolean> cf3 = conveyor.build().id(3).create();
	
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get()); //default supplier;
		
		conveyor.command().id(4).create();
		conveyor.command().id(5).create(sup2);
	
		conveyor.setName("testBasicsTestingCreatingInterface");
		conveyor.part().value("John").id(1).label(UserBuilderEvents2.SET_FIRST).place();
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.part().id(1).value("Doe").label(UserBuilderEvents2.SET_LAST).ttl(10, TimeUnit.MILLISECONDS).place();
		conveyor.part().id(2).value("Mike").label(UserBuilderEvents2.SET_FIRST).expirationTime(System.currentTimeMillis() + 10).place();
		conveyor.part().value(1999).id(1).label(UserBuilderEvents2.SET_YEAR).place();
		Thread.sleep(100);
		conveyor.part().foreach().value("CREATING").label(UserBuilderEvents2.PRINT).place();
	
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
	
		Thread.sleep(100);
	
		conveyor.stop();
	}

	/**
	 * Test basics testing.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testBasicsTestingState() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents3, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTestingState::new);

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setName("testBasicsTestingState");
		ShoppingCart<Integer, String, UserBuilderEvents3> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents3.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents3> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents3.SET_LAST);
		Cart<Integer, String, UserBuilderEvents3> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents3.SET_FIRST);
		Cart<Integer, Integer, UserBuilderEvents3> c4 = new ShoppingCart<>(1,1999, UserBuilderEvents3.SET_YEAR);

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);
		Thread.sleep(100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		Thread.sleep(100);

		conveyor.stop();
	}

	/**
	 * Test rejected start offer.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testRejectedStartOffer() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 2;
		});
		PartLoader<Integer,UserBuilderEvents> pl = conveyor.part().id(1);
		conveyor.rejectUnexpireableCartsOlderThan(1, TimeUnit.SECONDS);
		assertTrue(pl.label(UserBuilderEvents.SET_FIRST).value("John").place().get());
		Thread.sleep(1100);
		CompletableFuture<Boolean> future = pl.label(UserBuilderEvents.SET_LAST).value("Doe").place();
		assertTrue(future.isCompletedExceptionally());
		conveyor.stop();
	}

	/**
	 * Test rejected start add.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testRejectedStartAdd() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 2;
		});
		conveyor.rejectUnexpireableCartsOlderThan(1, TimeUnit.SECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		assertTrue(conveyor.place(c1).get());
		Thread.sleep(1100);
		assertThrows(ExecutionException.class,()->conveyor.place(c2).get());
		conveyor.stop();
	}

	/**
	 * Test smart.
	 */
	@Test
	public void testSmart() {
		UserBuilderSmart ub = new UserBuilderSmart();

		UserBuilderEvents.SET_FIRST.get().accept(ub, "first");
		UserBuilderEvents.SET_LAST.get().accept(ub, "last");
		UserBuilderEvents.SET_YEAR.get().accept(ub, 1970);

		System.out.println(ub.get());

	}

	/**
	 * Test future smart.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 * @throws TimeoutException     the timeout exception
	 */
	@Test
	public void testFutureSmart() throws InterruptedException, ExecutionException, TimeoutException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("testFutureSmart");
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE);
		Cart<Integer, Integer, UserBuilderEvents> c4 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		
		CompletableFuture<User> f1 = conveyor.future().id(1).get();
		
		assertNotNull(f1);
		assertFalse(f1.isCancelled());
		assertFalse(f1.isCompletedExceptionally());
		assertFalse(f1.isDone());


		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);

		CompletableFuture<User> f2 = conveyor.future().id(2).get();
		assertNotNull(f2);
		assertFalse(f2.isCancelled());
		assertFalse(f2.isCompletedExceptionally());
		assertFalse(f2.isDone());

		conveyor.part().foreach().value("FUTURE").label(UserBuilderEvents.PRINT).place();

		User user1 = f1.get();
		assertNotNull(user1);
		System.out.println(user1);

		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		assertFalse(f1.isCancelled());
		assertFalse(f1.isCompletedExceptionally());
		assertTrue(f1.isDone());

		assertThrows(CancellationException.class,()->f2.get(200,TimeUnit.MILLISECONDS));
		conveyor.stop();
	}

	/**
	 * Test upper case.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testUpperCase() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, AbstractBuilderEvents, User> conveyor = new AssemblingConveyor<>();

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Upper Assembler");
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		
		CompletableFuture<User> f1 = conveyor.build().id(1).supplier(UpperCaseUserBuilder::new).createFuture();
		
		assertNotNull(f1);
		assertFalse(f1.isCancelled());
		assertFalse(f1.isCompletedExceptionally());
		assertFalse(f1.isDone());
		conveyor.part().id(1).value("John").label(AbstractBuilderEvents.SET_FIRST).place();
		conveyor.part().id(1).value("Doe").label(AbstractBuilderEvents.SET_LAST).place();
		conveyor.part().id(1).value(1999).label(AbstractBuilderEvents.SET_YEAR).place();

		User user1 = f1.get();
		assertNotNull(user1);
		System.out.println(user1);
		assertEquals(user1,new UpperUser("JOHN","DOE",1999));
		conveyor.stop();
	}

	/**
	 * Test lower case.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testLowerCase() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, AbstractBuilderEvents, User> conveyor = new AssemblingConveyor<>();

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Lower Assembler");
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		
		CompletableFuture<User> f1 = conveyor.build().id(1).supplier(LowerCaseUserBuilder::new).createFuture();
		
		assertNotNull(f1);
		assertFalse(f1.isCancelled());
		assertFalse(f1.isCompletedExceptionally());
		assertFalse(f1.isDone());

		conveyor.part().id(1).value("John").label(AbstractBuilderEvents.SET_FIRST).place();
		conveyor.part().id(1).value("Doe").label(AbstractBuilderEvents.SET_LAST).place();
		conveyor.part().id(1).value(1999).label(AbstractBuilderEvents.SET_YEAR).place();

		User user1 = f1.get();
		assertNotNull(user1);
		System.out.println(user1);
		assertEquals(user1,new LowerUser("john","doe",1999));
		conveyor.stop();
	}

	/**
	 * Test object.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testObject() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, AbstractBuilderEvents, Object> conveyor = new AssemblingConveyor<>();

		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("Object Assembler");
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		
		CompletableFuture<Object> f1 = conveyor.build().id(1).supplier(LowerCaseUserBuilder::new).createFuture();
		
		assertNotNull(f1);
		assertFalse(f1.isCancelled());
		assertFalse(f1.isCompletedExceptionally());
		assertFalse(f1.isDone());

		conveyor.part().id(1).value("John").label(AbstractBuilderEvents.SET_FIRST).place();
		conveyor.part().id(1).value("Doe").label(AbstractBuilderEvents.SET_LAST).place();
		conveyor.part().id(1).value(1999).label(AbstractBuilderEvents.SET_YEAR).place();

		User user1 = (User) f1.get();
		assertNotNull(user1);
		System.out.println(user1);
		assertEquals(user1,new LowerUser("john","doe",1999));
		conveyor.stop();
	}

	/**
	 * Test upper case with added expiration and testing.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testUpperCaseWithAddedExpirationAndTesting() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, AbstractBuilderEvents, User> conveyor = new AssemblingConveyor<>();

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setName("User Upper Assembler");
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		
		CompletableFuture<User> f1 = conveyor.build().id(1).supplier(
				BuilderSupplier.of(UpperCaseUserBuilder::new)
				.expire(100,TimeUnit.MILLISECONDS)
				.readyAlgorithm(new ReadinessTester().accepted(3))).createFuture();
		
		assertNotNull(f1);
		assertFalse(f1.isCancelled());
		assertFalse(f1.isCompletedExceptionally());
		assertFalse(f1.isDone());

		conveyor.part().id(1).value("John").label(AbstractBuilderEvents.SET_FIRST).place();
		conveyor.part().id(1).value("Doe").label(AbstractBuilderEvents.SET_LAST).place();
		conveyor.part().id(1).value(1999).label(AbstractBuilderEvents.SET_YEAR).place();

		User user1 = f1.get();
		assertNotNull(user1);
		System.out.println(user1);
		assertEquals(user1,new UpperUser("JOHN","DOE",1999));
		conveyor.stop();
	}

	/**
	 * Test failing upper case with added expiration and testing.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testFailingUpperCaseWithAddedExpirationAndTesting() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, AbstractBuilderEvents, User> conveyor = new AssemblingConveyor<>();

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setName("User Upper Assembler");
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		
		CompletableFuture<User> f1 = conveyor.build().id(1).supplier(
				BuilderSupplier.of(UpperCaseUserBuilder::new)
				.expire(100,TimeUnit.MILLISECONDS)
				.readyAlgorithm(new ReadinessTester<Integer, AbstractBuilderEvents, User>().accepted(4))
				.onTimeout((b)->{System.out.println("--- TIMEOUT--- "+b.get());})
				).createFuture();
		
		assertNotNull(f1);
		assertFalse(f1.isCancelled());
		assertFalse(f1.isCompletedExceptionally());
		assertFalse(f1.isDone());

		conveyor.part().id(1).value("John").label(AbstractBuilderEvents.SET_FIRST).place();
		conveyor.part().id(1).value("Doe").label(AbstractBuilderEvents.SET_LAST).place();
		conveyor.part().id(1).value(1999).label(AbstractBuilderEvents.SET_YEAR).place();

		assertThrows(CancellationException.class,()->f1.get());
		conveyor.stop();
	}

	/**
	 * Test static values.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testStaticValues() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, SmartLabel<UserBuilder>, User> c = new AssemblingConveyor<>();
		c.setName("testStaticValues");
		SmartLabel<UserBuilder> FIRST = SmartLabel.of(UserBuilder::setFirst);
		SmartLabel<UserBuilder> LAST = SmartLabel.of(UserBuilder::setLast);
		SmartLabel<UserBuilder> YEAR = SmartLabel.of(UserBuilder::setYearOfBirth);
		c.setBuilderSupplier(UserBuilder::new);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(FIRST,LAST,YEAR));
		c.staticPart().label(FIRST).value("Mr.").place();
		
		PartLoader<Integer, SmartLabel<UserBuilder>> plLast = c.part().label(LAST);
		PartLoader<Integer, SmartLabel<UserBuilder>> plDate = c.part().label(YEAR);
		
		Future<User> f1 = c.build().id(1).createFuture();
		plLast.id(1).value("Smith").place();
		plDate.id(1).value(1999).place();
		User u1 = f1.get();
		assertNotNull(u1);
		assertEquals("Mr.", u1.getFirst());
		System.out.println(u1);

		Future<User> f2 = c.build().id(2).createFuture();
		plLast.id(2).value("Johnson").place();
		plDate.id(2).value(1977).place();
		User u2 = f2.get();
		assertNotNull(u2);
		assertEquals("Mr.", u2.getFirst());
		System.out.println(u2);

		c.staticPart().label(FIRST).value("Ms.").place();
		
		Future<User> f3 = c.build().id(3).createFuture();
		plLast.id(3).value("Jane").place();
		plDate.id(3).value(2010).place();
		User u3 = f3.get();
		assertNotNull(u3);
		assertEquals("Ms.", u3.getFirst());
		System.out.println(u3);

		c.staticPart().label(FIRST).delete().place();

		Future<User> f4 = c.build().id(4).createFuture();
		c.part().id(4).label(FIRST).value("Lady").place();
		plLast.id(4).value("Jane").place();
		plDate.id(4).value(2010).place();
		User u4 = f4.get();
		assertNotNull(u4);
		assertEquals("Lady", u4.getFirst());
		System.out.println(u4);
		c.completeAndStop().get();
	}

	/**
	 * Test basics smart.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testCompleteAndStop() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		conveyor.setName("testCompleteAndStop");
		
		for(int i = 0; i<100;i++) {
			PartLoader pl = conveyor.part().id(i);
			pl.label(UserBuilderEvents.SET_FIRST).value("First_"+i).place();
			pl.label(UserBuilderEvents.SET_LAST).value("Last_"+i).place();
			pl.label(UserBuilderEvents.SET_YEAR).value(1900+i).place(); 
		}
		
		Future<?> f = conveyor.completeAndStop();
		f.get();
		assertEquals(100, outQueue.size());
		conveyor.completeAndStop().get();
	}

	/**
	 * Test complete and stop with expiration.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testCompleteAndStopWithExpiration() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setDefaultBuilderTimeout(Duration.ofMillis(100));
		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		conveyor.setName("testCompleteAndStopWithExpiration");
		
		for(int i = 0; i<100;i++) {
			PartLoader pl = conveyor.part().id(i);
			pl.label(UserBuilderEvents.SET_FIRST).value("First_"+i).place();
			pl.label(UserBuilderEvents.SET_LAST).value("Last_"+i).place();
			if(i%2==0) {
				pl.label(UserBuilderEvents.SET_YEAR).value(1900+i).place(); 
			}
		}
		
		Future<?> f = conveyor.completeAndStop();
		f.get();
		assertEquals(50, outQueue.size());
		conveyor.completeAndStop().get();
	}


	/**
	 * Test basics smart.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testCompleteAndStopRejectMessages() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		conveyor.setName("testCompleteAndStopRejectMessages");
		
		for(int i = 0; i<1000;i++) {
			PartLoader pl = conveyor.part().id(i);
			pl.label(UserBuilderEvents.SET_FIRST).value("First_"+i).place();
			pl.label(UserBuilderEvents.SET_LAST).value("Last_"+i).place();
			pl.label(UserBuilderEvents.SET_YEAR).value(1900+i).place(); 
		}
		
		Future<?> f = conveyor.completeAndStop();
		PartLoader pl = conveyor.part().id(101);
		Future<Boolean> cf = pl.label(UserBuilderEvents.SET_FIRST).value("First_"+101).place();
		System.err.println(cf);
		assertTrue(cf.isDone());
		assertThrows(ExecutionException.class,()->cf.get());
		conveyor.completeAndStop().get();
	}


	/**
	 * Test complete and stop reject command.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testCompleteAndStopRejectCommand() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		conveyor.setName("testCompleteAndStopRejectCommand");
		
		for(int i = 0; i<1000;i++) {
			PartLoader pl = conveyor.part().id(i);
			pl.label(UserBuilderEvents.SET_FIRST).value("First_"+i).place();
			pl.label(UserBuilderEvents.SET_LAST).value("Last_"+i).place();
			pl.label(UserBuilderEvents.SET_YEAR).value(1900+i).place(); 
		}
		
		Future<?> f = conveyor.completeAndStop();
		assertThrows(IllegalStateException.class,()->conveyor.command().id(102).create());
		f.get();
		conveyor.completeAndStop().get();
	}

}
