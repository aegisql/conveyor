package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.reflection.Label;
import com.aegisql.conveyor.reflection.SimpleConveyor;

public class LoaderPriorityTest {
	
	
	public static class TestStringBuilder implements Supplier<String>{
		String partA;
		String partB;
		List<String> order;
		public TestStringBuilder(List<String> order) {
			this.order = order;
		}
		
		@Label("partA")
		public void setPartA(String partA) {
			this.partA = partA;
			order.add(partA);
		}

		@Label("partB")
		public void setPartB(String partB) {
			this.partB = partB;
			order.add(partB);
		}

		@Override
		public String get() {
			return partA+" "+partB;
		}
	}

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
	public synchronized void testRegularPartPriority() throws InterruptedException, ExecutionException {

		List<String> order = new ArrayList<>();
		
		
		SimpleConveyor<Integer, String> c1 = new SimpleConveyor<>(PriorityBlockingQueue::new,()-> new TestStringBuilder(order));
		c1.resultConsumer(System.out::println).set();
		c1.setReadinessEvaluator(Conveyor.getTesterFor(c1).accepted("partA", "partB"));
		
		c1.part().id(1).label("partA").value("A").place();
		c1.part().id(1).label("partB").value("B").place();
		c1.part().id(2).label("partA").value("C").priority(1).place();
		c1.part().id(2).label("partB").value("D").priority(2).place();

		c1.completeAndStop().get();
		assertNotEquals(Arrays.asList("A","B","C","D"),order);
		
	}

}
