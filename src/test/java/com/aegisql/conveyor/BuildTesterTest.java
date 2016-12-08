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
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderEvents;
import com.aegisql.conveyor.user.UserBuilderEvents2;
import com.aegisql.conveyor.user.UserBuilderEvents3;
import com.aegisql.conveyor.user.UserBuilderSmart;
import com.aegisql.conveyor.user.UserBuilderTesting;
import com.aegisql.conveyor.user.UserBuilderTestingState;

public class BuildTesterTest {

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
	public void testTrivialCase() {
		BuildTester<String, String, String> bt = new BuildTester<>();
		assertTrue(bt.test(null,null));
	}

	@Test
	public void testAcceptedTimes() {
		BuildTester<String, String, String> bt1 = new BuildTester<String, String, String>().accepted(2);
		BuildTester<String, String, String> bt2 = new BuildTester<String, String, String>().accepted(3);
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, null, null);
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
	}

	@Test
	public void testAcceptedLabel() {
		BuildTester<String, String, String> bt1 = new BuildTester<String, String, String>().accepted("A");
		BuildTester<String, String, String> bt2 = new BuildTester<String, String, String>().accepted("B");
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, new HashMap<String,Integer>(){{
			put("A",1);
			put("C",2);
		}}, null);
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
	}

	@Test
	public void testAcceptedLabelComplex() {
		BuildTester<String, String, String> bt1 = new BuildTester<String, String, String>().accepted("A")
				.andThen( new BuildTester<String, String, String>().accepted("C") )
				.andNot(new BuildTester<String, String, String>().accepted("X"));
		BuildTester<String, String, String> bt2 = new BuildTester<String, String, String>().accepted("B");
		BuildTester<String, String, String> bt3 = new BuildTester<String, String, String>().accepted("E")
				.or(new BuildTester<String, String, String>().accepted("A"));
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, new HashMap<String,Integer>(){{
			put("A",1);
			put("C",2);
		}}, null);
		
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
		assertTrue(bt3.test(st,null));
	}

	@Test
	public void testAcceptedLabel2() {
		BuildTester<String, String, String> bt1 = new BuildTester<String, String, String>().accepted("A","C");
		BuildTester<String, String, String> bt2 = new BuildTester<String, String, String>().accepted("A","B");
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, new HashMap<String,Integer>(){{
			put("A",1);
			put("C",2);
		}}, null);
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
	}

	
	@Test
	public void testAcceptedLabelTimes() {
		BuildTester<String, String, String> bt1 = new BuildTester<String, String, String>().accepted("A",1).accepted("C") ;
		BuildTester<String, String, String> bt2 = new BuildTester<String, String, String>().accepted("C",1);
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, new HashMap<String,Integer>(){{
			put("A",1);
			put("C",2);
		}}, null);
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
	}
	
	@Test
	public void testBasicsSimpleAccept() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res -> {
			System.out.println(res);
		});
		conveyor.setReadinessEvaluator(new BuildTester<Integer, UserBuilderEvents, User>().accepted(3));
		conveyor.setName("User Assembler");

		CompletableFuture<User> f = conveyor.createBuildFuture(1);
		conveyor.add(1,"John",UserBuilderEvents.SET_FIRST);
		conveyor.add(1,"Doe",UserBuilderEvents.SET_LAST);
		conveyor.add(1,2000,UserBuilderEvents.SET_YEAR);
		User u = f.get();
		assertNotNull(u);
	}

	@Test
	public void testBasicsTestingAccept() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents2, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTesting::new);

		conveyor.setResultConsumer(res -> {
			System.out.println(res);
		});
		conveyor.setReadinessEvaluator(new BuildTester<Integer, UserBuilderEvents2, User>().usingBuilderTest(UserBuilderTesting.class));
		conveyor.setName("User Assembler");

		CompletableFuture<User> f = conveyor.createBuildFuture(1);
		conveyor.add(1,"John",UserBuilderEvents2.SET_FIRST);
		conveyor.add(1,"Doe",UserBuilderEvents2.SET_LAST);
		conveyor.add(1,2000,UserBuilderEvents2.SET_YEAR);
		User u = f.get();
		assertNotNull(u);
	}
	@Test
	public void testBasicsTestingStateAccept() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents3, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTestingState::new);

		conveyor.setResultConsumer(res -> {
			System.out.println(res);
		});
		conveyor.setReadinessEvaluator(new BuildTester<Integer, UserBuilderEvents3, User>().andThen((s,b)->{
			System.out.println("--- test state called ---");
			return true;
		} ).usingBuilderTest(UserBuilderTestingState.class));
		conveyor.setName("User Assembler");

		CompletableFuture<User> f = conveyor.createBuildFuture(1);
		conveyor.add(1,"John",UserBuilderEvents3.SET_FIRST);
		conveyor.add(1,"Doe",UserBuilderEvents3.SET_LAST);
		conveyor.add(1,2000,UserBuilderEvents3.SET_YEAR);
		Thread.sleep(100);
		User u = f.get();
		assertNotNull(u);
	}

	
}
