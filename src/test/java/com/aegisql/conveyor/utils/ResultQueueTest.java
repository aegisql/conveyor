package com.aegisql.conveyor.utils;

import static org.junit.Assert.*;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.BuildingSite.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.AbstractCommand;
import com.aegisql.conveyor.cart.command.CancelCommand;
import com.aegisql.conveyor.cart.command.CreateCommand;
import com.aegisql.conveyor.cart.command.TimeoutCommand;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;

public class ResultQueueTest {

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

	@Test
	public void testQueue() {
		ResultQueue<String,User> q = new ResultQueue<>();
		assertEquals(0, q.size());
		ConcurrentLinkedDeque<User> u = q.<ConcurrentLinkedDeque<User>>unwrap();
		assertNotNull(u);
		ProductBin<String, User> b1 = new ProductBin<String, User>("", new User("","",1999), 0, Status.READY);
		q.accept(b1);

		assertEquals(1, q.size());
		
		User u1 = q.poll();
		assertNotNull(u1);
		assertEquals(0, q.size());

		ProductBin<String, User> b2 = new ProductBin<String, User>("", new User("","",1999), 0, Status.READY);
		q.accept(b2);
		assertEquals(1, u.size());
		User u2 = u.poll();
		assertNotNull(u2);
		assertEquals(0, u.size());

		
	}
	
	@Test
	public void testSimpleConveyor() throws InterruptedException {
		
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();
		
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilder::new);
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		assertEquals(1000, conveyor.getDefaultBuilderTimeout());
		conveyor.setExpirationCollectionIdleInterval(500, TimeUnit.MILLISECONDS);
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
		conveyor.setResultConsumer(outQueue);
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = c1.nextCart("Doe", "setLast");
		Cart<Integer, String, String> c3 = new ShoppingCart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = c1.nextCart(1999, "setYearOfBirth");

		Cart<Integer, Integer, String> c5 = new ShoppingCart<>(3, 1999, "setBlah");

		Cart<Integer, String, String> c6 = new ShoppingCart<>(6, "Ann", "setFirst");
		Cart<Integer, String, String> c7 = new ShoppingCart<>(7, "Nik", "setLast", 1, TimeUnit.HOURS);

		AbstractCommand<Integer,?> c8 = new CreateCommand<>(8,1,TimeUnit.SECONDS);
		AbstractCommand<Integer,?> c9 = new CreateCommand<>(8,UserBuilder::new,1,TimeUnit.SECONDS);

		conveyor.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.offer(c2);
		conveyor.offer(c3);
		conveyor.offer(c4);
		conveyor.offer(c6);
		Thread.sleep(100);
		conveyor.setExpirationCollectionIdleInterval(1000, TimeUnit.MILLISECONDS);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		conveyor.offer(c7);
		conveyor.addCommand(c8);
		conveyor.addCommand(c9);
		Thread.sleep(100);
		conveyor.addCommand( new CancelCommand<Integer>(6));
		conveyor.addCommand( new TimeoutCommand<Integer, Supplier<?>>(7));

		conveyor.offer(c5);
		Thread.sleep(2000);
		System.out.println("COL:"+conveyor.getCollectorSize());
		System.out.println("DEL:"+conveyor.getDelayedQueueSize());
		System.out.println("IN :"+conveyor.getInputQueueSize());
		conveyor.stop();
		Thread.sleep(1000);
	}


}
