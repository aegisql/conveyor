package com.aegisql.conveyor.parallel;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.consumers.result.ResultMap;
import com.aegisql.conveyor.reflection.SimpleConveyor;

public class PBalancedConvTest {
	
	static class StringConcatBuilder1 implements Supplier<String> {
		String del = "";
		String first;
		String second;
		@Override
		public String get() {
			return first+del+second;
		}
	}

	static class StringConcatBuilder2 implements Supplier<String> {
		String del = "";
		String first;
		String second;
		@Override
		public String get() {
			return second+del+first;
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
	public void testConveyors() {
		ResultMap<Integer, String> results = new ResultMap<>();
		SimpleConveyor<Integer, String> c1 = new SimpleConveyor<>(StringConcatBuilder1::new);
		SimpleConveyor<Integer, String> c2 = new SimpleConveyor<>(StringConcatBuilder2::new);
		c1.setReadinessEvaluator(Conveyor.getTesterFor(c1).accepted("first", "second"));
		c2.setReadinessEvaluator(Conveyor.getTesterFor(c2).accepted("first", "second"));
		c1.setName("C1");
		c2.setName("C2");
		c1.resultConsumer(results).set();
		c2.resultConsumer(results).set();
		c1.staticPart().label("del").value(" ").place();
		c2.staticPart().label("del").value(" ").place();
		c1.part().id(1).label("first").value("A").place();
		c2.part().id(2).label("first").value("X").place();
		c1.part().id(1).label("second").value("B").place();
		c2.part().id(2).label("second").value("Y").place();
		c1.completeAndStop().join();
		c2.completeAndStop().join();
		System.out.println(results);
		assertEquals(2, results.size());
		assertEquals("A B", results.get(1));
		assertEquals("Y X", results.get(2));
	}

	@Test
	public void testPConveyorSimple() {
		
		ResultMap<Integer, String> results = new ResultMap<>();
		SimpleConveyor<Integer, String> c1 = new SimpleConveyor<>(StringConcatBuilder1::new);
		SimpleConveyor<Integer, String> c2 = new SimpleConveyor<>(StringConcatBuilder2::new);
		
		CartPropertyTester<Integer, String, String> t1 = new CartPropertyTester<>(c1);
		t1.addKeyPredicate("version", x->x.equals(1));
		CartPropertyTester<Integer, String, String> t2 = new CartPropertyTester<>(c2);
		t2.addKeyPredicate("version", x->x.equals(2));
		
		PBalancedParallelConveyor<Integer, String, String> pbc = new PBalancedParallelConveyor<>(Arrays.asList(t1,t2));
		pbc.setName("testPConveyorSimple");
		pbc.setReadinessEvaluator(Conveyor.getTesterFor(pbc).accepted("first", "second"));
		pbc.resultConsumer(results).set();

		pbc.staticPart().label("del").addProperty("version", 1).value(" ").place();
		pbc.staticPart().label("del").addProperty("version", 2).value(" ").place();

		pbc.part().id(1).label("first").addProperty("version", 1).value("A").place();
		pbc.part().id(2).label("first").addProperty("version", 2).value("X").place();
		pbc.part().id(1).label("second").addProperty("version", 1).value("B").place();
		pbc.part().id(2).label("second").addProperty("version", 2).value("Y").place();
		pbc.completeAndStop().join();
		System.out.println(results);
		assertEquals(2, results.size());
		assertEquals("A B", results.get(1));
		assertEquals("Y X", results.get(2));

	}

	@Test
	public void testPConveyorDouble() {
		
		ResultMap<Integer, String> results = new ResultMap<>();
		
		SimpleConveyor<Integer, String> c1 = new SimpleConveyor<>(StringConcatBuilder1::new);
		SimpleConveyor<Integer, String> c2 = new SimpleConveyor<>(StringConcatBuilder2::new);
		SimpleConveyor<Integer, String> c3 = new SimpleConveyor<>(StringConcatBuilder1::new);
		SimpleConveyor<Integer, String> c4 = new SimpleConveyor<>(StringConcatBuilder2::new);
		
		CartPropertyTester<Integer, String, String> t1 = new CartPropertyTester<>(c1);
		t1.addKeyPredicate("version", x->x.equals(1));
		t1.addKeyPredicate("abtest", x->"A".equals(x));
		
		CartPropertyTester<Integer, String, String> t2 = new CartPropertyTester<>(c2);
		t2.addKeyPredicate("version", x->x.equals(2));
		t2.addKeyPredicate("abtest", x->"A".equals(x));
		
		CartPropertyTester<Integer, String, String> t3 = new CartPropertyTester<>(c3);
		t3.addKeyPredicate("version", x->x.equals(1));
		t3.addKeyPredicate("abtest", x->"B".equals(x));
		
		CartPropertyTester<Integer, String, String> t4 = new CartPropertyTester<>(c4);
		t4.addKeyPredicate("version", x->x.equals(2));
		t4.addKeyPredicate("abtest", x->"B".equals(x));
		
		PBalancedParallelConveyor<Integer, String, String> pbc = new PBalancedParallelConveyor<>(Arrays.asList(t1,t2,t3,t4));
		pbc.setName("testPConveyorDouble");
		pbc.setReadinessEvaluator(Conveyor.getTesterFor(pbc).accepted("first", "second"));
		pbc.resultConsumer(results).set();
		
		pbc.staticPart().label("del").addProperty("version", 1).addProperty("abtest","A").value(" ").place();
		pbc.staticPart().label("del").addProperty("version", 1).addProperty("abtest","B").value("-").place();
		pbc.staticPart().label("del").addProperty("version", 2).addProperty("abtest","A").value(" ").place();
		pbc.staticPart().label("del").addProperty("version", 2).addProperty("abtest","B").value("-").place();


		pbc.part().id(1).label("first").addProperty("version", 1).addProperty("abtest","A").value("A").place();
		pbc.part().id(2).label("first").addProperty("version", 2).addProperty("abtest","A").value("X").place();
		pbc.part().id(1).label("second").addProperty("version", 1).addProperty("abtest","A").value("B").place();
		pbc.part().id(2).label("second").addProperty("version", 2).addProperty("abtest","A").value("Y").place();

		pbc.part().id(3).label("first").addProperty("version", 1).addProperty("abtest","B").value("W").place();
		pbc.part().id(4).label("first").addProperty("version", 2).addProperty("abtest","B").value("R").place();
		pbc.part().id(3).label("second").addProperty("version", 1).addProperty("abtest","B").value("S").place();
		pbc.part().id(4).label("second").addProperty("version", 2).addProperty("abtest","B").value("T").place();

		pbc.completeAndStop().join();
		System.out.println(results);
		assertEquals(4, results.size());
		assertEquals("A B", results.get(1));
		assertEquals("Y X", results.get(2));
		assertEquals("W-S", results.get(3));
		assertEquals("T-R", results.get(4));

	}

}
