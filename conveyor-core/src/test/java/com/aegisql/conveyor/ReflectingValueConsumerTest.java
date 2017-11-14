package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.reflection.ReflectingValueConsumer;
import com.aegisql.conveyor.reflection.SimpleConveyor;

public class ReflectingValueConsumerTest {

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

	public static class A {
		private String val;

		public String getVal() {
			return val;
		}

		public void setVal(String val) {
			this.val = val;
		}
	}

	public static class AS {
		private String val;

		public String getVal() {
			return val;
		}

		public static void setVal(AS b, String val) {
			b.val = val;
		}
	}

	public static class B extends A {
		private int x;

		public int getX() {
			return x;
		}

		private void setX(Integer x) {
			this.x = x;
		}
	}

	public static class C extends B {
		private String hidden;

		public String getHidden() {
			return hidden;
		}

	}

	public static class D extends C implements Supplier<String> {

		@Override
		public String get() {
			return getVal()+" "+getX()+" "+getHidden();
		}

	}

	
	@Test
	public void testA() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	A a = new A();
	vc.accept("setVal", "test", a);
	assertEquals("test",a.getVal());
	}

	@Test
	public void testB() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	B a = new B();
	vc.accept("setVal", "test", a);
	vc.accept("setX", 100, a);
	assertEquals("test",a.getVal());
	assertEquals(100,a.getX());
	}

	@Test(expected=RuntimeException.class)
	public void testAFailure() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	A a = new A();
	vc.accept("setValue", "test", a);
	}

	@Test
	public void testC() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	C a = new C();
	vc.accept("setVal", "test", a);
	vc.accept("setX", 100, a);
	vc.accept("hidden", "found", a);
	assertEquals("test",a.getVal());
	assertEquals(100,a.getX());
	assertEquals("found",a.getHidden());
	}

	@Test
	public void testAS() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	AS a = new AS();
	vc.accept("setVal", "test", a);
	assertEquals("test",a.getVal());
	}

	@Test
	public void testAWithNull() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	A a = new A();
	a.setVal("NO");
	assertNotNull(a.getVal());
	vc.accept("setVal", null, a);
	assertNull(a.getVal());
	}

	@Test
	public void testAWithNullField() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	A a = new A();
	a.setVal("NO");
	assertNotNull(a.getVal());
	vc.accept("val", null, a);
	assertNull(a.getVal());
	}

	@Test
	public void testWithConveyor() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, String> c = new AssemblingConveyor<>();
		
		c.setBuilderSupplier(D::new);
		c.setDefaultCartConsumer(new ReflectingValueConsumer<D>());
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("setVal", "setX","hidden"));
		
		Future<String> f = c.build().id(1).createFuture();
		
		c.part().id(1).value("test").label("setVal").place();
		c.part().id(1).value(100).label("setX").place();
		c.part().id(1).value("hide").label("hidden").place().get();
		
		System.out.println(f.get());
		assertEquals("test 100 hide", f.get());

	}

	
	@Test
	public void testWithSimpleConveyor() throws InterruptedException, ExecutionException {
		SimpleConveyor<Integer,String> c = new SimpleConveyor<>();
		
		c.setBuilderSupplier(D::new);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("setVal", "setX","hidden"));
		
		Future<String> f = c.build().id(1).createFuture();
		
		c.part().id(1).value("test").label("setVal").place();
		c.part().id(1).value(100).label("setX").place();
		c.part().id(1).value("hide").label("hidden").place().get();
		
		System.out.println(f.get());
		assertEquals("test 100 hide", f.get());

	}

}
