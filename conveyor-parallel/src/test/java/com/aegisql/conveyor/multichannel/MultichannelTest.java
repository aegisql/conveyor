package com.aegisql.conveyor.multichannel;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.ForwardResult;
import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.parallel.KBalancedParallelConveyor;
import com.aegisql.conveyor.parallel.LBalancedParallelConveyor;
import com.aegisql.conveyor.user.User;

// TODO: Auto-generated Javadoc
/**
 * The Class MultichannelTest.
 */
public class MultichannelTest {

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
	 * Test with parallel conveyor.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testWithParallelConveyor() throws InterruptedException {
		AtomicReference<User> user = new AtomicReference<User>(null);
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(Duration.ofMillis(50));
		ac.scrapConsumer(LogScrap.error(ac)).set();
		ac.resultConsumer().first(bin->{
			System.out.println("AC result: "+bin);
			user.set(bin.product);
		}).set();
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});

		assertFalse(ac.isLBalanced());
		ac.acceptLabels(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B);
		assertTrue(ac.isLBalanced());
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ForwardResult.from(ch1).to(ac).label(UserBuilderEvents.MERGE_A).bind();
		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		AssemblingConveyor<Integer, UserBuilderEvents, User> ch2 = ac.detach();
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR);
		ForwardResult.from(ch2).to(ac).label(UserBuilderEvents.MERGE_B).bind();
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
		
		pc.place(cartA1);
		pc.place(cartA2);
		pc.place(cartB1);
		
		Thread.sleep(100);

		assertNotNull(user.get());
	}

	@Test
	public void testWithParallelConveyorBoundByNames() throws InterruptedException {
		AtomicReference<User> user = new AtomicReference<User>(null);
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(Duration.ofMillis(50));
		ac.scrapConsumer(LogScrap.error(ac)).set();
		ac.resultConsumer().first(bin->{
			System.out.println("AC result: "+bin);
			user.set(bin.product);
		}).set();
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});

		assertFalse(ac.isLBalanced());
		ac.acceptLabels(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B);
		assertTrue(ac.isLBalanced());
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ForwardResult.from(ch1).to(ac).label(UserBuilderEvents.MERGE_A).bind();
		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		AssemblingConveyor<Integer, UserBuilderEvents, User> ch2 = ac.detach();
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR);
		ForwardResult.from(ch2).to(ac).label(UserBuilderEvents.MERGE_B).bind();
		ch2.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.yearOfBirth != null;
		});
		ch2.setName("CH2");
		assertTrue(ch2.isLBalanced());
		
		Conveyor<Integer, UserBuilderEvents, User> pc = new LBalancedParallelConveyor<>("main","CH1","CH2");
		assertTrue(pc.isLBalanced());
	
		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		System.out.println("AC  "+ac);
		System.out.println("CH1 "+ch1);
		System.out.println("CH2 "+ch2);
		System.out.println("PC  "+pc);
		
		pc.place(cartA1);
		pc.place(cartA2);
		pc.place(cartB1);
		
		Thread.sleep(100);

		assertNotNull(user.get());
	}

	/**
	 * Test with parallel conveyor and default.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testWithParallelConveyorAndDefault() throws InterruptedException {
		AtomicReference<User> user = new AtomicReference<User>(null);
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.scrapConsumer(LogScrap.error(ac)).set();
		ac.resultConsumer().first(bin->{
			System.out.println("AC result: "+bin);
			user.set(bin.product);
		}).set();
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});

		assertFalse(ac.isLBalanced());
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ForwardResult.from(ch1).to(ac).label(UserBuilderEvents.MERGE_A).bind();

		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		AssemblingConveyor<Integer, UserBuilderEvents, User> ch2 = ac.detach();
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR);
		ForwardResult.from(ch2).to(ac).label(UserBuilderEvents.MERGE_B).bind();
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
		
		pc.place(cartA1);
		pc.place(cartA2);
		pc.place(cartB1);
		
		Thread.sleep(100);
		assertNotNull(user.get());
		
	}

	
	/**
	 * Test with parallel conveyor common labels.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testWithParallelConveyorCommonLabels() throws InterruptedException {
		AtomicReference<User> user = new AtomicReference<User>(null);
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.scrapConsumer(LogScrap.error(ac)).set();
		ac.resultConsumer().first(bin->{
			System.out.println("AC result: "+bin);
			user.set(bin.product);
		}).set();
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});

		assertFalse(ac.isLBalanced());
		ac.acceptLabels(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B,UserBuilderEvents.INFO);
		assertTrue(ac.isLBalanced());
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.INFO);
		ForwardResult.from(ch1).to(ac).label(UserBuilderEvents.MERGE_A).bind();

		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		AssemblingConveyor<Integer, UserBuilderEvents, User> ch2 = ac.detach();
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR,UserBuilderEvents.INFO);
		ForwardResult.from(ch2).to(ac).label(UserBuilderEvents.MERGE_B).bind();
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
		
		pc.place(cartA1);
		pc.place(cartA2);
		pc.place(info);
		pc.place(cartB1);
		
		Thread.sleep(100);

		assertNotNull(user.get());
	}


	/**
	 * Test with double parallel conveyor.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void testWithDoubleParallelConveyor() throws InterruptedException, ExecutionException {
		AtomicReference<User> user = new AtomicReference<User>(null);
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setBuilderSupplier(UserBuilder::new);
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.scrapConsumer(LogScrap.error(ac)).set();
		ac.resultConsumer().first(bin->{
			System.out.println("AC result: "+bin);
			user.set(bin.product);
		}).set();
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});

		assertFalse(ac.isLBalanced());
		ac.acceptLabels(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B);
		assertTrue(ac.isLBalanced());
		assertFalse(ac.isForwardingResults());
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		assertFalse(ch1.isForwardingResults());
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ForwardResult.from(ch1).to(ac).label(UserBuilderEvents.MERGE_A).bind();

		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		Conveyor<Integer, UserBuilderEvents, User> ch2 = new KBalancedParallelConveyor<>(3);
		ch2.setBuilderSupplier(UserBuilder::new);
		ac.scrapConsumer(LogScrap.error(ac)).set();
		
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR);
		ch2.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.yearOfBirth != null;
		});
		ch2.setName("CH2");
		ForwardResult.from(ch2).to(ac).label(UserBuilderEvents.MERGE_B).bind();
		
		assertFalse(ac.isForwardingResults());
		assertTrue(ch1.isForwardingResults());
		assertTrue(ch2.isForwardingResults());
		
		Conveyor<Integer, UserBuilderEvents, User> pc = new LBalancedParallelConveyor<>(ac,ch1,ch2);
		assertTrue(pc.isLBalanced());
	
		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		System.out.println("AC  "+ac);
		System.out.println("CH1 "+ch1);
		System.out.println("CH2 "+ch2);
		System.out.println("PC  "+pc);
		
		CompletableFuture<User> f = pc.future().id(1).get();
		assertNotNull(f);
		assertFalse(f.isCancelled());
		assertFalse(f.isCompletedExceptionally());
		assertFalse(f.isDone());
		pc.place(cartA1);
		pc.place(cartA2);
		pc.place(cartB1);
		
		Thread.sleep(100);

		assertNotNull(user.get());
		assertFalse(f.isCancelled());
		assertFalse(f.isCompletedExceptionally());
		assertTrue(f.isDone());
		
		User u = f.get();
		assertNotNull(u);
		System.out.println("Future user: "+u);
	}

	
	/**
	 * Test with parallel conveyor build future and supplier.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 * @throws TimeoutException 
	 */
	@Test
	public void testWithParallelConveyorBuildFutureAndSupplier() throws InterruptedException, ExecutionException, TimeoutException {
		AtomicReference<User> user = new AtomicReference<User>(null);
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("MAIN");
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.scrapConsumer(LogScrap.error(ac)).set();
		ac.resultConsumer().first(bin->{
			System.out.println("MAIN result: "+bin);
			user.set(bin.product);
		}).set();
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null && ub.yearOfBirth > 0;
		});

		assertFalse(ac.isLBalanced());
		ac.acceptLabels(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B);
		assertTrue(ac.isLBalanced());
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ForwardResult.from(ch1).to(ac).label(UserBuilderEvents.MERGE_A).bind();
		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		AssemblingConveyor<Integer, UserBuilderEvents, User> ch2 = ac.detach();
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR);
		ForwardResult.from(ch2).to(ac).label(UserBuilderEvents.MERGE_B).bind();
		ch2.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.yearOfBirth != null;
		});
		ch2.setName("CH2");
		assertTrue(ch2.isLBalanced());
		
		LBalancedParallelConveyor<Integer, UserBuilderEvents, User> pc = new LBalancedParallelConveyor<>(ac,ch1,ch2);
		assertTrue(pc.isLBalanced());
	
		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		System.out.println("AC  "+ac);
		System.out.println("CH1 "+ch1);
		System.out.println("CH2 "+ch2);
		System.out.println("PC  "+pc);
		//pc.setBuilderSupplier(UserBuilder::new);
		CompletableFuture<User> f = pc.build().id(1).supplier(UserBuilder::new).createFuture();//.supplier(UserBuilder::new)
		assertFalse(f.isCancelled());
		assertFalse(f.isCompletedExceptionally());
		assertFalse(f.isDone());
		
		pc.place(cartA1);
		pc.place(cartA2);
		pc.place(cartB1);
		
		User u = f.get(1,TimeUnit.SECONDS);
		
		assertNotNull(u);
		System.out.println("Future user: "+u);
		assertEquals(u,user.get());
		assertEquals(3, ac.getCartCounter());
		assertEquals(3, ch1.getCartCounter());
		assertEquals(2, ch2.getCartCounter());
	}

	/**
	 * Test with parallel conveyor build future and default supplier.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 * @throws TimeoutException 
	 */
	@Test
	public void testWithParallelConveyorBuildFutureAndDefaultSupplier() throws InterruptedException, ExecutionException, TimeoutException {
		AtomicReference<User> user = new AtomicReference<User>(null);
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("MAIN");
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.scrapConsumer(LogScrap.error(ac)).set();
		ac.resultConsumer().first(bin->{
			System.out.println("MAIN result: "+bin);
			user.set(bin.product);
		}).set();
		ac.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null && ub.yearOfBirth > 0;
		});

		assertFalse(ac.isLBalanced());
		ac.acceptLabels(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B);
		assertTrue(ac.isLBalanced());
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ForwardResult.from(ch1).to(ac).label(UserBuilderEvents.MERGE_A).bind();
		ch1.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.first != null && ub.last != null;
		});
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		AssemblingConveyor<Integer, UserBuilderEvents, User> ch2 = ac.detach();
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR);
		ForwardResult.from(ch2).to(ac).label(UserBuilderEvents.MERGE_B).bind();
		ch2.setReadinessEvaluator(b->{
			UserBuilder ub = (UserBuilder)b;
			return ub.yearOfBirth != null;
		});
		ch2.setName("CH2");
		assertTrue(ch2.isLBalanced());
		
		LBalancedParallelConveyor<Integer, UserBuilderEvents, User> pc = new LBalancedParallelConveyor<>(ac,ch1,ch2);
		assertTrue(pc.isLBalanced());
	
		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		System.out.println("AC  "+ac);
		System.out.println("CH1 "+ch1);
		System.out.println("CH2 "+ch2);
		System.out.println("PC  "+pc);
		pc.setBuilderSupplier(UserBuilder::new);
		CompletableFuture<User> f = pc.build().id(1).createFuture();//.supplier(UserBuilder::new)
		assertFalse(f.isCancelled());
		assertFalse(f.isCompletedExceptionally());
		assertFalse(f.isDone());
		
		pc.place(cartA1);
		pc.place(cartA2);
		pc.place(cartB1);
		
		User u = f.get(1,TimeUnit.SECONDS);
		
		assertNotNull(u);
		System.out.println("Future user: "+u);
		assertEquals(u,user.get());
		System.out.println("AC  "+ac.getCartCounter()+" "+ac.getAcceptedLabels());
		System.out.println("CH1 "+ch1.getCartCounter()+" "+ch1.getAcceptedLabels());
		System.out.println("CH2 "+ch2.getCartCounter()+" "+ch2.getAcceptedLabels());
		System.out.println("PC  "+pc.getAcceptedLabels());
		assertEquals(3, ac.getCartCounter());
		assertEquals(3, ch1.getCartCounter());
		assertEquals(2, ch2.getCartCounter());
	}
	
	
}
