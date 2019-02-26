package com.aegisql.conveyor.parallel;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ConveyorRuntimeException;
import com.aegisql.conveyor.consumers.result.ResultMap;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.loaders.StaticPartLoader;
import com.aegisql.conveyor.reflection.SimpleConveyor;
import org.junit.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PBalancedConvTest {

	static abstract class AbstractConcatBuilder implements Supplier<String> {
		String del = "";
		String first;
		String second;
	}

	static class StringConcatBuilder1 extends AbstractConcatBuilder {
		@Override
		public String get() {
			return first+del+second;
		}
	}

	static class StringConcatBuilder2 extends AbstractConcatBuilder {
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
		
		ConveyorAcceptor<Integer, String, String> t1 = new ConveyorAcceptor<>(c1);
		t1.expectsValue("version", 1);
		ConveyorAcceptor<Integer, String, String> t2 = new ConveyorAcceptor<>(c2);
		t2.expectsValue("version", 2);
		
		PBalancedParallelConveyor<Integer, String, String> pbc = new PBalancedParallelConveyor<>(t1,t2);
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
	public void testPConveyorWithVersionAndABTest() {
		// place results here
		ResultMap<Integer, String> results = new ResultMap<>();
		//V1,A
		SimpleConveyor<Integer, String> c1 = new SimpleConveyor<>(StringConcatBuilder1::new);
		//V1,B
		SimpleConveyor<Integer, String> c2 = new SimpleConveyor<>(StringConcatBuilder2::new);
		//V2,A
		SimpleConveyor<Integer, String> c3 = new SimpleConveyor<>(StringConcatBuilder1::new);
		//V2,B
		SimpleConveyor<Integer, String> c4 = new SimpleConveyor<>(StringConcatBuilder2::new);
		//assign predicates
		ConveyorAcceptor<Integer, String, String> t1 = new ConveyorAcceptor<>(c1);
		t1.expectsValue("version", 1).expectsValue("abtest", "A");
		ConveyorAcceptor<Integer, String, String> t2 = new ConveyorAcceptor<>(c2);
		t2.expectsValue("version", 2).expectsValue("abtest", "A");
		ConveyorAcceptor<Integer, String, String> t3 = new ConveyorAcceptor<>(c3);
		t3.expectsValue("version", 1).expectsValue("abtest", "B");
		ConveyorAcceptor<Integer, String, String> t4 = new ConveyorAcceptor<>(c4);
		t4.expectsValue("version", 2).expectsValue("abtest", "B");
		// wrap conveyors with PBalancedParallelConveyor
		PBalancedParallelConveyor<Integer, String, String> pbc = new PBalancedParallelConveyor<>(t1,t2,t3,t4);
		pbc.setName("testPConveyorDouble");
		pbc.setReadinessEvaluator(Conveyor.getTesterFor(pbc).accepted("first", "second"));
		pbc.resultConsumer(results).set();
		// Obtain and set up loaders
		StaticPartLoader<String> delLoader = pbc.staticPart().label("del");
		PartLoader<Integer, String> v1Loader = pbc.part().addProperty("version", 1);
		PartLoader<Integer, String> v2Loader = pbc.part().addProperty("version", 2);
		// load constants
		delLoader.addProperty("version", 1).addProperty("abtest","A").value(" ").place();
		delLoader.addProperty("version", 1).addProperty("abtest","B").value("-").place();
		delLoader.addProperty("version", 2).addProperty("abtest","A").value(" ").place();
		delLoader.addProperty("version", 2).addProperty("abtest","B").value("-").place();
		// load data and metadata
		v1Loader.id(1).label("first").addProperty("abtest","A").value("A").place();
		v2Loader.id(2).label("first").addProperty("abtest","A").value("X").place();
		v1Loader.id(1).label("second").addProperty("abtest","A").value("B").place();
		v2Loader.id(2).label("second").addProperty("abtest","A").value("Y").place();

		v1Loader.id(3).label("first").addProperty("abtest","B").value("W").place();
		v2Loader.id(4).label("first").addProperty("abtest","B").value("R").place();
		v1Loader.id(3).label("second").addProperty("abtest","B").value("S").place();
		v2Loader.id(4).label("second").addProperty("abtest","B").value("T").place();
		// wait and stop
		pbc.completeAndStop().join();
		System.out.println(results);
		// test results
		assertEquals(4, results.size());
		assertEquals("A B", results.get(1));
		assertEquals("Y X", results.get(2));
		assertEquals("W-S", results.get(3));
		assertEquals("T-R", results.get(4));

	}

	@Test(expected = ConveyorRuntimeException.class)
	public void testFailNotSamePropTest() {
		// place results here
		ResultMap<Integer, String> results = new ResultMap<>();
		//V1,A
		SimpleConveyor<Integer, String> c1 = new SimpleConveyor<>(StringConcatBuilder1::new);
		//V1,B
		SimpleConveyor<Integer, String> c2 = new SimpleConveyor<>(StringConcatBuilder2::new);
		//V2,A
		SimpleConveyor<Integer, String> c3 = new SimpleConveyor<>(StringConcatBuilder1::new);
		//V2,B
		SimpleConveyor<Integer, String> c4 = new SimpleConveyor<>(StringConcatBuilder2::new);
		//assign predicates
		ConveyorAcceptor<Integer, String, String> t1 = new ConveyorAcceptor<>(c1);
		t1.expectsValue("version", 1).expectsValue("abtest", "A");
		ConveyorAcceptor<Integer, String, String> t2 = new ConveyorAcceptor<>(c2);
		t2.expectsValue("version", 2).expectsValue("abtest", "A");
		ConveyorAcceptor<Integer, String, String> t3 = new ConveyorAcceptor<>(c3);
		ConveyorAcceptor<Integer, String, String> t4 = new ConveyorAcceptor<>(c4);
		t4.expectsValue("version", 2).expectsValue("abtest", "B");
		// wrap conveyors with PBalancedParallelConveyor
		PBalancedParallelConveyor<Integer, String, String> pbc = new PBalancedParallelConveyor<>(Arrays.asList(t1, t2, t3, t4));
	}

	@Test(expected = ConveyorRuntimeException.class)
	public void testFailNotSetPropTest() {
		// place results here
		ResultMap<Integer, String> results = new ResultMap<>();
		//V1,A
		SimpleConveyor<Integer, String> c1 = new SimpleConveyor<>(StringConcatBuilder1::new);
		//V1,B
		SimpleConveyor<Integer, String> c2 = new SimpleConveyor<>(StringConcatBuilder2::new);
		//V2,A
		SimpleConveyor<Integer, String> c3 = new SimpleConveyor<>(StringConcatBuilder1::new);
		//V2,B
		SimpleConveyor<Integer, String> c4 = new SimpleConveyor<>(StringConcatBuilder2::new);
		//assign predicates
		ConveyorAcceptor<Integer, String, String> t1 = new ConveyorAcceptor<>(c1);
		ConveyorAcceptor<Integer, String, String> t2 = new ConveyorAcceptor<>(c2);
		ConveyorAcceptor<Integer, String, String> t3 = new ConveyorAcceptor<>(c3);
		ConveyorAcceptor<Integer, String, String> t4 = new ConveyorAcceptor<>(c4);
		// not set, should fail
		PBalancedParallelConveyor<Integer, String, String> pbc = new PBalancedParallelConveyor<>(Arrays.asList(t1, t2, t3, t4));
	}

	@Test(expected = ConveyorRuntimeException.class)
	public void testFailEmptyPropTest() {
		PBalancedParallelConveyor<Integer, String, String> pbc = new PBalancedParallelConveyor<>(new ArrayList<>());
	}

	@Test(expected = NullPointerException.class)
	public void testFailNullPropTest() {
		PBalancedParallelConveyor<Integer, String, String> pbc = new PBalancedParallelConveyor<>((List<ConveyorAcceptor<Integer, String, String>>) null);
	}

	@Test
	public void constructorTest() {
		ConveyorAcceptor<Integer, String, String> t1 = new ConveyorAcceptor<>();
		assertNotNull(t1.getConveyor());
	}

}
