/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.CreateCommand;
import com.aegisql.conveyor.cart.command.RescheduleCommand;
import com.aegisql.conveyor.user.AbstractBuilderEvents;
import com.aegisql.conveyor.user.LowerCaseUserBuilder;
import com.aegisql.conveyor.user.LowerUser;
import com.aegisql.conveyor.user.UpperCaseUserBuilder;
import com.aegisql.conveyor.user.UpperUser;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderEvents;
import com.aegisql.conveyor.user.UserBuilderEvents2;
import com.aegisql.conveyor.user.UserBuilderEvents3;
import com.aegisql.conveyor.user.UserBuilderSmart;
import com.aegisql.conveyor.user.UserBuilderTesting;
import com.aegisql.conveyor.user.UserBuilderTestingState;

// TODO: Auto-generated Javadoc
/**
 * The Class SmartConveyorTest.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class SmartConveyorTest {

	/** The out queue. */
	Queue<User> outQueue = new ConcurrentLinkedQueue<>();

	/**
	 * Sets the up before class.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * Tear down after class.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Sets the up.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Tear down.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test basics smart.
	 *
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	@Test
	public void testBasicsSmart() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE);
		Cart<Integer, Integer, UserBuilderEvents> c4 = c1.nextCart(1999, UserBuilderEvents.SET_YEAR);

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);
		
		conveyor.forEachKeyAndBuilder((key,builder)->{
			System.out.println("-- key="+key+" builder="+builder.get());
		});
		
		Thread.sleep(100);
		conveyor.forEachKeyAndBuilder((key,builder)->{
			System.out.println("-- key="+key+" builder="+builder.get());
		});
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
	 */
	@Test
	public void testRescheduleSmart() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			System.out.println(state);
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST,
				1, TimeUnit.SECONDS);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1, 1999, UserBuilderEvents.SET_YEAR);

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);

		RescheduleCommand<Integer> reschedule = new RescheduleCommand<>(1, 4, TimeUnit.SECONDS);
		conveyor.addCommand(reschedule);
		Thread.sleep(1500);
		conveyor.place(c3);
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
	 * Test basics testing.
	 *
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	@Test
	public void testBasicsTesting() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents2, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTesting::new);

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setName("Testing User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents2> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents2.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents2> c2 = c1.nextCart("Doe", UserBuilderEvents2.SET_LAST);
		Cart<Integer, String, UserBuilderEvents2> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents2.SET_FIRST);
		Cart<Integer, Integer, UserBuilderEvents2> c4 = c1.nextCart(1999, UserBuilderEvents2.SET_YEAR);

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

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setName("Testing User Assembler");
		conveyor.id(1).value("John").label(UserBuilderEvents2.SET_FIRST).place();
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.id(1).value("Doe").label(UserBuilderEvents2.SET_LAST).expirationTime(System.currentTimeMillis() + 10).place();
		conveyor.id(2).value("Mike").label(UserBuilderEvents2.SET_FIRST).ttl(10, TimeUnit.MILLISECONDS).place();
		conveyor.id(1).value(1999).label(UserBuilderEvents2.SET_YEAR).place();
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
		AssemblingConveyor<Integer, UserBuilderEvents2, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTesting::new);

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setName("Testing User Assembler");
		conveyor.part("John").id(1).label(UserBuilderEvents2.SET_FIRST).place();
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.id(1).value("Doe").label(UserBuilderEvents2.SET_LAST).ttl(10, TimeUnit.MILLISECONDS).place();
		conveyor.id(2).value("Mike").label(UserBuilderEvents2.SET_FIRST).expirationTime(System.currentTimeMillis() + 10).place();
		conveyor.part(1999).id(1).label(UserBuilderEvents2.SET_YEAR).place();
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
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void testBasicsTestingCreatingInterface() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents2, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(() -> {
			System.out.println("Default Supplier");
			return new UserBuilderTesting();
		});

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});

		BuilderSupplier<User> sup = () -> {
			System.out.println("Cart Supplier");
			return new UserBuilderTesting();
		};

		BuilderSupplier<User> sup2 = () -> {
			System.out.println("Cmd Supplier");
			return new UserBuilderTesting();
		};

		CompletableFuture<Boolean> cf1 = conveyor.createBuild(1, sup);
		CompletableFuture<Boolean> cf2 = conveyor.createBuild(2, sup);
		CompletableFuture<Boolean> cf3 = conveyor.createBuild(3);

		assertTrue(cf1.get());
		assertTrue(cf2.get());
