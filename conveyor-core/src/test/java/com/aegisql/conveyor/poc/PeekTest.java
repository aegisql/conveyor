package com.aegisql.conveyor.poc;

import com.aegisql.conveyor.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;


public class PeekTest {

	@BeforeAll
	public static void setUpBeforeClass() {
	}

	@AfterAll
	public static void tearDownAfterClass() {
	}

	@BeforeEach
	public void setUp() {
	}

	@AfterEach
	public void tearDown() {
	}

	@Test
	public void testPeek() {
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testPeek");
		c.setBuilderSupplier(TestProdBuilder::new);
		c.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		LabeledValueConsumer<String, Object, TestProdBuilder> lvc = new LabeledValueConsumer<String, Object, TestProdBuilder>() {
			@Override
			public void accept(String label, Object value, TestProdBuilder builder) {
				System.out.println("Unsupported "+label+" = "+value);
			}

		};
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("A", "B"));
		c.setDefaultCartConsumer(lvc
				.<String>when("A", TestProdBuilder::setA)
				.<String>when("B", TestProdBuilder::setB)
				);
		c.resultConsumer(bin->{
			System.out.println("READY "+bin);
			assertEquals("A", bin.product.getA());
			assertEquals("B", bin.product.getB());
		}).set();
		
		c.part().id(1).label("A").value("A").place().join();
		CompletableFuture<ProductBin<Integer, TestProd>> cf = c.command().id(1).peek();
		ProductBin<Integer, TestProd> bin = cf.join();
		assertNotNull(bin);
		assertNotNull(bin.product);
		assertEquals(bin.status, Status.WAITING_DATA);
		System.out.println(bin);
		c.part().id(1).label("B").value("B").place().join();
	}

	@Test
	public void testPeekWithSuspend() {
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testPeek");
		c.setBuilderSupplier(TestProdBuilder::new);
		c.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		LabeledValueConsumer<String, Object, TestProdBuilder> lvc = new LabeledValueConsumer<String, Object, TestProdBuilder>() {
			@Override
			public void accept(String label, Object value, TestProdBuilder builder) {
				System.out.println("Unsupported "+label+" = "+value);
			}

		};
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("A", "B"));
		c.setDefaultCartConsumer(lvc
				.<String>when("A", TestProdBuilder::setA)
				.<String>when("B", TestProdBuilder::setB)
				);
		c.resultConsumer(bin->{
			System.out.println("READY "+bin);
			assertEquals("A", bin.product.getA());
			assertEquals("B", bin.product.getB());
		}).set();
		
		c.part().id(1).label("A").value("A").place().join();
		c.command().suspend().join();
		c.part().id(1).label("B").value("B").place();
		CompletableFuture<ProductBin<Integer, TestProd>> cf = c.command().id(1).peek();
		ProductBin<Integer, TestProd> bin = cf.join();
		assertNotNull(bin);
		assertNotNull(bin.product);
		assertEquals(bin.status, Status.WAITING_DATA);
		assertEquals("",bin.product.getB());
		System.out.println(bin);
		c.resume();
		c.completeAndStop().join();
	}

	@Test
	public void testExtConsumerPeek() {
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testPeek");
		c.setBuilderSupplier(TestProdBuilder::new);
		c.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		LabeledValueConsumer<String, Object, TestProdBuilder> lvc = new LabeledValueConsumer<String, Object, TestProdBuilder>() {
			@Override
			public void accept(String label, Object value, TestProdBuilder builder) {
				System.out.println("Unsupported "+label+" = "+value);
			}

		};
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("A", "B"));
		c.setDefaultCartConsumer(lvc
				.<String>when("A", TestProdBuilder::setA)
				.<String>when("B", TestProdBuilder::setB)
				);
		c.resultConsumer(bin->{
			System.out.println("READY "+bin);
			assertEquals("A", bin.product.getA());
			assertEquals("B", bin.product.getB());
		}).set();
		
		c.part().id(1).label("A").value("A").place().join();
		
		AtomicReference<ProductBin<Integer, TestProd>> r = new AtomicReference<ProductBin<Integer,TestProd>>();
		
		c.command().id(1).peek(r::set).join();
		ProductBin<Integer, TestProd> bin = r.get();
		assertNotNull(bin);
		assertNotNull(bin.product);
		assertEquals(bin.status, Status.WAITING_DATA);
		System.out.println(bin);
		c.part().id(1).label("B").value("B").place().join();
	}

	
	@Test
	public void testExtListConsumerPeek() {
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testPeek");
		c.setBuilderSupplier(TestProdBuilder::new);
		c.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		LabeledValueConsumer<String, Object, TestProdBuilder> lvc = new LabeledValueConsumer<String, Object, TestProdBuilder>() {
			@Override
			public void accept(String label, Object value, TestProdBuilder builder) {
				System.out.println("Unsupported "+label+" = "+value);
			}

		};
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("A", "B"));
		c.setDefaultCartConsumer(lvc
				.<String>when("A", TestProdBuilder::setA)
				.<String>when("B", TestProdBuilder::setB)
				);
		c.resultConsumer(bin->{
			System.out.println("READY "+bin);
		}).set();
		
		c.part().id(1).label("A").value("A").place();
		c.part().id(2).label("A").value("AA").place();
		c.part().id(3).label("A").value("AAA").place().join();

		List<ProductBin<Integer, TestProd>> r = new ArrayList<ProductBin<Integer,TestProd>>();
		
		c.command().foreach().peek(r::add).join();
		ProductBin<Integer, TestProd> bin = r.get(0);
		assertNotNull(bin);
		assertNotNull(bin.product);
		assertEquals(bin.status, Status.WAITING_DATA);
		System.out.println(bin);

		bin = r.get(1);
		assertNotNull(bin);
		assertNotNull(bin.product);
		assertEquals(bin.status, Status.WAITING_DATA);
		System.out.println(bin);

		bin = r.get(2);
		assertNotNull(bin);
		assertNotNull(bin.product);
		assertEquals(bin.status, Status.WAITING_DATA);
		System.out.println(bin);

		c.part().id(1).label("B").value("B").place();
		c.part().id(2).label("B").value("BB").place();
		c.part().id(3).label("B").value("BBB").place().join();
	}

	@Test
	public void testListConsumerPeek() {
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testPeek");
		c.setBuilderSupplier(TestProdBuilder::new);
		c.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		LabeledValueConsumer<String, Object, TestProdBuilder> lvc = new LabeledValueConsumer<String, Object, TestProdBuilder>() {
			@Override
			public void accept(String label, Object value, TestProdBuilder builder) {
				System.out.println("Unsupported "+label+" = "+value);
			}

		};
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("A", "B"));
		c.setDefaultCartConsumer(lvc
				.<String>when("A", TestProdBuilder::setA)
				.<String>when("B", TestProdBuilder::setB)
				);
		c.resultConsumer(bin->{
			System.out.println("READY "+bin);
		}).set();
		
		c.part().id(1).label("A").value("A").place();
		c.part().id(2).label("A").value("AA").place();
		c.part().id(3).label("A").value("AAA").place().join();

		List<ProductBin<Integer, TestProd>> r = c.command().foreach().peek().join();
		ProductBin<Integer, TestProd> bin = r.get(0);
		assertNotNull(bin);
		assertNotNull(bin.product);
		assertEquals(bin.status, Status.WAITING_DATA);
		System.out.println(bin);

		bin = r.get(1);
		assertNotNull(bin);
		assertNotNull(bin.product);
		assertEquals(bin.status, Status.WAITING_DATA);
		System.out.println(bin);

		bin = r.get(2);
		assertNotNull(bin);
		assertNotNull(bin.product);
		assertEquals(bin.status, Status.WAITING_DATA);
		System.out.println(bin);

		c.part().id(1).label("B").value("B").place();
		c.part().id(2).label("B").value("BB").place();
		c.part().id(3).label("B").value("BBB").place().join();
	}

	@Test
	public void testPeekError() {
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testPeek");
		c.setBuilderSupplier(TestProdBuilder::new);
		c.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		LabeledValueConsumer<String, Object, TestProdBuilder> lvc = new LabeledValueConsumer<String, Object, TestProdBuilder>() {
			@Override
			public void accept(String label, Object value, TestProdBuilder builder) {
				System.out.println("Unsupported "+label+" = "+value);
			}

		};
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("A", "B"));
		c.setDefaultCartConsumer(lvc
				.<String>when("A", TestProdBuilder::setA)
				.<String>when("B", TestProdBuilder::setB)
				);
		c.resultConsumer(bin->{
			System.out.println("READY "+bin);
			assertEquals("XA", bin.product.getA());
			assertEquals("B", bin.product.getB());
		}).set();
		
		c.part().id(1).label("A").value("X").place().join();
		CompletableFuture<ProductBin<Integer, TestProd>> cf = c.command().id(1).peek();
		ProductBin<Integer, TestProd> bin = cf.join();
		assertNotNull(bin);
		assertNull(bin.product);
		assertEquals(bin.status, Status.INVALID);
		System.out.println(bin);
		c.part().id(1).label("A").value("A").place();
		c.part().id(1).label("B").value("B").place().join();
	}

	@Test
	public void testListConsumerPeekError() {
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testPeek");
		c.setBuilderSupplier(TestProdBuilder::new);
		c.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		LabeledValueConsumer<String, Object, TestProdBuilder> lvc = new LabeledValueConsumer<String, Object, TestProdBuilder>() {
			@Override
			public void accept(String label, Object value, TestProdBuilder builder) {
				System.out.println("Unsupported "+label+" = "+value);
			}

		};
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("A", "B"));
		c.setDefaultCartConsumer(lvc
				.<String>when("A", TestProdBuilder::setA)
				.<String>when("B", TestProdBuilder::setB)
				);
		c.resultConsumer(bin->{
			System.out.println("READY "+bin);
		}).set();
		
		c.part().id(1).label("A").value("A").place();
		c.part().id(2).label("A").value("X").place();
		c.part().id(3).label("A").value("AAA").place().join();

		List<ProductBin<Integer, TestProd>> r = c.command().foreach().peek().join();
		ProductBin<Integer, TestProd> bin = r.get(0);
		assertNotNull(bin);
		assertNotNull(bin.product);
		assertEquals(bin.status, Status.WAITING_DATA);
		System.out.println(bin);

		bin = r.get(1);
		assertNotNull(bin);
		assertNull(bin.product);
		assertEquals(bin.status, Status.INVALID);
		System.out.println(bin);

		bin = r.get(2);
		assertNotNull(bin);
		assertNotNull(bin.product);
		assertEquals(bin.status, Status.WAITING_DATA);
		System.out.println(bin);

		c.part().id(1).label("B").value("B").place();
		c.part().id(2).label("A").value("A").place();
		c.part().id(2).label("B").value("BB").place();
		c.part().id(3).label("B").value("BBB").place().join();
	}

	
}
