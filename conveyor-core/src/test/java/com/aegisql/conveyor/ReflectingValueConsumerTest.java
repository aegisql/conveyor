package com.aegisql.conveyor;

import com.aegisql.conveyor.consumers.result.ObservableResultConsumer;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.loaders.StaticPartLoader;
import com.aegisql.conveyor.reflection.ReflectingValueConsumer;
import com.aegisql.conveyor.reflection.SimpleConveyor;
import com.aegisql.conveyor.utils.BuilderUtils;
import com.aegisql.conveyor.utils.MultiValue;
import com.aegisql.java_path.*;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.aegisql.conveyor.ReflectingValueConsumerTest.PhoneType.*;
import static org.junit.jupiter.api.Assertions.*;

public class ReflectingValueConsumerTest {

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

	public static class A {
		@PathElement("value")
		protected String val;

		public String getVal() {
			return val;
		}

		@PathElement({"VALUE","SET_VALUE"})
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
		protected int x;

		public int getX() {
			return x;
		}

		@PathElement("X")
		private void setX(Integer x) {
			this.x = x;
		}
	}

	public static class C extends B {
		private String hidden;

		public String getHidden() {
			return hidden;
		}

		public C() {}

		public C(String a, int b) {
			val = a;
			x = b;
		}

	}
	
	public static class BE extends B {
		@PathElement("X") //duplicate
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
		@NoPathElement
		public void setVal(Integer val) {
			this.iVal = val;
		}
	}
	
	@Test
	public void testA() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
		vc.setEnablePathCaching(true);
		vc.setPathAlias("setVal","set-val");
		A a = new A();
		vc.accept("setVal", "test", a);
		assertEquals("test",a.getVal());
		vc.accept("set-val", "TEST", a);
		assertEquals("TEST",a.getVal());
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

