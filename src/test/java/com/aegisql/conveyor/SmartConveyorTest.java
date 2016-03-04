/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import com.aegisql.conveyor.cart.command.RescheduleCommand;
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
	 * Test basics smart.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testBasicsSmart() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents.CREATE);
		Cart<Integer, Integer, UserBuilderEvents> c4 = c1.nextCart(1999, UserBuilderEvents.SET_YEAR);


		conveyor.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.offer(c2);
		conveyor.offer(c3);
		conveyor.offer(c4);
		Thread.sleep(100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		Thread.sleep(100);

		conveyor.stop();
	}

	@Test
	public void testRescheduleSmart() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setReadinessEvaluator((state, builder) -> {
			System.out.println(state);
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST,1,TimeUnit.SECONDS);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);


		conveyor.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.offer(c2);
		
		RescheduleCommand<Integer> reschedule = new RescheduleCommand<>(
						1, 4,TimeUnit.SECONDS);
		conveyor.addCommand(reschedule);
		Thread.sleep(1500);
		conveyor.offer(c3);
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
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testBasicsTesting() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents2, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTesting::new);

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setName("Testing User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents2> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents2.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents2> c2 = c1.nextCart("Doe", UserBuilderEvents2.SET_LAST);
		Cart<Integer, String, UserBuilderEvents2> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents2.SET_FIRST);
		Cart<Integer, Integer, UserBuilderEvents2> c4 = c1.nextCart(1999, UserBuilderEvents2.SET_YEAR);


		conveyor.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.offer(c2);
		conveyor.offer(c3);
		conveyor.offer(c4);
		Thread.sleep(100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		Thread.sleep(100);

		conveyor.stop();
	}

	@Test
	public void testBasicsTestingWithInternalOfferInterfaces() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents2, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTesting::new);

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setName("Testing User Assembler");
		conveyor.offer(1, "John", UserBuilderEvents2.SET_FIRST);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.offer(1,"Doe", UserBuilderEvents2.SET_LAST,System.currentTimeMillis()+10);
		conveyor.offer(2, "Mike", UserBuilderEvents2.SET_FIRST,10,TimeUnit.MILLISECONDS);
		conveyor.offer(1,1999, UserBuilderEvents2.SET_YEAR);
		Thread.sleep(100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		Thread.sleep(100);

		conveyor.stop();
	}

	@Test
	public void testBasicsTestingWithInternalAddInterfaces() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents2, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTesting::new);

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setName("Testing User Assembler");
		conveyor.add(1, "John", UserBuilderEvents2.SET_FIRST);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.add(1,"Doe", UserBuilderEvents2.SET_LAST,10,TimeUnit.MILLISECONDS);
		conveyor.add(2, "Mike", UserBuilderEvents2.SET_FIRST,System.currentTimeMillis()+10);
		conveyor.add(1,1999, UserBuilderEvents2.SET_YEAR);
		Thread.sleep(100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		Thread.sleep(100);

		conveyor.stop();
	}

	@Test
	public void testBasicsTestingCreatingInterface() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents2, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(()->{
			System.out.println("Default Supplier");
			return new UserBuilderTesting();});

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		
		BuilderSupplier<User> sup = ()->{
			System.out.println("Cart Supplier");
			return new UserBuilderTesting();};
		
		conveyor.createBuild(1,sup);
		conveyor.createBuild(2,sup);
		conveyor.createBuild(3);
		
		conveyor.setName("Testing User Assembler");
		conveyor.add(1, "John", UserBuilderEvents2.SET_FIRST);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.add(1,"Doe", UserBuilderEvents2.SET_LAST,10,TimeUnit.MILLISECONDS);
		conveyor.add(2, "Mike", UserBuilderEvents2.SET_FIRST,System.currentTimeMillis()+10);
		conveyor.add(1,1999, UserBuilderEvents2.SET_YEAR);
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
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testBasicsTestingState() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents3, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTestingState::new);

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setName("TestingState User Assembler");
		ShoppingCart<Integer, String, UserBuilderEvents3> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents3.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents3> c2 = c1.nextCart("Doe", UserBuilderEvents3.SET_LAST);
		Cart<Integer, String, UserBuilderEvents3> c3 = new ShoppingCart<>(2, "Mike", UserBuilderEvents3.SET_FIRST);
		Cart<Integer, Integer, UserBuilderEvents3> c4 = c1.nextCart(1999, UserBuilderEvents3.SET_YEAR);


		conveyor.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.offer(c2);
		conveyor.offer(c3);
		conveyor.offer(c4);
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
	 */
	@Test
	public void testRejectedStartOffer() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 2;
		});
		conveyor.rejectUnexpireableCartsOlderThan(1, TimeUnit.SECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		assertTrue(conveyor.offer(c1));
		Thread.sleep(1100);
		assertFalse(conveyor.offer(c2));
		conveyor.stop();
	}

	/**
	 * Test rejected start add.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test(expected=IllegalStateException.class) //???? Failed
	public void testRejectedStartAdd() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res.product);
				    });
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 2;
		});
		conveyor.rejectUnexpireableCartsOlderThan(1, TimeUnit.SECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		assertTrue(conveyor.add(c1));
		Thread.sleep(1100);
		conveyor.add(c2);
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
		
}