//		assertFalse(cf3.get()); //this fails - no supplier;
		
		conveyor.addCommand(new CreateCommand<Integer, User>(4));
		conveyor.addCommand(new CreateCommand<Integer, User>(5, sup2));

		conveyor.setName("Testing User Assembler");
		conveyor.part("John").id(1).label(UserBuilderEvents2.SET_FIRST).place();
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.id(1).value("Doe").label(UserBuilderEvents2.SET_LAST).ttl(10, TimeUnit.MILLISECONDS).place();
		conveyor.id(2).value("Mike").label(UserBuilderEvents2.SET_FIRST).expirationTime(System.currentTimeMillis() + 10).place();
		conveyor.part(1999).id(1).label(UserBuilderEvents2.SET_YEAR).place();
		Thread.sleep(100);
		conveyor.forEachKeyAndBuilder((key,builder)->{
			System.out.println("-- key="+key+" builder="+builder.get());
		});

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
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	@Test
	public void testBasicsTestingState() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents3, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTestingState::new);

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setName("TestingState User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents3> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents3.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents3> c2 = c1.nextCart("Doe", UserBuilderEvents3.SET_LAST);
		Cart<Integer, String, UserBuilderEvents3> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents3.SET_FIRST);
		Cart<Integer, Integer, UserBuilderEvents3> c4 = c1.nextCart(1999, UserBuilderEvents3.SET_YEAR);

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
	 * @throws InterruptedException             the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void testRejectedStartOffer() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 2;
		});
		conveyor.rejectUnexpireableCartsOlderThan(1, TimeUnit.SECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		assertTrue(conveyor.place(c1).get());
		Thread.sleep(1100);
		CompletableFuture<Boolean> future = conveyor.place(c2);
		assertTrue(future.isCompletedExceptionally());
		conveyor.stop();
	}

	/**
	 * Test rejected start add.
	 *
	 * @throws InterruptedException             the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test(expected = ExecutionException.class) // ???? Failed
	public void testRejectedStartAdd() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 2;
		});
		conveyor.rejectUnexpireableCartsOlderThan(1, TimeUnit.SECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		assertTrue(conveyor.place(c1).get());
		Thread.sleep(1100);
		conveyor.place(c2).get();
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
	 * @throws ExecutionException the execution exception
	 * @throws TimeoutException the timeout exception
	 */
	@Test(expected=CancellationException.class)
	public void testFutureSmart() throws InterruptedException, ExecutionException, TimeoutException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE);
		Cart<Integer, Integer, UserBuilderEvents> c4 = c1.nextCart(1999, UserBuilderEvents.SET_YEAR);

		
		CompletableFuture<User> f1 = conveyor.getFuture(1);
		
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

		CompletableFuture<User> f2 = conveyor.getFuture(2);
		assertNotNull(f2);
		assertFalse(f2.isCancelled());
		assertFalse(f2.isCompletedExceptionally());
		assertFalse(f2.isDone());

		conveyor.forEachKeyAndBuilder((key,builder)->{
			System.out.println("-- key="+key+" builder="+builder.get());
		});
		
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

		User user2 = f2.get(200,TimeUnit.MILLISECONDS);

		conveyor.stop();
	}

	/**
	 * Test upper case.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 * @throws TimeoutException the timeout exception
	 */
	@Test
	public void testUpperCase() throws InterruptedException, ExecutionException, TimeoutException {
		AssemblingConveyor<Integer, AbstractBuilderEvents, User> conveyor = new AssemblingConveyor<>();

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Upper Assembler");
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		
		CompletableFuture<User> f1 = conveyor.createBuildFuture(1,UpperCaseUserBuilder::new);
		
		assertNotNull(f1);
		assertFalse(f1.isCancelled());
		assertFalse(f1.isCompletedExceptionally());
		assertFalse(f1.isDone());
		conveyor.id(1).value("John").label(AbstractBuilderEvents.SET_FIRST).place();
		conveyor.id(1).value("Doe").label(AbstractBuilderEvents.SET_LAST).place();
		conveyor.id(1).value(1999).label(AbstractBuilderEvents.SET_YEAR).place();

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
	 * @throws ExecutionException the execution exception
	 * @throws TimeoutException the timeout exception
	 */
	@Test
	public void testLowerCase() throws InterruptedException, ExecutionException, TimeoutException {
		AssemblingConveyor<Integer, AbstractBuilderEvents, User> conveyor = new AssemblingConveyor<>();

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Lower Assembler");
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		
		CompletableFuture<User> f1 = conveyor.createBuildFuture(1,LowerCaseUserBuilder::new);
		
		assertNotNull(f1);
		assertFalse(f1.isCancelled());
		assertFalse(f1.isCompletedExceptionally());
		assertFalse(f1.isDone());

		conveyor.id(1).value("John").label(AbstractBuilderEvents.SET_FIRST).place();
		conveyor.id(1).value("Doe").label(AbstractBuilderEvents.SET_LAST).place();
		conveyor.id(1).value(1999).label(AbstractBuilderEvents.SET_YEAR).place();

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
	 * @throws ExecutionException the execution exception
	 * @throws TimeoutException the timeout exception
	 */
	@Test
	public void testObject() throws InterruptedException, ExecutionException, TimeoutException {
		AssemblingConveyor<Integer, AbstractBuilderEvents, Object> conveyor = new AssemblingConveyor<>();

		conveyor.setResultConsumer(res -> {
			System.out.println(res.product);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("Object Assembler");
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		
		CompletableFuture<Object> f1 = conveyor.createBuildFuture(1,LowerCaseUserBuilder::new);
		
		assertNotNull(f1);
		assertFalse(f1.isCancelled());
		assertFalse(f1.isCompletedExceptionally());
		assertFalse(f1.isDone());

		conveyor.id(1).value("John").label(AbstractBuilderEvents.SET_FIRST).place();
		conveyor.id(1).value("Doe").label(AbstractBuilderEvents.SET_LAST).place();
		conveyor.id(1).value(1999).label(AbstractBuilderEvents.SET_YEAR).place();

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
	 * @throws ExecutionException the execution exception
	 * @throws TimeoutException the timeout exception
	 */
	@Test
	public void testUpperCaseWithAddedExpirationAndTesting() throws InterruptedException, ExecutionException, TimeoutException {
		AssemblingConveyor<Integer, AbstractBuilderEvents, User> conveyor = new AssemblingConveyor<>();

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setName("User Upper Assembler");
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		
		CompletableFuture<User> f1 = conveyor.createBuildFuture(1,
				BuilderSupplier.of(UpperCaseUserBuilder::new)
				.expire(100,TimeUnit.MILLISECONDS)
				.readyAlgorithm(new ReadinessTester().accepted(3)));
		
		assertNotNull(f1);
		assertFalse(f1.isCancelled());
		assertFalse(f1.isCompletedExceptionally());
		assertFalse(f1.isDone());

		conveyor.id(1).value("John").label(AbstractBuilderEvents.SET_FIRST).place();
		conveyor.id(1).value("Doe").label(AbstractBuilderEvents.SET_LAST).place();
		conveyor.id(1).value(1999).label(AbstractBuilderEvents.SET_YEAR).place();

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
	 * @throws ExecutionException the execution exception
	 * @throws TimeoutException the timeout exception
	 */
	@Test(expected=CancellationException.class)
	public void testFailingUpperCaseWithAddedExpirationAndTesting() throws InterruptedException, ExecutionException, TimeoutException {
		AssemblingConveyor<Integer, AbstractBuilderEvents, User> conveyor = new AssemblingConveyor<>();

		conveyor.setResultConsumer(res -> {
			outQueue.add(res.product);
		});
		conveyor.setName("User Upper Assembler");
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		
		CompletableFuture<User> f1 = conveyor.createBuildFuture(1,
				BuilderSupplier.of(UpperCaseUserBuilder::new)
				.expire(100,TimeUnit.MILLISECONDS)
				.readyAlgorithm(new ReadinessTester<Integer, AbstractBuilderEvents, User>().accepted(4))
				.onTimeout((b)->{System.out.println("--- TIMEOUT--- "+b.get());})
				);
		
		assertNotNull(f1);
		assertFalse(f1.isCancelled());
		assertFalse(f1.isCompletedExceptionally());
		assertFalse(f1.isDone());

		conveyor.id(1).value("John").label(AbstractBuilderEvents.SET_FIRST).place();
		conveyor.id(1).value("Doe").label(AbstractBuilderEvents.SET_LAST).place();
		conveyor.id(1).value(1999).label(AbstractBuilderEvents.SET_YEAR).place();

		User user1 = f1.get();
		assertNotNull(user1);
		System.out.println(user1);
		assertEquals(user1,new UpperUser("JOHN","DOE",1999));
		conveyor.stop();
	}

}