	@Test
	public void testAFailure() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	A a = new A();
	assertThrows(RuntimeException.class,()->vc.accept("setValue", "test", a));//no value setter
	}

	@Test
	public void testEmptyLabelFailure() {
		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		A a = new A();
		assertThrows(ConveyorRuntimeException.class,()->vc.accept("", "test", a));//no empty setter
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
	vc.accept("setVal(#)", "test", a);
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
	public void testAWithNullLabeled() {
		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		A a = new A();
		a.setVal("NO");
		assertNotNull(a.getVal());
		vc.accept("VALUE", null, a);
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
		
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("@done"));
		
		Future<String> f = c.build().id(1).createFuture();
		
		c.part().id(1).value("test").label("setVal").place();
		c.part().id(1).value(100).label("setX").place();
		c.part().id(1).value("hide").label("hidden").place();
        c.part().id(1).label("@done").place().get();

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
		c.part().id(1).label("#.ab.a.value").value("test").place();
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

	@Test
	public void testBEWithDuplicatedAnnotation() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	BE a = new BE();
	assertThrows(JavaPathRuntimeException.class,()->vc.accept("value", "test", a));
	}


	@Test
	public void testDup1WithNull() {
	ReflectingValueConsumer vc = new ReflectingValueConsumer();
	DUP1 a = new DUP1();
	a.setVal("NO");
	a.setVal(100);
	assertNotNull(a.getSVal());
	assertNotNull(a.getIVal());
	assertThrows(RuntimeException.class,()->vc.accept("setVal", null, a));
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
    public void testDeep3LabelWithShortClassNames() {
        ReflectingValueConsumer vc = new ReflectingValueConsumer();
        vc.registerClass(AB.class,"$AB");
        vc.registerClass(A.class,"$A");
        ABC abc = new ABC();
        vc.accept("($AB ab).($A a).value","a-test",abc);
        assertNotNull(abc.ab);
        assertNotNull(abc.ab.a);
        assertEquals("a-test",abc.ab.a.val);
    }

    @Test
    public void testDeep3LabelWithLongClassNames() {
        ReflectingValueConsumer vc = new ReflectingValueConsumer();
        ABC abc = new ABC();
        vc.accept("("+AB.class.getName()+" ab).("+A.class.getName()+" a).value","a-test",abc);
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
		vc.accept("append",0,sb);
		assertEquals("X=test0",sb.toString());
	}

	public static class AMap {
		HashMap<String,String> map;
		ArrayList<String> list;
		public void doIt(){}
	}

	@Test
	public void testIdentityGetter() {
		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		AMap aMap = new AMap();
		vc.accept("doIt",null,aMap);
	}

	@Test
	public void testIgnoredLabel() {
		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		AMap aMap = new AMap();
		vc.accept("@label",null,aMap);
	}

	public static class PG {
		public B b;
		public C c;
		public static C staticC;

		@PathElement("b")
		public B getB(String val) {
			this.b = new B();
			b.setVal(val);
			return b;
		}
		@PathElement("c")
		public C getC(String val,String hide) {
			this.c = new C();
			c.setVal(val);
			c.hidden = hide;
			return c;
		}

		@PathElement("sc")
		public static C getStaticC(String val,String hide) {
			PG.staticC = new C();
			staticC.setVal(val);
			staticC.hidden = hide;
			return staticC;
		}

		@PathElement("psc")
		public static C getStaticPC(PG pg, String val,String hide) {
			pg.c = new C();
			pg.c.setVal(val);
			pg.c.hidden = hide;
			return pg.c;
		}

	}

	@Test
	public void parametrizedGetterTest() {
		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		PG pg = new PG();

		B b = new B();
		b.x = 10;
		b.setVal("init");
		vc.accept("b",b,pg);
		assertEquals("init",pg.b.getVal());
		assertEquals(10,pg.b.getX());

		vc.accept("b(test).x",1,pg);
		assertEquals("test",pg.b.getVal());
		assertEquals(1,pg.b.getX());

		vc.accept("c(test,hide).x",2,pg);
		assertEquals("test",pg.c.getVal());
		assertEquals("hide",pg.c.getHidden());
		assertEquals(2,pg.c.getX());

		vc.accept("sc(staticTest,staticHide).x",3,pg);
		assertEquals("staticTest",PG.staticC.getVal());
		assertEquals("staticHide",PG.staticC.getHidden());
		assertEquals(3,PG.staticC.getX());

		vc.accept("psc(#,staticPTest,staticPHide).x",4,pg);
		assertEquals("staticPTest",pg.c.getVal());
		assertEquals("staticPHide",pg.c.getHidden());
		assertEquals(4,pg.c.getX());


	}

	public static class PS{
		HashMap<String,String> map;

		static HashMap<String,String> staticMap;

		public static void setMap(PS b, String key, String val) {
			b.map.put(key,val);
		}

	}

	@Test
	public void parametrizedSetterTest(){
		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		PS ps = new PS();
		vc.accept("map.put(test)","value",ps);
		assertNotNull(ps.map);
		assertEquals("value",ps.map.get("test"));

		vc.accept("map.put(null)",null,ps);
		assertTrue(ps.map.containsKey("null"));
		assertNull(ps.map.get("null"));

		vc.accept("staticMap.put(staticTest)","staticValue",ps);
		assertNotNull(PS.staticMap);
		assertEquals("staticValue",PS.staticMap.get("staticTest"));

		vc.accept("setMap(#,builderTest)","builderValue",ps);
		assertTrue(ps.map.containsKey("builderTest"));
		assertEquals("builderValue",ps.map.get("builderTest"));

		vc.accept("setMap(#,builderNullTest)",null,ps);
		assertTrue(ps.map.containsKey("builderNullTest"));
		assertEquals(null,ps.map.get("builderNullTest"));

	}

	public enum PhoneType{HOME,CELL,WORK}

	static class GS {
		ArrayList<A> list;
		HashMap<PhoneType, List<String>> phones;
		HashMap<String, PhoneType> reversedPhones;
		List<String> values;
	}

	@Test
	public void getterSetterTest() {
		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		GS gs = new GS();
		vc.accept("list.add",new A(),gs);
		vc.accept("list.get(i 0).val","test",gs);
		assertNotNull(gs.list);
		assertNotNull(gs.list.get(0));
		assertEquals("test",gs.list.get(0).val);

	}

	@Test
	public void getterEnumSetterTest() {
		StringConverter<PhoneType> converter = PhoneType::valueOf;

		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		vc.registerStringConverter(PhoneType.class,converter);
		vc.registerClass(PhoneType.class,"PhoneType");

		GS gs = new GS();

		vc.accept("phones.put(PhoneType HOME)",new ArrayList<>(),gs);
		vc.accept("phones.put(PhoneType CELL)",new ArrayList<>(),gs);
		vc.accept("phones.put(PhoneType WORK)",new ArrayList<>(),gs);

		vc.accept("phones.get(PhoneType CELL).add","111-2233",gs);
		vc.accept("phones.get(PhoneType WORK).add","222-3334",gs);
		vc.accept("phones.get(PhoneType WORK).add","222-3335",gs);
		vc.accept("phones.get(PhoneType HOME).add","111-1133",gs);

		assertNotNull(gs.phones);
		assertTrue(gs.phones.containsKey(HOME));
		assertTrue(gs.phones.containsKey(CELL));
		assertTrue(gs.phones.containsKey(WORK));
		assertEquals(1,gs.phones.get(CELL).size());
		assertEquals(1,gs.phones.get(HOME).size());
		assertEquals(2,gs.phones.get(WORK).size());

	}

	@Test
	public void getterEnumSetterMultiValueTest() {

		MultiValue mv = new MultiValue();
		MultiValue home = mv.add(HOME);
		MultiValue cell = mv.add(CELL);
		MultiValue work = mv.add(WORK);

		Function<PhoneType,List> lambda = phoneType -> new ArrayList();

		StringConverter<PhoneType> converter = PhoneType::valueOf;

		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		vc.registerStringConverter(PhoneType.class,converter);
		vc.registerStringConverter(x->new ArrayList<>(),"ArrayList");
		//vc.registerStringConverter("PhoneType",converter);
		vc.registerClass(PhoneType.class,"PhoneType");

		vc.registerClass(ArrayList.class,"ArrayList");

		GS gs = new GS();


		vc.accept("phones.computeIfAbsent($0,$1).add($2)",cell.add(lambda).add("111-2233"),gs);
		vc.accept("phones.computeIfAbsent($0,$1).add($2)",work.add(lambda).add("222-3334"),gs);
		vc.accept("phones.computeIfAbsent($0,$1).add($2)",work.add(lambda).add("222-3335"),gs);
		vc.accept("phones.computeIfAbsent($0,$1).add($2)",home.add(lambda).add("111-1133"),gs);

		assertNotNull(gs.phones);
		assertTrue(gs.phones.containsKey(CELL));
		assertTrue(gs.phones.containsKey(HOME));
		assertTrue(gs.phones.containsKey(WORK));
		assertEquals(1,gs.phones.get(CELL).size());
		assertEquals(1,gs.phones.get(HOME).size());
		assertEquals(2,gs.phones.get(WORK).size());

		vc.accept("reversedPhones.put($1,$0)",home.add("111-1133"),gs);
		assertTrue(gs.reversedPhones.containsKey("111-1133"));

	}


	@Test
	public void getterEnumSetterWithNewTest() {
		StringConverter<PhoneType> converter = PhoneType::valueOf;

		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		vc.registerStringConverter(PhoneType.class,converter);
		vc.registerStringConverter(x->new ArrayList<>(),"ArrayList");
		//vc.registerStringConverter("PhoneType",converter);
		vc.registerClass(PhoneType.class,"PhoneType");

		vc.registerClass(ArrayList.class,"ArrayList");

		GS gs = new GS();


		vc.accept("phones.computeIfAbsent(PhoneType CELL,key->new ArrayList).add","111-2233",gs);
		vc.accept("phones.computeIfAbsent(PhoneType WORK,key->new ArrayList).add","222-3334",gs);
		vc.accept("phones.computeIfAbsent(PhoneType WORK,key->new ArrayList).add","222-3335",gs);
		vc.accept("phones.computeIfAbsent(PhoneType HOME,key->new ArrayList).add(str $)","111-1133",gs);

		assertNotNull(gs.phones);
		assertTrue(gs.phones.containsKey(CELL));
		assertTrue(gs.phones.containsKey(HOME));
		assertTrue(gs.phones.containsKey(WORK));
		assertEquals(1,gs.phones.get(CELL).size());
		assertEquals(1,gs.phones.get(HOME).size());
		assertEquals(2,gs.phones.get(WORK).size());

		vc.accept("reversedPhones.put($,PhoneType HOME)","111-1133",gs);
		assertTrue(gs.reversedPhones.containsKey("111-1133"));

		vc.accept("(ArrayList values).add","111-1133",gs);
		assertEquals(1,gs.values.size());
		assertEquals("111-1133",gs.values.get(0));
	}

	static class PGF {
		C c;
		C c1;
		C c2;
	}
	@Test
	public void parametrizedFieldGetterTest() {
		ReflectingValueConsumer vc = new ReflectingValueConsumer();
		PGF pgf = new PGF();
		vc.accept("c(str val,int 100).hidden","hidden",pgf);
		assertEquals("hidden",pgf.c.getHidden());
		assertEquals(100,pgf.c.getX());
		assertEquals("val",pgf.c.getVal());
		String cClass = C.class.getName();
		vc.accept("("+cClass+" c1(str val1,int 101)).hidden","hidden1",pgf);
		assertEquals("hidden1",pgf.c1.getHidden());
		assertEquals(101,pgf.c1.getX());
		assertEquals("val1",pgf.c1.getVal());

		vc.registerClass(C.class,"$C");
		vc.accept("($C c2(str val2,int 102)).hidden","hidden2",pgf);
		assertEquals("hidden2",pgf.c2.getHidden());
		assertEquals(102,pgf.c2.getX());
		assertEquals("val2",pgf.c2.getVal());
	}

	static class Phones {
		final String firstName;
		final String lastName;
		final Map<PhoneType, Set<String>> phones;
		final Map<String, PhoneType> reversedPhones;

		public Phones(String firstName, String lastName, Map<PhoneType, Set<String>> phones, Map<String, PhoneType> reversedPhones) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.phones = Collections.unmodifiableMap(phones);
			this.reversedPhones = Collections.unmodifiableMap(reversedPhones);
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("Phones{");
			sb.append("firstName='").append(firstName).append('\'');
			sb.append(", lastName='").append(lastName).append('\'');
			sb.append(", phones=").append(phones);
			sb.append(", reversedPhones=").append(reversedPhones);
			sb.append('}');
			return sb.toString();
		}
	}

	public static class PhonesBuilder implements Supplier<Phones>{
		public String firstName;
		public String lastName;
		public Map<PhoneType, Set<String>> phones;
		public Map<String, PhoneType> reversedPhones;

		@Override
		public Phones get() {
			Map<PhoneType, Set<String>> phonesNew = new HashMap<>();
			phonesNew.putAll(phones);
			Map<String, PhoneType> reversedPhonesNew = new HashMap<>() ;
			reversedPhonesNew.putAll(reversedPhones);

			return new Phones(firstName,lastName,phonesNew,reversedPhonesNew);
		}
	}

	@Test
	public void demoPhonesTest() {

		ClassRegistry.registerGlobalStringConverter(PhoneType.class,PhoneType::valueOf);
		ClassRegistry.registerGlobalClass(PhoneType.class,"PhoneType");

		SimpleConveyor<Integer,Phones> sc = new SimpleConveyor<>(PhonesBuilder::new);
		ObservableResultConsumer<Integer, Phones> result = ObservableResultConsumer.of(sc);
		sc.resultConsumer(result).set();

		CompletableFuture<Phones> phonesFuture = result.waitFor(1);

		//label @done has no action inside the builder, but will be accepted by the readiness evaluator
		sc.setReadinessEvaluator(Conveyor.getTesterFor(sc).accepted("@done"));
		//init variables using static parts. Note '@' at the end - meaning 'do nothing with created object'

		StaticPartLoader<String> staticPartLoader = sc.staticPart();
		staticPartLoader.label("(map phones).computeIfAbsent(PhoneType HOME,key->new set).@").place();
		staticPartLoader.label("(map phones).computeIfAbsent(PhoneType WORK,key->new set).@").place();
		staticPartLoader.label("(map phones).computeIfAbsent(PhoneType CELL,key->new set).@").place();
		staticPartLoader.label("(map reversedPhones).@").place();

		PartLoader<Integer, String> loader = sc.part().id(1);
		//Just set first and last name
		loader.label("firstName").value("John").place();
		loader.label("lastName").value("Smith").place();
		//Load phones
		loader.label("phones.get(PhoneType CELL).add").value("111-2233").place();
		loader.label("phones.get(PhoneType WORK).add").value("222-3334").place();
		//made a mistake, remove value
		loader.label("phones.get(PhoneType WORK).remove").value("222-3334").place();
		//add more
		loader.label("phones.get(PhoneType WORK).add").value("222-3335").place();
		loader.label("phones.get(PhoneType WORK).add").value("222-3336").place();
		//type alias 'map' is technically redundant after first use.
		loader.label("phones.get(PhoneType HOME).add").value("111-1133").place();

		//In reversed map phone is the first parameter and passed value
		//We explicitly refer it as a $
		loader.label("reversedPhones.put($,PhoneType CELL)").value("111-2233").place();
		loader.label("reversedPhones.put($,PhoneType WORK)").value("222-3335").place();
		loader.label("reversedPhones.put($,PhoneType WORK)").value("222-3336").place();
		loader.label("reversedPhones.put($,PhoneType HOME)").value("111-1133").place();

		loader.label("@done").place();

		Phones phones = phonesFuture.join();
		assertNotNull(phones);
		System.out.println(phones);
		System.out.println(phones.phones);
		System.out.println(phones.phones.get("WORK"));
		assertFalse(phones.phones.get(WORK).contains("222-3334")); //removed
		assertTrue(phones.phones.get(WORK).contains("222-3335"));
		assertTrue(phones.phones.get(WORK).contains("222-3336"));

		//phones.phones.computeIfAbsent(HOME,kye->new HashSet<>()).add("123-4567");

	}
}
