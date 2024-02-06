package com.aegisql.conveyor.poc;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class PeekPocTest {

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
	public void testPreviewArrayPoc() throws InterruptedException, ExecutionException {
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testPreviewArrayPoc");
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
				.<Consumer<TestProd>>when("PEEK", (b,v)->{
					v.accept(b.get());
				})
				);
		c.resultConsumer(bin->{
			System.out.println("READY "+bin);
			assertEquals("A", bin.product.getA());

		}).set();
		
		c.part().id(1).label("A").value("A").place();
		c.part().id(2).label("A").value("A").place();
		c.part().id(3).label("A").value("A").place();
		List<TestProd> r = new ArrayList<TestProd>();
		assertEquals(0, r.size());
		c.part().foreach().label("PEEK").value((Consumer<TestProd>)r::add).place().get();
		assertEquals(3, r.size());
		System.out.println(r);
		c.part().id(1).label("B").value("B").place();
		c.part().id(2).label("B").value("B").place();
		c.part().id(3).label("B").value("B").place().get();
		
	}

	@Test
	public void testPreviewPoc() throws InterruptedException, ExecutionException {
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testPreviewPoc");
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
				.<Consumer<TestProd>>when("PEEK", (b,v)->{
					v.accept(b.get());
				})
				);
		c.resultConsumer(bin->{
			System.out.println("READY "+bin);
			assertEquals("A", bin.product.getA());
			assertEquals("B", bin.product.getB());
		}).set();
		
		c.part().id(1).label("A").value("A").place();
		AtomicReference<TestProd> r = new AtomicReference<TestProd>();
		assertNull(r.get());
		c.part().id(1).label("PEEK").value((Consumer<TestProd>)r::set).place().get();
		assertEquals("A", r.get().getA());
		assertEquals("", r.get().getB());
		assertNotNull(r.get());
		System.out.println(r);
		c.part().id(1).label("B").value("B").place().get();
		
	}

	@Test
	public void testPreviewFuturePoc() throws InterruptedException, ExecutionException {
		Conveyor<Integer,String,TestProd> c = new AssemblingConveyor<>();
		c.setName("testPreviewPoc");
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
				.<Consumer<TestProd>>when("PEEK", (b,v)->{
					v.accept(b.get());
				})
				);
		c.resultConsumer(bin->{
			System.out.println("READY "+bin);
			assertEquals("A", bin.product.getA());
			assertEquals("B", bin.product.getB());
		}).set();
		
		c.part().id(1).label("A").value("A").place();
		
		CompletableFuture<TestProd> r = new CompletableFuture<TestProd>();
		assertFalse(r.isDone());
		c.part().id(1).label("PEEK").value((Consumer<TestProd>)r::complete).place();
		r.get();
		assertEquals("A", r.get().getA());
		assertEquals("", r.get().getB());
		System.out.println(r);
		c.part().id(1).label("B").value("B").place().get();
		
	}

	
	
}
