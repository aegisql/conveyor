package com.aegisql.conveyor.loaders;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.multichannel.UserBuilder;
import com.aegisql.conveyor.multichannel.UserBuilderEvents;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.utils.parallel.KBalancedParallelConveyor;
import com.aegisql.conveyor.utils.parallel.LBalancedParallelConveyor;
import com.aegisql.conveyor.utils.parallel.ParallelConveyor;

public class CommandLoaderTest {

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
	public void testSingleKey() {
		long current = System.currentTimeMillis();

		CommandLoader cl0 = new CommandLoader<>(c->{
			System.out.println("Final: "+c);
			return new CompletableFuture();
		});
		
		System.out.println(cl0);

		assertTrue(cl0.creationTime >= current);
		
		current = cl0.creationTime;

		CommandLoader cl1 = cl0.id(1);
		System.out.println(cl1);
		
		assertEquals(1,cl1.key);
		assertEquals(cl0.creationTime,cl1.creationTime);

		CommandLoader cl2et = cl1.expirationTime(current+1000);
		CommandLoader cl2in = cl1.expirationTime(Instant.ofEpochMilli(current+1000));
		CommandLoader cl2ttl = cl1.ttl(1000,TimeUnit.MILLISECONDS);
		CommandLoader cl2dur = cl1.ttl(Duration.ofMillis(1000));
		
		System.out.println(cl2et);
		System.out.println(cl2in);
		System.out.println(cl2ttl);
		System.out.println(cl2dur);
		assertEquals(cl2et.creationTime,cl2in.creationTime);
		assertEquals(cl2et.creationTime,cl2ttl.creationTime);
		assertEquals(cl2et.creationTime,cl2dur.creationTime);

		assertEquals(0,cl2in.ttlMsec);
		assertEquals(0,cl2et.ttlMsec);
		assertEquals(1000,cl2ttl.ttlMsec);
		assertEquals(1000,cl2dur.ttlMsec);

		CompletableFuture cancel = cl2in.cancel();
		assertNotNull(cancel);
		CompletableFuture create = cl2in.create();
		assertNotNull(create);
		CompletableFuture create2 = cl2in.create(BuilderSupplier.of(UserBuilder::new));
		assertNotNull(create2);
		CompletableFuture check = cl2in.check();
		assertNotNull(check);
		CompletableFuture reschedule = cl2in.reschedule();
		assertNotNull(reschedule);
		CompletableFuture timeout = cl2in.timeout();
		assertNotNull(timeout);
	}

	@Test
	public void testMultiKey() {
		long current = System.currentTimeMillis();

		MultiKeyCommandLoader cl0 = new MultiKeyCommandLoader<>(c->{
			System.out.println("Final: "+c);
			return new CompletableFuture();
		});
		
		System.out.println(cl0);

		assertTrue(cl0.creationTime >= current);
		
		current = cl0.creationTime;
		
		MultiKeyCommandLoader cl1 = cl0;
		System.out.println(cl1);
		
		MultiKeyCommandLoader cl2et = cl1.expirationTime(current+1000);
		MultiKeyCommandLoader cl2in = cl1.expirationTime(Instant.ofEpochMilli(current+1000));
		MultiKeyCommandLoader cl2ttl = cl1.ttl(1000,TimeUnit.MILLISECONDS);
		MultiKeyCommandLoader cl2dur = cl1.ttl(Duration.ofMillis(1000));
		
		System.out.println(cl2et);
		System.out.println(cl2in);
		System.out.println(cl2ttl);
		System.out.println(cl2dur);
		assertEquals(cl2et.creationTime,cl2in.creationTime);
		assertEquals(cl2et.creationTime,cl2ttl.creationTime);
		assertEquals(cl2et.creationTime,cl2dur.creationTime);

		assertEquals(0,cl2in.ttlMsec);
		assertEquals(0,cl2et.ttlMsec);
		assertEquals(1000,cl2ttl.ttlMsec);
		assertEquals(1000,cl2dur.ttlMsec);

		CompletableFuture cancel = cl2in.cancel();
		assertNotNull(cancel);
		CompletableFuture reschedule = cl2in.reschedule();
		assertNotNull(reschedule);
		CompletableFuture timeout = cl2in.timeout();
		assertNotNull(timeout);

	}

