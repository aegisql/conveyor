package com.aegisql.conveyor.multichannel;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.user.User;

public class MultichannelTest {

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
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.setScrapConsumer(bin->{
			System.out.println("rejected: "+bin);
		});
		ac.setResultConsumer(bin->{
			System.out.println("result: "+bin);
		});
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detachConveyor(UserBuilderEvents.MERGE_A, "ch1", UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});

		
		
		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		ch1.add(cartA1);
		ch1.add(cartA2);
		ac.add(cartB1);
		
		Thread.sleep(100);
		
	}

	@Test
	public void testThreeChannels() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.setScrapConsumer(bin->{
			System.out.println("rejected: "+bin);
		});
		ac.setResultConsumer(bin->{
			System.out.println("result: "+bin);
		});
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detachConveyor(UserBuilderEvents.MERGE_A, "ch1", UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});

		AssemblingConveyor<Integer, UserBuilderEvents, User> ch2 = ac.detachConveyor(UserBuilderEvents.MERGE_B, "ch2", UserBuilderEvents.SET_YEAR);
		ch2.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.yearOfBirth != null;
		});
		
		
		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		ch1.add(cartA1);
		ch1.add(cartA2);
		ch2.add(cartB1);
		
		Thread.sleep(100);
		
	}

	
	@Test(expected=IllegalStateException.class)
	public void testFilter1() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.setScrapConsumer(bin->{
			System.out.println("rejected: "+bin);
		});
		ac.setResultConsumer(bin->{
			System.out.println("result: "+bin);
		});
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detachConveyor(UserBuilderEvents.MERGE_A, "ch1", UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		
		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		//ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		//ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		ac.add(cartA1);
		
	}

	@Test(expected=IllegalStateException.class)
	public void testFilter2() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.setScrapConsumer(bin->{
			System.out.println("rejected: "+bin);
		});
		ac.setResultConsumer(bin->{
			System.out.println("result: "+bin);
		});
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detachConveyor(UserBuilderEvents.MERGE_A, "ch1", UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		
		ShoppingCart<Integer, User, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,new User("John",null,0), UserBuilderEvents.MERGE_A,100,TimeUnit.MILLISECONDS);
		//ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		//ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		ch1.add(cartA1);
		
	}

}
