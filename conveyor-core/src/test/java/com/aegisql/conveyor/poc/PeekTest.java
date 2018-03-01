package com.aegisql.conveyor.poc;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.Status;

public class PeekTest {

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
	public void testPeek() throws InterruptedException, ExecutionException {
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
	public void testExtConsumerPeek() throws InterruptedException, ExecutionException {
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
	public void testExtListConsumerPeek() throws InterruptedException, ExecutionException {
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
	public void testListConsumerPeek() throws InterruptedException, ExecutionException {
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

	
}
