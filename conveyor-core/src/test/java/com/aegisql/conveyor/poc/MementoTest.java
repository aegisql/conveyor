package com.aegisql.conveyor.poc;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuildingSite.Memento;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.Status;

public class MementoTest {

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
	public void testMemento() throws InterruptedException, ExecutionException {
		AtomicBoolean ready = new AtomicBoolean(false);
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testMemento");
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
			ready.set(true);
		}).set();
		
		c.part().id(1).label("A").value("A").place().join();
		CompletableFuture<Memento> cf = c.command().id(1).memento();
		Memento m = cf.join();
		assertNotNull(m);
		assertNotNull(m.getId());
		System.out.println(m);
		c.command().restore(m).join();
		c.part().id(1).label("B").value("B").place().join();
		assertTrue(ready.get());
	}

	
	@Test
	public void testEmptyFromMemento() throws InterruptedException, ExecutionException {
		AtomicBoolean ready = new AtomicBoolean(false);
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testMemento");
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
			ready.set(true);
		}).set();
		
		c.part().id(1).label("A").value("A").place().join();
		CompletableFuture<Memento> cf = c.command().id(1).memento();
		Memento m = cf.join();
		assertNotNull(m);
		assertNotNull(m.getId());
		System.out.println(m);
		c.command().id(1).cancel();
		c.command().restore(m).join();
		c.part().id(1).label("B").value("B").place().join();
		assertTrue(ready.get());
	}

	
	@Test
	public void testExtConsumerMemento() throws InterruptedException, ExecutionException {
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testMemento");
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
		
		AtomicReference<Memento> r = new AtomicReference<Memento>();
		
		c.command().id(1).memento(r::set).join();
		Memento memento= r.get();
		assertNotNull(memento);
		assertEquals(1,memento.getId());
		System.out.println(memento);
		c.part().id(1).label("B").value("B").place().join();
	}

	
	@Test
	public void testExtListConsumerMemento() throws InterruptedException, ExecutionException {
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

		List<Memento> r = new ArrayList<>();
		
		c.command().foreach().memento(r::add).join();
		Memento memento = r.get(0);
		assertNotNull(memento);
		assertEquals(1,memento.getId());
		System.out.println(memento);

		memento = r.get(1);
		assertNotNull(memento);
		assertEquals(2,memento.getId());
		System.out.println(memento);

		memento = r.get(2);
		assertNotNull(memento);
		assertEquals(3,memento.getId());
		System.out.println(memento);

		c.part().id(1).label("B").value("B").place();
		c.part().id(2).label("B").value("BB").place();
		c.part().id(3).label("B").value("BBB").place().join();
	}

	@Test
	public void testListConsumerMemento() throws InterruptedException, ExecutionException {
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

		List<Memento> r = c.command().foreach().memento().join();
		Memento memento = r.get(0);
		assertNotNull(memento);
		assertEquals(1,memento.getId());
		System.out.println(memento);

		memento = r.get(1);
		assertNotNull(memento);
		assertEquals(2,memento.getId());
		System.out.println(memento);

		memento = r.get(2);
		assertNotNull(memento);
		assertEquals(3,memento.getId());
		System.out.println(memento);

		c.part().id(1).label("B").value("B").place();
		c.part().id(2).label("B").value("BB").place();
		c.part().id(3).label("B").value("BBB").place().join();
	}
	
}
