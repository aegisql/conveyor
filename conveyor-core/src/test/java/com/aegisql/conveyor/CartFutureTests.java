package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderEvents;
import com.aegisql.conveyor.user.UserBuilderSmart;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// TODO: Auto-generated Javadoc
/**
 * The Class CartFutureTests.
 */
public class CartFutureTests {

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
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsSmart() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE,100,TimeUnit.MILLISECONDS);
		Cart<Integer, Integer, UserBuilderEvents> c4 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<Boolean> cf1 = conveyor.place(c1);
		assertFalse(cf1.isDone());
		CompletableFuture<Boolean> cf2 = conveyor.place(c2);
		assertFalse(cf2.isDone());
		CompletableFuture<Boolean> cf3 = conveyor.place(c3);
		assertFalse(cf3.isDone());
		CompletableFuture<Boolean> cf4 = conveyor.place(c4);
		assertFalse(cf4.isDone());

		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		assertTrue(cf4.get());

		assertTrue(cf1.isDone());
		assertTrue(cf2.isDone());
		assertTrue(cf3.isDone());
		assertTrue(cf4.isDone());

		CompletableFuture<User> uf3 = conveyor.future().id(2).get();

		assertThrows(CancellationException.class,()->uf3.get());
		
		conveyor.stop();
	}
	
	/**
	 * Test expired smart.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testExpiredSmart() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(10, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST,10,TimeUnit.MILLISECONDS);
		Thread.sleep(20);
		CompletableFuture<Boolean> cf1 = conveyor.place(c1);
		
		assertTrue(cf1.isDone());

		assertThrows(ExecutionException.class,()->assertFalse(cf1.get()));
		conveyor.stop();
	}
	
	/**
	 * Test expired smart2.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testExpiredSmart2() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(10, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST,10,TimeUnit.MILLISECONDS);
		Thread.sleep(20);
		CompletableFuture<Boolean> cf1 = conveyor.place(c1);
		assertThrows(ExecutionException.class,()->cf1.get());

	}

	/**
	 * Test basics build future smart.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsbuildSmart() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE,100,TimeUnit.MILLISECONDS);
		Cart<Integer, Integer, UserBuilderEvents> c4 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.build().id(1).ttl(100,TimeUnit.MILLISECONDS).createFuture();
		CompletableFuture<User> cf2 = conveyor.build().id(2).ttl(100,TimeUnit.MILLISECONDS).createFuture();

		assertFalse(cf1.isDone());
		assertFalse(cf2.isDone());

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);

		User u1 = cf1.get();
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}

	/**
	 * Test basics future smart1.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsFutureSmart1() {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE,100,TimeUnit.MILLISECONDS);
		Cart<Integer, Integer, UserBuilderEvents> c4 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);
		CompletableFuture<User> cf1 = conveyor.future().id(1).get();
		CompletableFuture<User> cf2 = conveyor.future().id(2).get();

		assertFalse(cf1.isDone());
		assertFalse(cf2.isDone());


		assertThrows(CancellationException.class,()->{
			var user = cf1.join();
			assertNotNull(user);
		});
		assertThrows(CancellationException.class,()->cf2.join());
		conveyor.stop();
	}

	
	/**
	 * Test basics future smart2.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsFutureSmart2() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE,100,TimeUnit.MILLISECONDS);
		Cart<Integer, Integer, UserBuilderEvents> c4 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);
		CompletableFuture<User> cf1 = conveyor.future().id(1).ttl(100,TimeUnit.MILLISECONDS).get();
		CompletableFuture<User> cf2 = conveyor.future().id(2).ttl(100,TimeUnit.MILLISECONDS).get();

		assertFalse(cf1.isDone());
		assertFalse(cf2.isDone());


		assertThrows(CancellationException.class,()->{
			var user = cf1.join();
			assertNotNull(user);
		});
		assertThrows(CancellationException.class,()->cf2.join());

		conveyor.stop();
	}
	
	/**
	 * Test basics future smart3.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsFutureSmart3() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE,100,TimeUnit.MILLISECONDS);
		Cart<Integer, Integer, UserBuilderEvents> c4 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		conveyor.place(c1);
		CompletableFuture<User> cf1 = conveyor.future().id(1).expirationTime(System.currentTimeMillis()+100).get();
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);

		CompletableFuture<User> cf2 = conveyor.future().id(2).expirationTime(System.currentTimeMillis()+100).get();

		assertFalse(cf1.isDone());
		assertFalse(cf2.isDone());


		User u1 = cf1.get();
		assertNotNull(u1);
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}
	
	/**
	 * Test basics future smart4.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsFutureSmart4() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE,100,TimeUnit.MILLISECONDS);
		Cart<Integer, Integer, UserBuilderEvents> c4 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.future().id(1).ttl(Duration.ofMillis(100)).get();
		CompletableFuture<User> cf2 = conveyor.future().id(2).ttl(Duration.ofMillis(100)).get();

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);

		assertFalse(cf1.isDone());
		assertFalse(cf2.isDone());


		User u1 = cf1.get();

		assertTrue(cf1.isDone());
		assertFalse(cf2.isDone());

		assertNotNull(u1);
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}
	
	/**
	 * Test basics future smart5.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsFutureSmart5() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE,100,TimeUnit.MILLISECONDS);
		Cart<Integer, Integer, UserBuilderEvents> c4 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.future().id(1).expirationTime(Instant.now().plusMillis(100)).get();
		CompletableFuture<User> cf2 = conveyor.future().id(2).expirationTime(Instant.now().plusMillis(100)).get();

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);

		assertFalse(cf1.isDone());
		assertFalse(cf2.isDone());


		User u1 = cf1.get();

		assertTrue(cf1.isDone());
		assertFalse(cf2.isDone());

		assertNotNull(u1);
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}
	
	/**
	 * Test basics build future smart2.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsbuildSmart2() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.build().id(1).createFuture();
		CompletableFuture<User> cf2 = conveyor.build().id(2).createFuture();

		assertFalse(cf1.isDone());

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);

		User u1 = cf1.get();
		assertNotNull(u1);
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}

	/**
	 * Test basics build future smart3.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsbuildSmart3() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.build().id(1).expirationTime(System.currentTimeMillis()+100).createFuture();
		CompletableFuture<User> cf2 = conveyor.build().id(2).expirationTime(System.currentTimeMillis()+100).createFuture();

		assertFalse(cf1.isDone());

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);

		User u1 = cf1.get();
		assertNotNull(u1);
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}

	/**
	 * Test basics build future smart4.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsbuildSmart4() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.build().id(1).ttl(Duration.ofMillis(100)).createFuture();
		CompletableFuture<User> cf2 = conveyor.build().id(2).ttl(Duration.ofMillis(100)).createFuture();

		assertFalse(cf1.isDone());

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);

		User u1 = cf1.get();
		assertNotNull(u1);
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}
	
	/**
	 * Test basics build future smart5.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsbuildSmart5() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.build().id(1).expirationTime(Instant.now().plusMillis(100)).createFuture();
		CompletableFuture<User> cf2 = conveyor.build().id(2).expirationTime(Instant.now().plusMillis(100)).createFuture();

		assertFalse(cf1.isDone());

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);

		User u1 = cf1.get();
		assertNotNull(u1);
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}

	/**
	 * Test basics build future smart6.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsbuildSmart6() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.build().id(1).supplier(UserBuilderSmart::new).createFuture();
		CompletableFuture<User> cf2 = conveyor.build().id(2).supplier(UserBuilderSmart::new).createFuture();

		assertFalse(cf1.isDone());

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);

		User u1 = cf1.get();
		assertNotNull(u1);
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}

	/**
	 * Test basics build future smart7.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsbuildSmart7() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.build().id(1).supplier(UserBuilderSmart::new).expirationTime(System.currentTimeMillis()+100).createFuture();
		CompletableFuture<User> cf2 = conveyor.build().id(2).supplier(UserBuilderSmart::new).expirationTime(System.currentTimeMillis()+100).createFuture();

		assertFalse(cf1.isDone());

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);

		User u1 = cf1.get();
		assertNotNull(u1);
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}
	
	/**
	 * Test basics build future smart8.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsbuildSmart8() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.build().id(1).supplier(UserBuilderSmart::new).ttl(100,TimeUnit.MILLISECONDS).createFuture();
		CompletableFuture<User> cf2 = conveyor.build().id(2).supplier(UserBuilderSmart::new).ttl(100,TimeUnit.MILLISECONDS).createFuture();

		assertFalse(cf1.isDone());

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);

		User u1 = cf1.get();
		assertNotNull(u1);
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}

	/**
	 * Test basics build future smart9.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsbuildSmart9() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.build().id(1).supplier(UserBuilderSmart::new).ttl(Duration.ofMillis(100)).createFuture();
		CompletableFuture<User> cf2 = conveyor.build().id(2).supplier(UserBuilderSmart::new).ttl(Duration.ofMillis(100)).createFuture();

		assertFalse(cf1.isDone());

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);

		User u1 = cf1.get();
		assertNotNull(u1);
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}

	/**
	 * Test basics build future smart10.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws Exception the exception
	 */
	@Test
	public void testBasicsbuildSmart10() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.resultConsumer().first(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.build().id(1).supplier(UserBuilderSmart::new).expirationTime(Instant.now().plusMillis(100)).createFuture();
		CompletableFuture<User> cf2 = conveyor.build().id(2).supplier(UserBuilderSmart::new).expirationTime(Instant.now().plusMillis(100)).createFuture();

		assertFalse(cf1.isDone());

		conveyor.place(c1);
		conveyor.place(c2);
		conveyor.place(c3);

		User u1 = cf1.get();
		assertNotNull(u1);
		assertThrows(CancellationException.class,()->cf2.get());
		conveyor.stop();
	}
	
}
