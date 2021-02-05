package com.aegisql.conveyor;

import com.aegisql.conveyor.reflection.SimpleConveyor;
import com.aegisql.java_path.PathElement;
import org.junit.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class LoaderPriorityTest {

	public static class TestStringBuilder implements Supplier<String>{
		String partA;
		String partB;
		List<String> order;
		public TestStringBuilder(List<String> order) {
			this.order = order;
		}

		@PathElement("partA")
		public void setPartA(String partA) {
			this.partA = partA;
			order.add(partA);
			sleep(100);
		}

		@PathElement("partB")
		public void setPartB(String partB) {
			this.partB = partB;
			order.add(partB);
		}

		@Override
		public String get() {
			return partA+" "+partB;
		}
		private void sleep(long t) {
			try {
				Thread.sleep(t);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
		SimpleConveyor<Integer, String> c1 = new SimpleConveyor<>(()->new PriorityBlockingQueue(),()-> new TestStringBuilder(order));
		c1.resultConsumer(System.out::println).set();
		c1.setReadinessEvaluator(Conveyor.getTesterFor(c1).accepted("partA", "partB"));
		c1.part().id(1).label("partA").value("A").place();
		c1.part().id(1).label("partB").value("B").place();
		c1.part().id(2).label("partA").value("C").priority(1).place();
		c1.part().id(2).label("partB").value("D").priority(2).place();
		c1.completeAndStop().get();
		assertNotEquals(Arrays.asList("A","B","C","D"),order);
	}

	
	@Test
	public synchronized void testMultiKeyPartPriority() throws InterruptedException, ExecutionException {

		List<String> order = new ArrayList<>();
		
		
		SimpleConveyor<Integer, String> c1 = new SimpleConveyor<>(()->new PriorityBlockingQueue(),()-> new TestStringBuilder(order));
		c1.resultConsumer(System.out::println).set();
		c1.setReadinessEvaluator(Conveyor.getTesterFor(c1).accepted("partA", "partB"));
		c1.command().id(1).create().get();
		c1.command().id(2).create().get();
		c1.part().id(1).label("partA").value("A").place();
		c1.part().id(2).label("partA").value("B").place();
		c1.part().foreach(x->true).label("partB").priority(1).value("X").place();
		c1.completeAndStop().get();
		assertNotEquals(Arrays.asList("A","B","X","X"),order);
	}

	@Test
	public synchronized void testStaticPartPriority() throws InterruptedException, ExecutionException {
		List<String> order = new ArrayList<>();
		SimpleConveyor<Integer, String> c1 = new SimpleConveyor<>(()->new PriorityBlockingQueue(),()-> new TestStringBuilder(order));
		c1.resultConsumer(System.out::println).set();
		c1.setReadinessEvaluator(Conveyor.getTesterFor(c1).accepted("partA", "partB"));
		c1.command().suspend().join();
		c1.staticPart().label("partB").priority(-1).value("X").place();
		c1.part().id(1).label("partA").value("A").place();
		c1.part().id(2).label("partA").value("B").place();
		c1.staticPart().label("partB").priority(1).value("Y").place();
		c1.resume();
		c1.completeAndStop().get();
		assertEquals("Y",order.get(2));
		assertEquals("B",order.get(3));
	}

	private static int N_OF_TESTS=20;
	
	@Test
	public void testAccumulationWithoutPriority() throws InterruptedException, ExecutionException {
		List<String> order = new ArrayList<>();
		SimpleConveyor<Integer, String> c1 = new SimpleConveyor<>(()-> new TestStringBuilder(order));
		c1.resultConsumer(bin->{}).set();
		c1.setReadinessEvaluator(Conveyor.getTesterFor(c1).accepted("partA", "partB"));

		for(int i = 0; i < N_OF_TESTS; i++) {
			c1.part().id(i).label("partA").value("A"+i).priority(i).place();
		}
		
		for(int i = N_OF_TESTS-1; i >= 0; i--) {
			c1.part().id(i).label("partB").value("B"+i).priority(i).place();
		}
		
		c1.completeAndStop().get();
		
	}

	
	@Test
	public void testAccumulationWithPriority() throws InterruptedException, ExecutionException {
		List<String> order = new ArrayList<>();
		SimpleConveyor<Integer, String> c1 = new SimpleConveyor<>(()->new PriorityBlockingQueue(),()-> new TestStringBuilder(order));
		c1.resultConsumer(bin->{}).set();
		c1.setReadinessEvaluator(Conveyor.getTesterFor(c1).accepted("partA", "partB"));

		for(int i = 0; i < N_OF_TESTS; i++) {
			c1.part().id(i).label("partA").value("A"+i).priority(i).place();
		}
		
		for(int i = N_OF_TESTS-1; i >= 0; i--) {
			c1.part().id(i).label("partB").value("B"+i).priority(i).place();
		}
		
		c1.completeAndStop().get();
		
	}
	
	
}