	@Test
	public void testMultiKeyCancelCommand() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("testMultiKeyCancelCommand");
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setResultConsumer(bin->{
			System.out.println(bin);
		});
		CompletableFuture<Boolean> cf1 = c.build().supplier(UserBuilder::new).id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().supplier(UserBuilder::new).id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().supplier(UserBuilder::new).id(3).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		
		CompletableFuture<User> f1 = c.future().id(1).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f3 = c.future().id(3).get();
		
		assertEquals(3,c.getCollectorSize());
		Thread.sleep(100);
		c.multiKeyCommand().foreach().cancel();
		
		try {
			f3.get();
			fail("Not expected future");
		} catch(Exception e) {
			assertEquals(0,c.getCollectorSize());
		}	
	}

	@Test
	public void testMultiKeyRescheduleCommand() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("testMultiKeyRescheduleCommand");
		c.setBuilderSupplier(UserBuilder::new);
		c.setResultConsumer(bin->{
			System.out.println(bin);
		});
		c.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		CompletableFuture<Boolean> cf1 = c.build().id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().id(3).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		
		CompletableFuture<User> f1 = c.future().id(1).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f3 = c.future().id(3).get();
		
		assertEquals(3,c.getCollectorSize());
		
		c.multiKeyCommand().foreach().ttl(500, TimeUnit.MILLISECONDS).reschedule();

		Thread.sleep(110);
		assertFalse(f1.isDone());
		assertFalse(f2.isDone());
		assertFalse(f3.isDone());
		Thread.sleep(410);
		assertTrue(f1.isDone());
		assertTrue(f2.isDone());
		assertTrue(f3.isDone());
		
	}

	@Test
	public void testMultiKeyTimeoutCommand() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("testMultiKeyTimeoutCommand");
		c.setBuilderSupplier(UserBuilder::new);
		c.setResultConsumer(bin->{
			System.out.println(bin);
		});
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		CompletableFuture<Boolean> cf1 = c.build().id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().id(3).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		
		CompletableFuture<User> f1 = c.future().id(1).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f3 = c.future().id(3).get();
		
		assertEquals(3,c.getCollectorSize());
		
		Thread.sleep(110);
		c.multiKeyCommand().foreach().timeout();
		Thread.sleep(110);

		assertTrue(f1.isDone());
		assertTrue(f2.isDone());
		assertTrue(f3.isDone());
		
	}

	
	
	@Test
	public void testMultiKeyCancelKParallelCommand() throws InterruptedException, ExecutionException {
		ParallelConveyor<Integer, UserBuilderEvents, User> c = new KBalancedParallelConveyor<>(2);
		c.setBuilderSupplier(UserBuilder::new);
		c.setResultConsumer(bin->{
			System.out.println(bin);
		});
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
		c.multiKeyCommand().foreach().cancel();
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
		ac.setScrapConsumer(bin->{
			System.out.println("rejected ac: "+bin);
		});
		ac.setResultConsumer(bin->{
			System.out.println("AC result: "+bin);
		});
		ac.setReadinessEvaluator(Conveyor.getTesterFor(ac).accepted(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B));

		assertFalse(ac.isLBalanced());
		ac.acceptLabels(UserBuilderEvents.MERGE_A,UserBuilderEvents.MERGE_B);
		assertTrue(ac.isLBalanced());
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = ac.detach();
		ch1.acceptLabels(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST);
		ch1.forwardPartialResultTo(UserBuilderEvents.MERGE_A, ac);
		ch1.setReadinessEvaluator(Conveyor.getTesterFor(ac).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST));
		ch1.setName("CH1");

		assertTrue(ch1.isLBalanced());

		Conveyor<Integer, UserBuilderEvents, User> ch2 = new KBalancedParallelConveyor<>(3);
		//Conveyor<Integer, UserBuilderEvents, User> ch2 = new AssemblingConveyor<>();
		ch2.setBuilderSupplier(UserBuilder::new);
		ch2.setScrapConsumer(bin->{
			System.out.println("rejected ch2: "+bin);
			((Cart)bin.scrap).getFuture().cancel(true);
		});
		
		ch2.acceptLabels(UserBuilderEvents.SET_YEAR);
		ch2.forwardPartialResultTo(UserBuilderEvents.MERGE_B, ac);
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
		
		CompletableFuture<Boolean> bfCancel = pc.multiKeyCommand().foreach().cancel();
		assertTrue(bfCancel.get());
	}
	
}
