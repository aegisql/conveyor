package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderEvents;
import com.aegisql.conveyor.user.UserBuilderEvents2;
import com.aegisql.conveyor.user.UserBuilderEvents3;
import com.aegisql.conveyor.user.UserBuilderSmart;
import com.aegisql.conveyor.user.UserBuilderTesting;
import com.aegisql.conveyor.user.UserBuilderTestingState;

// TODO: Auto-generated Javadoc
/**
 * The Class BuildTesterTest.
 */
public class BuildTesterTest {

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
	 * Test trivial case.
	 */
	@Test
	public void testTrivialCase() {
		ReadinessTester<String, String, String> bt = new ReadinessTester<>();
		assertTrue(bt.test(null,null));
	}

	/**
	 * Test accepted times.
	 */
	@Test
	public void testAcceptedTimes() {
		ReadinessTester<String, String, String> bt1 = new ReadinessTester<String, String, String>().accepted(2);
		ReadinessTester<String, String, String> bt2 = new ReadinessTester<String, String, String>().accepted(3);
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, null, null);
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
	}

	/**
	 * Test accepted label.
	 */
	@Test
	public void testAcceptedLabel() {
		ReadinessTester<String, String, String> bt1 = new ReadinessTester<String, String, String>().accepted("A");
		ReadinessTester<String, String, String> bt2 = new ReadinessTester<String, String, String>().accepted("B");
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, new HashMap<String,Integer>(){{
			put("A",1);
			put("C",2);
		}}, null);
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
	}

	/**
	 * Test accepted label complex.
	 */
	@Test
	public void testAcceptedLabelComplex() {
		ReadinessTester<String, String, String> bt1 = new ReadinessTester<String, String, String>().accepted("A")
				.andThen( new ReadinessTester<String, String, String>().accepted("C") )
				.andNot(new ReadinessTester<String, String, String>().accepted("X"));
		ReadinessTester<String, String, String> bt2 = new ReadinessTester<String, String, String>().accepted("B");
		ReadinessTester<String, String, String> bt3 = new ReadinessTester<String, String, String>().accepted("E")
				.or(new ReadinessTester<String, String, String>().accepted("A"));
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, new HashMap<String,Integer>(){{
			put("A",1);
			put("C",2);
		}}, null);
		
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
		assertTrue(bt3.test(st,null));
	}

	/**
	 * Test accepted label 2.
	 */
	@Test
	public void testAcceptedLabel2() {
		ReadinessTester<String, String, String> bt1 = new ReadinessTester<String, String, String>().accepted("A","C");
		ReadinessTester<String, String, String> bt2 = new ReadinessTester<String, String, String>().accepted("A","B");
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, new HashMap<String,Integer>(){{
			put("A",1);
			put("C",2);
		}}, null);
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
	}

	
	/**
	 * Test accepted label times.
	 */
	@Test
	public void testAcceptedLabelTimes() {
		ReadinessTester<String, String, String> bt1 = new ReadinessTester<String, String, String>().accepted("A",1).accepted("C") ;
		ReadinessTester<String, String, String> bt2 = new ReadinessTester<String, String, String>().accepted("C",1);
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, new HashMap<String,Integer>(){{
			put("A",1);
			put("C",2);
		}}, null);
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
	}
	
	/**
	 * Test basics simple accept.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void testBasicsSimpleAccept() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.resultConsumer().first(res -> {
			System.out.println(res);
		}).set();
		conveyor.setReadinessEvaluator(new ReadinessTester<Integer, UserBuilderEvents, User>().accepted(3));
		conveyor.setName("User Assembler");

		CompletableFuture<User> f = conveyor.build().id(1).createFuture();
		PartLoader<Integer,UserBuilderEvents,?,?,?> loader = conveyor.part().id(1);
		loader.value("John").label(UserBuilderEvents.SET_FIRST).place();
		loader.value("Doe").label(UserBuilderEvents.SET_LAST).place();
		loader.value(2000).label(UserBuilderEvents.SET_YEAR).place();
		User u = f.get();
		assertNotNull(u);
	}

	/**
	 * Test basics testing accept.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void testBasicsTestingAccept() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents2, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTesting::new);

		conveyor.resultConsumer().first(res -> {
			System.out.println(res);
		}).set();
		conveyor.setReadinessEvaluator(new ReadinessTester<Integer, UserBuilderEvents2, User>().usingBuilderTest(UserBuilderTesting.class));
		conveyor.setName("User Assembler");

		CompletableFuture<User> f = conveyor.build().id(1).createFuture();
		PartLoader<Integer,UserBuilderEvents2,?,?,?> loader = conveyor.part().id(1);
		loader.value("John").label(UserBuilderEvents2.SET_FIRST).place();
		loader.value("Doe").label(UserBuilderEvents2.SET_LAST).place();
		loader.value(2000).label(UserBuilderEvents2.SET_YEAR).place();
		User u = f.get();
		assertNotNull(u);
	}
	
	/**
	 * Test basics testing state accept.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void testBasicsTestingStateAccept() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents3, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTestingState::new);

		conveyor.resultConsumer().first(res -> {
			System.out.println(res);
		}).set();
		conveyor.setReadinessEvaluator(new ReadinessTester<Integer, UserBuilderEvents3, User>().andThen((s,b)->{
			System.out.println("--- test state called ---");
			return true;
		} ).usingBuilderTest(UserBuilderTestingState.class));
		conveyor.setName("User Assembler");

		CompletableFuture<User> f = conveyor.build().id(1).createFuture();
		
		PartLoader<Integer,UserBuilderEvents3,?,?,?> loader = conveyor.part().id(1);
		loader.value("John").label(UserBuilderEvents3.SET_FIRST).place();
		loader.value("Doe").label(UserBuilderEvents3.SET_LAST).place();
		loader.value(2000).label(UserBuilderEvents3.SET_YEAR).place();

		Thread.sleep(100);
		User u = f.get();
		assertNotNull(u);
	}

	
}
