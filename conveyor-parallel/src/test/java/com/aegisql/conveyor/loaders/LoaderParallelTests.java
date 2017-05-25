package com.aegisql.conveyor.loaders;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.multichannel.UserBuilder;
import com.aegisql.conveyor.multichannel.UserBuilderEvents;
import com.aegisql.conveyor.parallel.KBalancedParallelConveyor;
import com.aegisql.conveyor.parallel.LBalancedParallelConveyor;
import com.aegisql.conveyor.parallel.ParallelConveyor;
import com.aegisql.conveyor.user.User;

public class LoaderParallelTests {

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
	public void testMultiKeyCancelKParallelCommand() throws InterruptedException, ExecutionException {
		ParallelConveyor<Integer, UserBuilderEvents, User> c = new KBalancedParallelConveyor<>(2);
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).set();
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setName("testMultiKeyCancelKParallelCommand");
		CompletableFuture<Boolean> cf1 = c.build().id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().id(3).create();
		CompletableFuture<Boolean> cf4 = c.build().id(4).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		assertTrue(cf4.get());
		
		CompletableFuture<User> f4 = c.future().id(4).get();
		CompletableFuture<User> f3 = c.future().id(3).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f1 = c.future().id(1).get();
		
		assertEquals(2,c.getCollectorSize(0));
		assertEquals(2,c.getCollectorSize(1));
		Thread.sleep(100);
		c.command().foreach().cancel();
		while(! (f4.isDone() && f3.isDone()) ) {
			System.out.println("~3 "+f3);
			System.out.println("~4 "+f4);
			Thread.sleep(10);
		}
		System.out.println("");
		try {
			System.out.println("About to stop 3 "+f3);
			f3.get();
			fail("Not expected future");
		} catch(Exception e) {
//			assertEquals(0,c.getCollectorSize(0));
			assertTrue(c.getCollectorSize(1)<2);
		}
		try {
			System.out.println("About to stop 4 "+f4);
			f4.get();
			fail("Not expected future");
		} catch(Exception e) {
			assertTrue(c.getCollectorSize(0)<2);
//			assertEquals(0,c.getCollectorSize(1));
		}

	}

	@Test
	public void testMultiKeyCancelLParallelCommand() throws InterruptedException, ExecutionException {
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ac = new AssemblingConveyor<>();
		ac.setName("main");
		ac.setDefaultBuilderTimeout(50, TimeUnit.MILLISECONDS);
		ac.scrapConsumer(bin->{
			System.out.println("rejected ac: "+bin);
		}).set();
		ac.resultConsumer().first(LogResult.stdOut(ac)).set();
		ac.setReadinessEvaluator(Conveyor.getTesterFor(ac).accepted(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B));

		assertFalse(ac.isLBalanced());
		ac.acceptLabels(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B);
		assertTrue(ac.isLBalanced());
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ch1.forwardResultTo(ac,UserBuilderEvents.MERGE_A);
		ch1.setReadinessEvaluator(Conveyor.getTesterFor(ac).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST));
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		Conveyor<Integer, UserBuilderEvents, User> ch2 = new KBalancedParallelConveyor<>(3);
		//Conveyor<Integer, UserBuilderEvents, User> ch2 = new AssemblingConveyor<>();
		ch2.setBuilderSupplier(UserBuilder::new);
		ch2.scrapConsumer(bin->{
			System.out.println("rejected ch2: "+bin);
			((Cart)bin.scrap).getFuture().cancel(true);
		}).set();
		
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR);
		ch2.forwardResultTo(ac,UserBuilderEvents.MERGE_B);
		ch2.setReadinessEvaluator(Conveyor.getTesterFor(ac).accepted(UserBuilderEvents.SET_YEAR));
		ch2.setName("CH2");
		
		Conveyor<Integer, UserBuilderEvents, User> pc = new LBalancedParallelConveyor<>(ac,ch1,ch2);
		pc.setBuilderSupplier(UserBuilder::new);
		assertTrue(pc.isLBalanced());
		pc.build().id(1).create();
		pc.build().id(2).create();
		CompletableFuture<Boolean> bf1 = pc.part().id(1).label(UserBuilderEvents.SET_FIRST).value("A").place();
		assertTrue(bf1.get());
		CompletableFuture<Boolean> bf2 = pc.part().id(2).label(UserBuilderEvents.SET_LAST).value("B").place();
		assertTrue(bf2.get());
		
		CompletableFuture<Boolean> bfCancel = pc.command().foreach().cancel();
		assertTrue(bfCancel.get());
	}

	@Test
	public void multyKeyParts() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("multyKeyParts");
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).set();
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		CompletableFuture<Boolean> cf1 = c.build().id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().id(3).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		
		CompletableFuture<User> f1 = c.future().id(1).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f3 = c.future().id(3).get();

		c.part().foreach().label(UserBuilderEvents.SET_FIRST).value("A").place();
		c.part().foreach().label(UserBuilderEvents.SET_LAST).value("B").place();
		c.part().foreach().label(UserBuilderEvents.SET_YEAR).value(2000).place();
		
		User u1 = f1.get();
		User u2 = f2.get();
		User u3 = f3.get();
	}

	@Test
	public void multyKeyPartsWithFilter() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("multyKeyParts");
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).set();
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		CompletableFuture<Boolean> cf1 = c.build().id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().id(3).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		
		CompletableFuture<User> f1 = c.future().id(1).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f3 = c.future().id(3).get();

		c.part().foreach(k->k>1).label(UserBuilderEvents.SET_FIRST).value("A").place();
		c.part().foreach(k->k>1).label(UserBuilderEvents.SET_LAST).value("B").place();
		c.part().foreach(k->k>1).label(UserBuilderEvents.SET_YEAR).value(2000).place();
		
		User u2 = f2.get();
		User u3 = f3.get();
		try {
			User u1 = f1.get();
			fail("Unexpected");
		} catch(Exception e) {
			
		}

		c.stop();
	}

	@Test
	public void testWithConveyor() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("multyKeyParts");
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).andThen(bin->{
			System.out.println("-----");
		}).set();
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		
		
		c.part().id(1).label(UserBuilderEvents.SET_FIRST).value("A").place();
		c.part().id(1).label(UserBuilderEvents.SET_LAST).value("B").place();
		c.part().id(1).label(UserBuilderEvents.SET_YEAR).value(2017).place();
		c.completeAndStop().get();
		
		
	}

	@Test
	public void testWithConveyorAndId() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("multyKeyParts");
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).andThen(bin->{
			System.out.println("-----");
		}).set();
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		
		CompletableFuture<User> f = c.build().id(1).createFuture();
		
		c.resultConsumer().id(1).first(LogResult.stdErr(c)).andThen(bin->{
			System.err.println("+++++");
		}).set();
		
		c.part().id(1).label(UserBuilderEvents.SET_FIRST).value("A").place();
		c.part().id(2).label(UserBuilderEvents.SET_FIRST).value("A2").place();
		c.part().id(1).label(UserBuilderEvents.SET_LAST).value("B").place();
		c.part().id(2).label(UserBuilderEvents.SET_LAST).value("B2").place();
		c.part().id(1).label(UserBuilderEvents.SET_YEAR).value(2017).place();
		c.part().id(2).label(UserBuilderEvents.SET_YEAR).value(2007).place();
		
		User u = f.get();
		assertNotNull(u);
		
		c.completeAndStop().get();
		
		
	}

	@Test
	public void testWithConveyorForeach() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("multyKeyParts");
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).andThen(bin->{
			System.out.println("-----");
		}).set();
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		
		CompletableFuture<User> f = c.build().id(1).createFuture();
		
		
		c.part().id(1).label(UserBuilderEvents.SET_FIRST).value("A").place();
		c.part().id(2).label(UserBuilderEvents.SET_FIRST).value("A2").place();

		c.resultConsumer().foreach().first(LogResult.stdErr(c)).andThen(bin->{
			System.err.println("+++++");
		}).set();

		c.part().id(1).label(UserBuilderEvents.SET_LAST).value("B").place();
		c.part().id(2).label(UserBuilderEvents.SET_LAST).value("B2").place();
		c.part().id(1).label(UserBuilderEvents.SET_YEAR).value(2017).place();
		c.part().id(2).label(UserBuilderEvents.SET_YEAR).value(2007).place();
		
		User u = f.get();
		assertNotNull(u);
		
		c.completeAndStop().get();
		
	}

	@Test
	public void testWithConveyorFilter() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("multyKeyParts");
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).andThen(bin->{
			System.out.println("-----");
		}).set();
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		
		CompletableFuture<User> f = c.build().id(1).createFuture();
		
		
		c.part().id(1).label(UserBuilderEvents.SET_FIRST).value("A").place();
		c.part().id(2).label(UserBuilderEvents.SET_FIRST).value("A2").place();

		c.resultConsumer().foreach(k->k%2==0).first(LogResult.stdErr(c)).andThen(bin->{
			System.err.println("+++++");
		}).set();

		c.part().id(1).label(UserBuilderEvents.SET_LAST).value("B").place();
		c.part().id(2).label(UserBuilderEvents.SET_LAST).value("B2").place();
		c.part().id(1).label(UserBuilderEvents.SET_YEAR).value(2017).place();
		c.part().id(2).label(UserBuilderEvents.SET_YEAR).value(2007).place();
		
		User u = f.get();
		assertNotNull(u);
		
		c.completeAndStop().get();
		
	}

	
}
