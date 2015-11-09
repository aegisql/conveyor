package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import com.aegisql.conveyor.user.UserBuilderEvents;
import com.aegisql.conveyor.user.UserBuilderSmart;

public class AssemblingConveyorTest {

	Queue<User> outQueue = new ConcurrentLinkedQueue<>();
	
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
	public void testBasics() throws InterruptedException {
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilder::new);
		conveyor.setCartConsumer((label, value, builder) -> {
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
				    	outQueue.add(res);
				    });
		conveyor.setReadinessEvaluator((lot, builder) -> {
			return lot.previouslyAccepted == 2;
		});
		
		Cart<Integer, String, String> c1 = new Cart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = c1.nextCart("Doe", "setLast");
		Cart<Integer, String, String> c3 = new Cart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = c1.nextCart(1999, "setYearOfBirth");

		Cart<Integer, Integer, String> c5 = new Cart<>(3, 1999, "setBlah");

		Cart<Integer, String, String> c6 = new Cart<>(6, "Ann", "setFirst");

		conveyor.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.offer(c2);
		conveyor.offer(c3);
		conveyor.offer(c4);
		conveyor.offer(c6);
		Thread.sleep(100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		conveyor.addCommand( new Cart<Integer,String,Command>(6,"",Command.CANCEL_KEY));

		conveyor.offer(c5);
		Thread.sleep(100);
		//conveyor.stop();
	}
	
	@Test
	public void testDelayed() throws InterruptedException {
		Cart<Integer, String, String> c1 = new Cart<>(1, "A", "setFirst",1,TimeUnit.SECONDS);
		Cart<Integer, String, String> c2 = new Cart<>(1, "B", "setLast",c1.getExpirationTime());

		assertFalse(c1.expired());
		assertFalse(c2.expired());
		System.out.println(c1);
		System.out.println(c2);
		assertEquals(1000, c1.getExpirationTime() - c1.getCreationTime());
		System.out.println(c1.getDelay(TimeUnit.MILLISECONDS));
		
		BlockingQueue q = new DelayQueue();
		q.add(c1);
		assertNull(q.poll());
		Thread.sleep(1000);
		assertNotNull(q.poll());
		
	}

	public static class MyDelayed implements Delayed {
		public long delay = 1;
		public int compareTo(Delayed o) {
			return 0;
		}
		public long getDelay(TimeUnit unit) {
			return delay;
		}
		
	}
	
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
