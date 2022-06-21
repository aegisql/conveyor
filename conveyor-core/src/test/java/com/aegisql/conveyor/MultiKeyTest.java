package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderSmart;

public class MultiKeyTest {

	@BeforeClass
	public static void setUpBeforeClass() {
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testMultiSimple() throws InterruptedException, ExecutionException {
		
		AssemblingConveyor<Integer, SmartLabel<UserBuilderSmart>, User> c = new AssemblingConveyor<>();
		c.resultConsumer().first(bin->{}).set();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		SmartLabel<UserBuilderSmart> l2 = SmartLabel.of(UserBuilderSmart::setLast);
		SmartLabel<UserBuilderSmart> l3 = SmartLabel.of(UserBuilderSmart::setYearOfBirth);

		AtomicInteger counter = new AtomicInteger(0);
		
		CompletableFuture<User> f1 = c.build().id(1).supplier(BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3))).createFuture();
		CompletableFuture<User> f2 = c.build().id(2).supplier(BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3))).createFuture();

		SmartLabel<UserBuilderSmart> multi = SmartLabel.of((builder,value)->{
			System.out.println("--- visited --- "+builder.get() + " --- " + value);
			counter.incrementAndGet();
		});

		PartLoader<Integer, SmartLabel<UserBuilderSmart>> loader1 = c.part().id(1);
		PartLoader<Integer, SmartLabel<UserBuilderSmart>> loader2 = c.part().id(2);
		loader1.value("FIRST").label(l1).place();
		loader1.value("LAST").label(l2).place();
		loader2.value("SECOND").label(l1).place();
		c.part().foreach().label(multi).value("TEST").place();
		loader2.value("LAST").label(l2).place();
		
		loader1.value(1999).label(l3).place();
		loader2.value(2001).label(l3).place();
		User u = f1.get();
		assertEquals("FIRST",u.getFirst());
		assertEquals("LAST",u.getLast());
		assertEquals(1999,u.getYearOfBirth());
		assertEquals(2, counter.get());
	}

	@Test
	public void testMultiSimple2() throws InterruptedException, ExecutionException {
		
		AssemblingConveyor<Integer, SmartLabel<UserBuilderSmart>, User> c = new AssemblingConveyor<>();
		c.resultConsumer().first(bin->{}).set();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		SmartLabel<UserBuilderSmart> l2 = SmartLabel.of(UserBuilderSmart::setLast);
		SmartLabel<UserBuilderSmart> l3 = SmartLabel.of(UserBuilderSmart::setYearOfBirth);

		AtomicInteger counter = new AtomicInteger(0);
		
		CompletableFuture<User> f1 = c.build().id(1).supplier(BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3))).createFuture();
		CompletableFuture<User> f2 = c.build().id(2).supplier(BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3))).createFuture();

		SmartLabel<UserBuilderSmart> multi = SmartLabel.of((builder,value)->{
			System.out.println("--- visited --- "+builder.get() + " --- " + value);
			counter.incrementAndGet();
		});

		PartLoader<Integer, SmartLabel<UserBuilderSmart>> loader1 = c.part().id(1);
		PartLoader<Integer, SmartLabel<UserBuilderSmart>> loader2 = c.part().id(2);
		loader1.value("FIRST").label(l1).place();
		loader1.value("LAST").label(l2).place();
		loader2.value("SECOND").label(l1).place();

		c.part().foreach().value("TEST").label(multi).place();

		loader2.value("LAST").label(l2).place();
		
		loader1.value(1999).label(l3).place();
		loader2.value(2001).label(l3).place();
		User u = f1.get();
		assertEquals("FIRST",u.getFirst());
		assertEquals("LAST",u.getLast());
		assertEquals(1999,u.getYearOfBirth());
		assertEquals(2, counter.get());
	}

	@Test
	public void testMultiWithPredicate() throws InterruptedException, ExecutionException {
		
		AssemblingConveyor<Integer, SmartLabel<UserBuilderSmart>, User> c = new AssemblingConveyor<>();
		c.resultConsumer().first(bin->{}).set();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		SmartLabel<UserBuilderSmart> l2 = SmartLabel.of(UserBuilderSmart::setLast);
		SmartLabel<UserBuilderSmart> l3 = SmartLabel.of(UserBuilderSmart::setYearOfBirth);

		AtomicInteger counter = new AtomicInteger(0);
		
		CompletableFuture<User> f1 = c.build().id(1).supplier(BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3))).createFuture();
		CompletableFuture<User> f2 = c.build().id(2).supplier(BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3))).createFuture();

		SmartLabel<UserBuilderSmart> multi = SmartLabel.of((builder,value)->{
			System.out.println("--- visited --- "+builder.get() + " --- " + value);
			counter.incrementAndGet();
		});

		PartLoader<Integer, SmartLabel<UserBuilderSmart>> loader1 = c.part().id(1);
		PartLoader<Integer, SmartLabel<UserBuilderSmart>> loader2 = c.part().id(2);
		loader1.value("FIRST").label(l1).place();
		loader1.value("LAST").label(l2).place();
		loader2.value("SECOND").label(l1).place();
		c.part().label(multi).value("TEST").foreach((key)->{
			return key % 2 == 0;
					}).place();

		loader2.value("LAST").label(l2).place();
		
		loader1.value(1999).label(l3).place();
		loader2.value(2001).label(l3).place();
		User u = f1.get();
		assertEquals("FIRST",u.getFirst());
		assertEquals("LAST",u.getLast());
		assertEquals(1999,u.getYearOfBirth());
		assertEquals(1, counter.get());
	}

	@Test
	public void testMultiWithPredicate2() throws InterruptedException, ExecutionException {
		
		AssemblingConveyor<Integer, SmartLabel<UserBuilderSmart>, User> c = new AssemblingConveyor<>();
		c.resultConsumer().first(bin->{}).set();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		SmartLabel<UserBuilderSmart> l2 = SmartLabel.of(UserBuilderSmart::setLast);
		SmartLabel<UserBuilderSmart> l3 = SmartLabel.of(UserBuilderSmart::setYearOfBirth);

		AtomicInteger counter = new AtomicInteger(0);
		
		CompletableFuture<User> f1 = c.build().id(1).supplier(BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3))).createFuture();
		CompletableFuture<User> f2 = c.build().id(2).supplier(BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3))).createFuture();

		SmartLabel<UserBuilderSmart> multi = SmartLabel.of((builder,value)->{
			System.out.println("--- visited --- "+builder.get() + " --- " + value);
			counter.incrementAndGet();
		});

		PartLoader<Integer, SmartLabel<UserBuilderSmart>> loader1 = c.part().id(1);
		PartLoader<Integer, SmartLabel<UserBuilderSmart>> loader2 = c.part().id(2);
		loader1.value("FIRST").label(l1).place();
		loader1.value("LAST").label(l2).place();
		loader2.value("SECOND").label(l1).place();
		c.part().foreach((key)->{
			return key % 2 == 0;
		}).value("TEST").label(multi).place();

		loader2.value("LAST").label(l2).place();
		
		loader1.value(1999).label(l3).place();
		loader2.value(2001).label(l3).place();
		User u = f1.get();
		assertEquals("FIRST",u.getFirst());
		assertEquals("LAST",u.getLast());
		assertEquals(1999,u.getYearOfBirth());
		assertEquals(1, counter.get());
	}

}
