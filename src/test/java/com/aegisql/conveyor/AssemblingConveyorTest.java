package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.BuildingSite.Status;
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
	public void testUnconfiguredBuilderSupplier() throws InterruptedException {
		AtomicBoolean visited = new AtomicBoolean(false);
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((ex,o)->{
			assertTrue(ex.startsWith("Cart processor failed Builder Supplier is not set"));
			assertTrue(o instanceof Cart);
			visited.set(true);
		});
		Cart<Integer, String, String> c1 = new Cart<>(1, "John", "setFirst");
		assertTrue(conveyor.offer(c1));
		Thread.sleep(100);
		assertTrue(visited.get());
	}

	@Test
	public void testOfferStopped() throws InterruptedException {
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((ex,o)->{
			System.out.println(ex+" "+o);
			assertTrue(ex.startsWith("Not Running"));
			assertTrue(o instanceof Cart);
		});
		Cart<Integer, String, String> c1 = new Cart<>(1, "John", "setFirst");
		conveyor.stop();
		assertFalse(conveyor.offer(c1));
	}

	@Test(expected=IllegalStateException.class)
	public void testCommandStopped() throws InterruptedException {
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((ex,o)->{
			System.out.println(ex+" "+o);
			assertTrue(ex.startsWith("Not Running"));
			assertTrue(o instanceof Cart);
		});
		Cart<Integer, String, Command> c1 = new Cart<>(1, null, Command.TIMEOUT_BUILD);
		conveyor.stop();
		assertFalse(conveyor.addCommand(c1));
	}

	@Test(expected=IllegalStateException.class)
	public void testCommandExpired() throws InterruptedException {
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((ex,o)->{
			System.out.println(ex+" "+o);
			assertTrue(ex.startsWith("Expired command"));
			assertTrue(o instanceof Cart);
		});
		Cart<Integer, String, Command> c1 = new Cart<>(1, null, Command.TIMEOUT_BUILD,1,TimeUnit.MILLISECONDS);
		Thread.sleep(10);
		conveyor.addCommand(c1);
	}

	@Test(expected=IllegalStateException.class)
	public void testCommandTooOld() throws InterruptedException {
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.rejectUnexpireableCartsOlderThan(10, TimeUnit.MILLISECONDS);
		conveyor.setScrapConsumer((ex,o)->{
			System.out.println(ex+" "+o);
			assertTrue(ex.startsWith("Command too old"));
			assertTrue(o instanceof Cart);
		});
		Cart<Integer, String, Command> c1 = new Cart<>(1, null, Command.TIMEOUT_BUILD,100,TimeUnit.MILLISECONDS);
		Thread.sleep(20);
		conveyor.addCommand(c1);
	}

	@Test(expected=IllegalStateException.class)
	public void testAddStopped() throws InterruptedException {
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((ex,o)->{
			System.out.println(ex+" "+o);
			assertTrue(ex.startsWith("Not Running"));
			assertTrue(o instanceof Cart);
		});
		Cart<Integer, String, String> c1 = new Cart<>(1, "John", "setFirst");
		conveyor.stop();
		conveyor.add(c1);
	}

	@Test(expected=IllegalStateException.class)
	public void testAddExpiredStopped() throws InterruptedException {
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((ex,o)->{
			System.out.println(ex+" "+o);
			assertTrue(ex.startsWith("Cart expired"));
			assertTrue(o instanceof Cart);
		});
		Cart<Integer, String, String> c1 = new Cart<>(1, "John", "setFirst",1,TimeUnit.MILLISECONDS);
		Thread.sleep(10);
		conveyor.add(c1);
	}

	@Test()
	public void testOfferExpiredStopped() throws InterruptedException {
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setScrapConsumer((ex,o)->{
			System.out.println(ex+" "+o);
			assertTrue(ex.startsWith("Cart expired"));
			assertTrue(o instanceof Cart);
		});
		Cart<Integer, String, String> c1 = new Cart<>(1, "John", "setFirst",1,TimeUnit.MILLISECONDS);
		Thread.sleep(10);
		assertFalse(conveyor.offer(c1));
	}

	@Test
	public void testBasics() throws InterruptedException {
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilder::new);
		conveyor.setBuilderTimeout(1, TimeUnit.SECONDS);
		assertEquals(1000, conveyor.getBuilderTimeout());
		conveyor.setExpirationCollectionInterval(500, TimeUnit.MILLISECONDS);
		assertEquals(500,conveyor.getExpirationCollectionInterval());
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
			return lot.previouslyAccepted == 3;
		});
		
		Cart<Integer, String, String> c1 = new Cart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = c1.nextCart("Doe", "setLast");
		Cart<Integer, String, String> c3 = new Cart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = c1.nextCart(1999, "setYearOfBirth");

		Cart<Integer, Integer, String> c5 = new Cart<>(3, 1999, "setBlah");

		Cart<Integer, String, String> c6 = new Cart<>(6, "Ann", "setFirst");
		Cart<Integer, String, String> c7 = new Cart<>(7, "Nik", "setLast", 1, TimeUnit.HOURS);

		Cart<Integer, String, Command> c8 = new Cart<>(8, null, Command.CREATE_BUILD, 1, TimeUnit.SECONDS);

		conveyor.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.offer(c2);
		conveyor.offer(c3);
		conveyor.offer(c4);
		conveyor.offer(c6);
		Thread.sleep(100);
		conveyor.setExpirationCollectionInterval(1000, TimeUnit.MILLISECONDS);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		conveyor.offer(c7);
		conveyor.addCommand(c8);
		Thread.sleep(100);
		conveyor.addCommand( new Cart<Integer,String,Command>(6,"",Command.CANCEL_BUILD));
		conveyor.addCommand( new Cart<Integer,String,Command>(7,"",Command.TIMEOUT_BUILD));

		conveyor.offer(c5);
		Thread.sleep(2000);
		System.out.println("COL:"+conveyor.getCollectorSize());
		System.out.println("DEL:"+conveyor.getDelayedQueueSize());
		System.out.println("IN :"+conveyor.getInputQueueSize());
		conveyor.stop();
		Thread.sleep(1000);
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
