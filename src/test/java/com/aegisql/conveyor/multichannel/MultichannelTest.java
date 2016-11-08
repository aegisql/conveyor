package com.aegisql.conveyor.multichannel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.utils.parallel.KBalancedParallelConveyor;
import com.aegisql.conveyor.utils.parallel.LBalancedParallelConveyor;

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
	public void testWithParallelConveyor() throws InterruptedException {
		AtomicReference<User> user = new AtomicReference<User>(null);
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.setScrapConsumer(bin->{
			System.out.println("rejected: "+bin);
		});
		ac.setResultConsumer(bin->{
			System.out.println("AC result: "+bin);
			user.set(bin.product);
		});
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});

		assertFalse(ac.isLBalanced());
		ac.acceptLabels(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B);
		assertTrue(ac.isLBalanced());
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ch1.forwardPartialResultTo(UserBuilderEvents.MERGE_A, ac);
		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		AssemblingConveyor<Integer, UserBuilderEvents, User> ch2 = ac.detach();
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR);
		ch2.forwardPartialResultTo(UserBuilderEvents.MERGE_B, ac);
		ch2.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.yearOfBirth != null;
		});
		ch2.setName("CH2");
		assertTrue(ch2.isLBalanced());
		
		Conveyor<Integer, UserBuilderEvents, User> pc = new LBalancedParallelConveyor<>(ac,ch1,ch2);
		assertTrue(pc.isLBalanced());
	
		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		System.out.println("AC  "+ac);
		System.out.println("CH1 "+ch1);
		System.out.println("CH2 "+ch2);
		System.out.println("PC  "+pc);
		
		pc.add(cartA1);
		pc.add(cartA2);
		pc.add(cartB1);
		
		Thread.sleep(100);

		assertNotNull(user.get());
	}

	@Test
	public void testWithParallelConveyorAndDefault() throws InterruptedException {
		AtomicReference<User> user = new AtomicReference<User>(null);
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.setScrapConsumer(bin->{
			System.out.println("rejected: "+bin);
		});
		ac.setResultConsumer(bin->{
			System.out.println("AC result: "+bin);
			user.set(bin.product);
		});
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});

		assertFalse(ac.isLBalanced());
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ch1.forwardPartialResultTo(UserBuilderEvents.MERGE_A, ac);
		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		AssemblingConveyor<Integer, UserBuilderEvents, User> ch2 = ac.detach();
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR);
		ch2.forwardPartialResultTo(UserBuilderEvents.MERGE_B, ac);
		ch2.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.yearOfBirth != null;
		});
		ch2.setName("CH2");
		assertTrue(ch2.isLBalanced());
		
		Conveyor<Integer, UserBuilderEvents, User> pc = new LBalancedParallelConveyor<>(ac,ch1,ch2);
		assertTrue(pc.isLBalanced());
	
		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		System.out.println("AC  "+ac);
		System.out.println("CH1 "+ch1);
		System.out.println("CH2 "+ch2);
		System.out.println("PC  "+pc);
		
		pc.add(cartA1);
		pc.add(cartA2);
		pc.add(cartB1);
		
		Thread.sleep(100);
		assertNotNull(user.get());
		
	}

	
	@Test
	public void testWithParallelConveyorCommonLabels() throws InterruptedException {
		AtomicReference<User> user = new AtomicReference<User>(null);
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.setScrapConsumer(bin->{
			System.out.println("rejected: "+bin);
		});
		ac.setResultConsumer(bin->{
			System.out.println("AC result: "+bin);
			user.set(bin.product);
		});
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});

		assertFalse(ac.isLBalanced());
		ac.acceptLabels(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B,UserBuilderEvents.INFO);
		assertTrue(ac.isLBalanced());
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.INFO);
		ch1.forwardPartialResultTo(UserBuilderEvents.MERGE_A, ac);
		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		AssemblingConveyor<Integer, UserBuilderEvents, User> ch2 = ac.detach();
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR,UserBuilderEvents.INFO);
		ch2.forwardPartialResultTo(UserBuilderEvents.MERGE_B, ac);
		ch2.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.yearOfBirth != null;
		});
		ch2.setName("CH2");
		assertTrue(ch2.isLBalanced());
		
		Conveyor<Integer, UserBuilderEvents, User> pc = new LBalancedParallelConveyor<>(ac,ch1,ch2);
		assertTrue(pc.isLBalanced());
	
		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		ShoppingCart<Integer, String, UserBuilderEvents> info = new ShoppingCart<>(1,"info-cart", UserBuilderEvents.INFO,100,TimeUnit.MILLISECONDS);

		System.out.println("AC  "+ac);
		System.out.println("CH1 "+ch1);
		System.out.println("CH2 "+ch2);
		System.out.println("PC  "+pc);
		
		pc.add(cartA1);
		pc.add(cartA2);
		pc.add(info);
		pc.add(cartB1);
		
		Thread.sleep(100);

		assertNotNull(user.get());
	}


	@Test
	public void testWithDoubleParallelConveyor() throws InterruptedException, ExecutionException {
		AtomicReference<User> user = new AtomicReference<User>(null);
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.setScrapConsumer(bin->{
			System.out.println("rejected: "+bin);
		});
		ac.setResultConsumer(bin->{
			System.out.println("AC result: "+bin);
			user.set(bin.product);
		});
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});

		assertFalse(ac.isLBalanced());
		ac.acceptLabels(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B);
		assertTrue(ac.isLBalanced());
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ch1.forwardPartialResultTo(UserBuilderEvents.MERGE_A, ac);
		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		Conveyor<Integer, UserBuilderEvents, User> ch2 = new KBalancedParallelConveyor<>(3);
		ch2.setBuilderSupplier(UserBuilder::new);
		ch2.setScrapConsumer(bin->{
			System.out.println("rejected: "+bin);
		});
		
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR);
		ch2.forwardPartialResultTo(UserBuilderEvents.MERGE_B, ac);
		ch2.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.yearOfBirth != null;
		});
		ch2.setName("CH2");
		
		Conveyor<Integer, UserBuilderEvents, User> pc = new LBalancedParallelConveyor<>(ac,ch1,ch2);
		assertTrue(pc.isLBalanced());
	
		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		System.out.println("AC  "+ac);
		System.out.println("CH1 "+ch1);
		System.out.println("CH2 "+ch2);
		System.out.println("PC  "+pc);
		
		CompletableFuture<User> f = pc.getFuture(1);
		assertNotNull(f);
		assertFalse(f.isCancelled());
		assertFalse(f.isCompletedExceptionally());
		assertFalse(f.isDone());
		pc.add(cartA1);
		pc.add(cartA2);
		pc.add(cartB1);
		
		Thread.sleep(100);

		assertNotNull(user.get());
		assertFalse(f.isCancelled());
		assertFalse(f.isCompletedExceptionally());
		assertTrue(f.isDone());
		
		User u = f.get();
		assertNotNull(u);
		System.out.println(u);
	}

	
}
