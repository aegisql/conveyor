package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import com.aegisql.conveyor.user.UserBuilderExpireable;
import com.aegisql.conveyor.user.UserBuilderSmart;

public class PostponeExpirationTest {

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

	Queue<User> outQueue = new ConcurrentLinkedQueue<>();

	@Test
	public void testDefaultExpirationPostpone() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		outQueue.clear();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res -> {
			System.out.println("Result " + res);
			outQueue.add(res.product);
		});
		conveyor.setScrapConsumer(bin -> {
			System.out.println("Error " + bin);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");

		conveyor.enablePostponeExpiration(true);
		conveyor.setExpirationPostponeTime(100, TimeUnit.MILLISECONDS);
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		conveyor.setIdleHeartBeat(1, TimeUnit.MILLISECONDS);

		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = c1.nextCart(1999, UserBuilderEvents.SET_YEAR);

		conveyor.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		Thread.sleep(50);
		long et1 = conveyor.getExpirationTime(1);		
		conveyor.offer(c2);
		Thread.sleep(50);
		long et2 = conveyor.getExpirationTime(1);
		conveyor.offer(c3);
		Thread.sleep(50); //over original exp time

		long diff = et2-et1;
		System.out.println("et1="+et1+" diff="+diff);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		conveyor.stop();
	}

	@Test
	public void testCartExpirationPostpone() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		outQueue.clear();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res -> {
			System.out.println("Result " + res);
			outQueue.add(res.product);
		});
		conveyor.setScrapConsumer(bin -> {
			System.out.println("Error " + bin);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");

		conveyor.enablePostponeExpiration(true);
		conveyor.setIdleHeartBeat(1, TimeUnit.MILLISECONDS);

		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents.SET_FIRST,10, TimeUnit.MILLISECONDS);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST,150, TimeUnit.MILLISECONDS);

		conveyor.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.offer(c2);
		Thread.sleep(100); //created with 10, but 100 added by second cart
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1, 1999, UserBuilderEvents.SET_YEAR,100, TimeUnit.MILLISECONDS);
		conveyor.offer(c3);

		Thread.sleep(10);

		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		conveyor.stop();
	}

	@Test
	public void testBuilderExpirationPostpone() throws InterruptedException {
		AssemblingConveyor<Integer, String, User> conveyor = new AssemblingConveyor<>();
		outQueue.clear();
		conveyor.setBuilderSupplier(()->new UserBuilderExpireable(10));

		conveyor.setResultConsumer(res -> {
			System.out.println("Result " + res);
			outQueue.add(res.product);
		});
		conveyor.setScrapConsumer(bin -> {
			System.out.println("Error " + bin);
		});
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");

		conveyor.enablePostponeExpiration(true);
		conveyor.setIdleHeartBeat(1, TimeUnit.MILLISECONDS);
		conveyor.setDefaultCartConsumer((l,v,b)->{
			UserBuilderExpireable ub = (UserBuilderExpireable)b;
			switch(l) {
				case "FIRST":
					ub.setFirst(v.toString());
					break;
				case "LAST":
					ub.setLast(v.toString());
					break;
				case "YEAR":
					ub.setYearOfBirth((Integer)v);
					break;
			}
		});
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John",
				"FIRST");
		Cart<Integer, String, String> c2 = new ShoppingCart<>(1,"Doe", "LAST");

		conveyor.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.offer(c2);
		Thread.sleep(100); //created with 10, but 100 added by second cart
		Cart<Integer, Integer, String> c3 = new ShoppingCart<>(1, 1999, "YEAR");
		conveyor.offer(c3);

		Thread.sleep(10);

		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		conveyor.stop();
	}

	
}
