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
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderSmart;

public class MultiKeyTest {

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
	public void testMultiSimple() throws InterruptedException, ExecutionException {
		
		AssemblingConveyor<Integer, SmartLabel<UserBuilderSmart>, User> c = new AssemblingConveyor<>();
		c.setResultConsumer(bin->{});
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		SmartLabel<UserBuilderSmart> l2 = SmartLabel.of(UserBuilderSmart::setLast);
		SmartLabel<UserBuilderSmart> l3 = SmartLabel.of(UserBuilderSmart::setYearOfBirth);

		AtomicInteger counter = new AtomicInteger(0);
		
		CompletableFuture<User> f1 = c.createBuildFuture(1,BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3)));
		CompletableFuture<User> f2 = c.createBuildFuture(2,BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3)));

		SmartLabel<UserBuilderSmart> multi = SmartLabel.of((builder,value)->{
			System.out.println("--- visited --- "+builder.get() + " --- " + value);
			counter.incrementAndGet();
		});

		
		c.add(1,"FIRST",l1);
		c.add(1,"LAST",l2);
		c.add(2,"SECOND",l1);
		c.add(new MultiKeyCart<Integer, String, SmartLabel<UserBuilderSmart>>("TEST", multi));
		c.add(2,"LAST",l2);
		
		c.add(1,1999,l3);
		c.add(2,2001,l3);
		User u = f1.get();
		assertEquals("FIRST",u.getFirst());
		assertEquals("LAST",u.getLast());
		assertEquals(1999,u.getYearOfBirth());
		assertEquals(2, counter.get());
	}
	@Test
	public void testMultiWithPredicate() throws InterruptedException, ExecutionException {
		
		AssemblingConveyor<Integer, SmartLabel<UserBuilderSmart>, User> c = new AssemblingConveyor<>();
		c.setResultConsumer(bin->{});
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		SmartLabel<UserBuilderSmart> l2 = SmartLabel.of(UserBuilderSmart::setLast);
		SmartLabel<UserBuilderSmart> l3 = SmartLabel.of(UserBuilderSmart::setYearOfBirth);

		AtomicInteger counter = new AtomicInteger(0);
		
		CompletableFuture<User> f1 = c.createBuildFuture(1,BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3)));
		CompletableFuture<User> f2 = c.createBuildFuture(2,BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3)));

		SmartLabel<UserBuilderSmart> multi = SmartLabel.of((builder,value)->{
			System.out.println("--- visited --- "+builder.get() + " --- " + value);
			counter.incrementAndGet();
		});

		
		c.add(1,"FIRST",l1);
		c.add(1,"LAST",l2);
		c.add(2,"SECOND",l1);
		c.add(new MultiKeyCart<Integer, String, SmartLabel<UserBuilderSmart>>((key)->{
			return key % 2 == 0;
					},"TEST", multi));
		c.add(2,"LAST",l2);
		
		c.add(1,1999,l3);
		c.add(2,2001,l3);
		User u = f1.get();
		assertEquals("FIRST",u.getFirst());
		assertEquals("LAST",u.getLast());
		assertEquals(1999,u.getYearOfBirth());
		assertEquals(1, counter.get());
	}

}
