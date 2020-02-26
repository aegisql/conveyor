package com.aegisql.conveyor;

import com.aegisql.conveyor.reflection.Label;
import com.aegisql.conveyor.reflection.NoLabel;
import com.aegisql.conveyor.reflection.ReflectingValueConsumer;
import com.aegisql.conveyor.reflection.SimpleConveyor;
import com.aegisql.conveyor.utils.BuilderUtils;
import org.junit.*;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.junit.Assert.*;

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
		@Label("value")
		private String val;

		public String getVal() {
			return val;
		}

		@Label({"VALUE","SET_VALUE"})
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

		@Label("X")
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
	
	public static class BE extends B {
		@Label("X") //duplicate
		private String other;

		public String getOther() {
			return other;
		}

	}

	public static class D extends C implements Supplier<String> {

		@Override
		public String get() {
			return getVal()+" "+getX()+" "+getHidden();
		}

	}

	public static class AB {
		private static A staticA;
		private A a;
		private String other;
		private ArrayList<String> data;

		public A getA() {
			if(a==null) {
				a = new A();
			}
			return a;
		}

		public static A getStaticA() {
			if(staticA == null) {
				staticA = new A();
			}
			return staticA;
		}

		public ArrayList<String> getData() {
			if(data == null){
				data = new ArrayList<>();
			}
			return data;
		}

	}

	public static class ABC {
		AB ab;
	}

	public static class DUP1 {
		private String sVal;
		private Integer iVal;

		public String getSVal() {
			return sVal;
		}
		public Integer getIVal() {
			return iVal;
		}

		public void setVal(String val) {
			this.sVal = val;
		}
		public void setVal(Integer val) {
			this.iVal = val;
		}
	}

	public static class DUP2 {
		private String sVal;
		private Integer iVal;

		public String getSVal() {
			return sVal;
		}
		public Integer getIVal() {
			return iVal;
		}

		public void setVal(String val) {
			this.sVal = val;
		}
		@NoLabel
		public void setVal(Integer val) {
			this.iVal = val;
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
		c.setDefaultCartConsumer(new ReflectingValueConsumer());
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
		SimpleConveyor<Integer,String> c = new SimpleConveyor<>(D::new);
		
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("setVal", "setX","hidden"));
		
		Future<String> f = c.build().id(1).createFuture();
		
		c.part().id(1).value("test").label("setVal").place();
		c.part().id(1).value(100).label("setX").place();
		c.part().id(1).value("hide").label("hidden").place().get();
		
		System.out.println(f.get());
		assertEquals("test 100 hide", f.get());

	}

	@Test
	public void testWithSimpleConveyorAndBuilderUtils() throws InterruptedException, ExecutionException {
		SimpleConveyor<Integer,String> c = new SimpleConveyor<>();
		BuilderUtils
				.wrapBuilderSupplier(c,ABC::new)
				.productSupplier(abc->abc.ab.a.val)
				.tester(abc->abc.ab.a.val != null)
				.setBuilderSupplier();
		Future<String> f = c.build().id(1).createFuture();
		c.part().id(1).label("$.ab.a.value").value("test").place();
		assertEquals("test", f.get());
	}

	@Test
	public void testAWithSetterAnnotation() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	A a = new A();
	vc.accept("VALUE", "test", a);
	assertEquals("test",a.getVal());
	}

	@Test
	public void testAWithFieldAnnotation() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	A a = new A();
	vc.accept("value", "test", a);
	assertEquals("test",a.getVal());
	}
	
	@Test
	public void testBWithSetterLabel() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	B a = new B();
	vc.accept("SET_VALUE", "test", a);
	vc.accept("X", 100, a);
	assertEquals("test",a.getVal());
	assertEquals(100,a.getX());
	}

	@Test(expected=RuntimeException.class)
	public void testBEWithDuplicatedAnnotation() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	BE a = new BE();
	vc.accept("value", "test", a);
	}


	@Test(expected=RuntimeException.class)
	public void testDup1WithNull() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	DUP1 a = new DUP1();
	a.setVal("NO");
	a.setVal(100);
	assertNotNull(a.getSVal());
	assertNotNull(a.getIVal());
	vc.accept("setVal", null, a);
	}

	@Test
	public void testDup2WithNull() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	DUP2 a = new DUP2();
	a.setVal("NO");
	a.setVal(100);
	assertNotNull(a.getSVal());
	assertNotNull(a.getIVal());
	vc.accept("setVal", null, a);
	assertNull(a.getSVal());
	assertNotNull(a.getIVal());
	}

	@Test
	public void testDeepLabel() {
		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		AB ab = new AB();
		vc.accept("other","test",ab);
		assertEquals("test",ab.other);
		vc.accept("a.value","a-test",ab);
		assertNotNull(ab.a);
		assertEquals("a-test",ab.a.val);
		vc.accept("data.add","a",ab);
		vc.accept("data.add","b",ab);
		assertNotNull(ab.data);
		assertEquals(2,ab.data.size());
		assertEquals("a",ab.data.get(0));
		assertEquals("b",ab.data.get(1));
		vc.accept("staticA.value","static-test",ab);
		assertNotNull(AB.staticA);
		assertEquals("static-test",AB.staticA.val);
	}

	@Test
	public void testDeep3Label() {
		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		ABC abc = new ABC();
		vc.accept("ab.a.value","a-test",abc);
		assertNotNull(abc.ab);
		assertNotNull(abc.ab.a);
		assertEquals("a-test",abc.ab.a.val);
	}

	@Test
	public void testDeepLabelWithGetters() {
		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		AB ab = new AB();
		vc.accept("getA.value","a-test",ab);
		assertNotNull(ab.a);
		assertEquals("a-test",ab.a.val);

		vc.accept("getStaticA.value","static-test-2",ab);
		assertNotNull(AB.staticA);
		assertEquals("static-test-2",AB.staticA.val);

		vc.accept("getData.add","a",ab);
		vc.accept("getData.add","b",ab);
		assertNotNull(ab.data);
		assertEquals(2,ab.data.size());
		assertEquals("a",ab.data.get(0));
		assertEquals("b",ab.data.get(1));
	}

	@Test
	public void testWithStringBuilder() {
		StringBuilder sb = new StringBuilder("X=");
		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		vc.accept("append","test",sb);
		assertEquals("X=test",sb.toString());
		vc.accept("append","1",sb); //once started with strings keep sending strings
		assertEquals("X=test1",sb.toString());
		//vc.accept("append",1,sb); //fails. need to find how to fix it?
	}

}
