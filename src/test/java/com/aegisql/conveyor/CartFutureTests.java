package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderEvents;
import com.aegisql.conveyor.user.UserBuilderSmart;

public class CartFutureTests {

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

	@Test(expected=CancellationException.class)
	public void testBasicsSmart() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setResultConsumer(res -> {
			System.out.println("Result:"+res);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE,100,TimeUnit.MILLISECONDS);
		Cart<Integer, Integer, UserBuilderEvents> c4 = c1.nextCart(1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<Boolean> cf1 = conveyor.offer(c1);
		CompletableFuture<Boolean> cf2 = conveyor.offer(c2);
		CompletableFuture<Boolean> cf3 = conveyor.offer(c3);
		CompletableFuture<Boolean> cf4 = conveyor.offer(c4);
		
		assertFalse(cf1.isDone());
		assertFalse(cf2.isDone());
		assertFalse(cf3.isDone());
		assertFalse(cf4.isDone());

		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		assertTrue(cf4.get());

		assertTrue(cf1.isDone());
		assertTrue(cf2.isDone());
		assertTrue(cf3.isDone());
		assertTrue(cf4.isDone());

		CompletableFuture<User> uf3 = conveyor.getFuture(2);
		
		User u3 = uf3.get();
		
		conveyor.stop();
	}

	@Test(expected=CancellationException.class)
	public void testBasicsFutureSmart() throws InterruptedException, Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		conveyor.setResultConsumer(res -> {
			System.out.println("Result:"+res);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE,100,TimeUnit.MILLISECONDS);
		Cart<Integer, Integer, UserBuilderEvents> c4 = c1.nextCart(1999, UserBuilderEvents.SET_YEAR);

		CompletableFuture<User> cf1 = conveyor.createBuildFuture(1,100,TimeUnit.MILLISECONDS);
		CompletableFuture<User> cf2 = conveyor.createBuildFuture(2,100,TimeUnit.MILLISECONDS);

		assertFalse(cf1.isDone());
		assertFalse(cf2.isDone());

		conveyor.offer(c1);
		conveyor.offer(c2);
		conveyor.offer(c3);
		conveyor.offer(c4);

		User u1 = cf1.get();
		cf2.get();
		conveyor.stop();
	}

	
}
