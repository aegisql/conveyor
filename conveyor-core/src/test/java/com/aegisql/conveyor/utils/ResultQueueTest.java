package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


// TODO: Auto-generated Javadoc
/**
 * The Class ResultQueueTest.
 */
public class ResultQueueTest {

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
	 * Test queue.
	 */
	@Test
	public void testQueue() {
		ResultQueue<String,User> q = new ResultQueue<>();
		assertEquals(0, q.size());
		ConcurrentLinkedDeque<User> u = q.<ConcurrentLinkedDeque<User>>unwrap();
		assertNotNull(u);
		ProductBin<String, User> b1 = new ProductBin<>(null,"", new User("","",1999), 0, Status.READY, new HashMap<>(), null);
		q.accept(b1);

		assertEquals(1, q.size());
		assertTrue(q.contains(b1.product));
		assertNotNull(q.iterator());
		assertNotNull(q.toArray());
		assertNotNull(q.toArray(new User[]{}));
		User u1 = q.poll();
		assertNotNull(u1);
		assertEquals(0, q.size());

		ProductBin<String, User> b2 = new ProductBin<>(null,"", new User("","",1999), 0, Status.READY, new HashMap<>(),null);
		q.accept(b2);
		assertEquals(1, u.size());
		User u2 = u.poll();
		assertNotNull(u2);
		assertEquals(0, u.size());

		ResultQueue<String,User> q2 = new ResultQueue<>(u);

		ProductBin<String, User> b3 = new ProductBin<>(null,"", new User("a","a",1999), 0, Status.READY, new HashMap<>(), null);
		ProductBin<String, User> b4 = new ProductBin<>(null,"", new User("b","b",1999), 0, Status.READY, new HashMap<>(), null);
		try {
			q2.addAll(Arrays.asList(b3.product,b4.product));
			fail("Must fail");
		} catch (Exception e) {}
		q2.accept(b3);
		q2.accept(b4);
		assertTrue(q2.containsAll(Arrays.asList(b3.product,b4.product)));
		assertTrue(q2.removeAll(Arrays.asList(b3.product,b4.product)));
		q2.accept(b3);
		q2.accept(b4);
		assertTrue(q2.remove(b3.product));
		q2.accept(b3);
		assertTrue(q2.retainAll(Arrays.asList(b3.product)));
		assertNotNull(q2.remove());
		q2.accept(b3);
		assertNotNull(q2.peek());
		assertNotNull(q2.element());
		q2.clear();

		ResultQueue<String,User> q3 = ResultQueue.of(null);
		ResultQueue<String,User> q4 = ResultQueue.of(null,new LinkedList<>());
		ResultQueue<String,User> q5 = ResultQueue.of(null,LinkedList::new);

	}

	@Test
	public void addTest() {
		ResultQueue<String,User> q = new ResultQueue<>();
		assertThrows(RuntimeException.class,()->q.add(new User("","",0)));
	}

	@Test
	public void offerTest() {
		ResultQueue<String,User> q = new ResultQueue<>();
		assertThrows(RuntimeException.class,()->q.offer(new User("","",0)));
	}

	/**
	 * Test simple conveyor.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testSimpleConveyor() throws InterruptedException {
		
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();
		
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilder::new);
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		assertEquals(1000, conveyor.getDefaultBuilderTimeout());
		conveyor.setIdleHeartBeat(Duration.ofMillis(500));
		assertEquals(500,conveyor.getExpirationCollectionIdleInterval());
		conveyor.setDefaultCartConsumer((label, value, builder) -> {
			UserBuilder userBuilder = (UserBuilder) builder;
			switch (label) {
			case "setFirst":
//				System.out.println("1---- "+value);
				userBuilder.setFirst((String) value);
				break;
			case "setLast":
//				System.out.println("2---- "+value);
				userBuilder.setLast((String) value);
				break;
			case "setYearOfBirth":
//				System.out.println("3---- "+value);
				userBuilder.setYearOfBirth((Integer) value);
				break;
			default:
//				System.out.println("E---- "+value);
				throw new RuntimeException("Unknown label " + label);
			}
		});
		conveyor.resultConsumer(outQueue).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		
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
		conveyor.command().id(8).ttl(1,TimeUnit.SECONDS).create(UserBuilder::new);
		Thread.sleep(100);
		conveyor.command().id(6).cancel();
		conveyor.command().id(7).timeout();

		conveyor.place(c5);
		Thread.sleep(2000);
		System.out.println("COL:"+conveyor.getCollectorSize());
		System.out.println("DEL:"+conveyor.getDelayedQueueSize());
		System.out.println("IN :"+conveyor.getInputQueueSize());
		conveyor.stop();
		Thread.sleep(1000);
	}


}
