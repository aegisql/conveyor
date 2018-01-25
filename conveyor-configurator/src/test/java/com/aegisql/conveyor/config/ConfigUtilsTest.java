package com.aegisql.conveyor.config;

import static org.junit.Assert.*;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.ScrapBin.FailureType;
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.config.harness.IntegerSupplier;
import com.aegisql.conveyor.config.harness.NameLabel;
import com.aegisql.conveyor.config.harness.StringSupplier;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.result.ResultCounter;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapCounter;

public class ConfigUtilsTest {

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
	public void testTimeToMillsConverter() {

		Long time = (Long) ConfigUtils.timeToMillsConverter.apply("1");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply("1000000 NANOSECONDS //comment");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply("1000 MICROSECONDS");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);
		
		time = (Long) ConfigUtils.timeToMillsConverter.apply("1 MILLISECONDS");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply("1 SECONDS");
		assertNotNull(time);
		assertEquals(Long.valueOf(1000), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply(" 1 MINUTES ");
		assertNotNull(time);
		assertEquals(Long.valueOf(60*1000), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply(" 1 HOURS ");
		assertNotNull(time);
		assertEquals(Long.valueOf(60*60*1000), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply(" 1 DAYS ");
		assertNotNull(time);
		assertEquals(Long.valueOf(24*60*60*1000), time);

		// FRACTION
		
		time = (Long) ConfigUtils.timeToMillsConverter.apply("1");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply("1000000.1 NANOSECONDS //comment");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply("1000.9 MICROSECONDS");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);
		
		time = (Long) ConfigUtils.timeToMillsConverter.apply("1.1 MILLISECONDS");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply("1.5 SECONDS");
		assertNotNull(time);
		assertEquals(Long.valueOf(1500), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply(" 1.5 MINUTES ");
		assertNotNull(time);
		assertEquals(Long.valueOf(90*1000), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply(" 1.5 HOURS ");
		assertNotNull(time);
		assertEquals(Long.valueOf(90*60*1000), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply(" 1.5 DAYS ");
		assertNotNull(time);
		assertEquals(Long.valueOf(36*60*60*1000), time);

		
		
	}
	
	@Test
	public void statusConverterTest() {
		Status[] s = (Status[]) ConfigUtils.stringToStatusConverter.apply("READY");
		assertNotNull(s);
		assertEquals(1, s.length);
		assertEquals(Status.READY, s[0]);

		s = (Status[]) ConfigUtils.stringToStatusConverter.apply("READY,TIMED_OUT");
		assertNotNull(s);
		assertEquals(2, s.length);
		assertEquals(Status.READY, s[0]);
		assertEquals(Status.TIMED_OUT, s[1]);

	}

	
	
	@Test
	public void builderSupplierSupplierTest() {
		BuilderSupplier bs = (BuilderSupplier) ConfigUtils.stringToBuilderSupplier.apply("new com.aegisql.conveyor.config.harness.StringSupplier('test2')");
		assertNotNull(bs);
		StringSupplier o1 = (StringSupplier) bs.get();
		assertNotNull(o1);
		StringSupplier o2 = (StringSupplier) bs.get();
		assertNotNull(o2);
		assertFalse(o2==o1);
		assertEquals("test2", o1.get());
		assertEquals("test2", o2.get());
	}

	@Test
	public void builderSupplierSupplierTestWithConcurency() {
		BuilderSupplier stringSupplier  = (BuilderSupplier) ConfigUtils.stringToBuilderSupplier.apply("new com.aegisql.conveyor.config.harness.StringSupplier('test2')");
		BuilderSupplier integerSupplier = (BuilderSupplier) ConfigUtils.stringToBuilderSupplier.apply("new com.aegisql.conveyor.config.harness.IntegerSupplier(3)");
		assertNotNull(stringSupplier);
		assertNotNull(integerSupplier);
		StringSupplier s1 = (StringSupplier) stringSupplier.get();
		IntegerSupplier i1 = (IntegerSupplier) integerSupplier.get();
		assertNotNull(s1);
		assertNotNull(i1);
		StringSupplier s2 = (StringSupplier) stringSupplier.get();
		IntegerSupplier i2 = (IntegerSupplier) integerSupplier.get();
		assertNotNull(s2);
		assertNotNull(i2);
		assertFalse(s2==s1);
		assertFalse(i2==i1);
		assertEquals("test2", s1.get());
		assertEquals("test2", s2.get());
		assertEquals(new Integer(3), i1.get());
		assertEquals(new Integer(3), i2.get());
	}

	
	public static ResultCounter rCounter = new ResultCounter<>();
	public static ResultCounter rCounter2 = new ResultCounter<>();
	public static ScrapCounter  sCounter = new ScrapCounter<>();
	
	@Test
	public void resultConsumerSupplierTest() {
		ResultConsumer rc = (ResultConsumer)ConfigUtils.stringToResultConsumerSupplier.apply("new com.aegisql.conveyor.consumers.result.LogResult()");
		assertNotNull(rc);
		rc.accept(new ProductBin(1, "test", 10000, Status.READY, null, null));
	}

	@Test
	public void resultConsumerSupplierTest2() {
		ResultConsumer rc = (ResultConsumer)ConfigUtils.stringToResultConsumerSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.rCounter2");
		assertNotNull(rc);
		assertEquals(0, rCounter2.get());
		rc.accept(null);
		assertEquals(1, rCounter2.get());
	}

	@Test
	public void scrapConsumerSupplierTest() {
		ScrapConsumer rc = (ScrapConsumer)ConfigUtils.stringToScrapConsumerSupplier.apply("new com.aegisql.conveyor.consumers.scrap.LogScrap()");
		assertNotNull(rc);
		rc.accept(new ScrapBin(1, "scrap", "test", null	, FailureType.BUILD_EXPIRED, null, null));
	}

	@Test
	public void scrapConsumerSupplierTest2() {
		ScrapConsumer rc = (ScrapConsumer)ConfigUtils.stringToScrapConsumerSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.sCounter");
		assertNotNull(rc);
		assertEquals(0, sCounter.get());
		rc.accept(null);
		assertEquals(1, sCounter.get());
	}

	public static Consumer<StringSupplier> timeoutAction = ss->{
		System.out.println("timeout "+ss.get());
	};
		
	@Test
	public void testOnTimeoutActionSupplier() {
		Consumer<StringSupplier> ta = (Consumer<StringSupplier>) ConfigUtils.stringToConsumerSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.timeoutAction");
		assertNotNull(ta);
		System.out.println(ta.getClass());
		timeoutAction.accept(new StringSupplier("A"));
		ta.accept(new StringSupplier("B"));
	}

	@Test
	public void testOnTimeoutActionFunctionSupplier() {
		Consumer<StringSupplier> ta = (Consumer<StringSupplier>) ConfigUtils.stringToConsumerSupplier.apply("function(b){print(b)}");
		assertNotNull(ta);
		System.out.println(ta.getClass());
		ta.accept(new StringSupplier("B"));
	}

	public static LabeledValueConsumer<String,String,StringSupplier> lvc = (l,v,ss)->{
		System.out.println("consume "+l+" = "+v+": "+ss.get());
	};

	@Test
	public void labeledValueConsumerTest() {
		LabeledValueConsumer lc = (LabeledValueConsumer)ConfigUtils.stringToLabeledValueConsumerSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.lvc");
		assertNotNull(lc);
		lvc.accept("label","value1",new StringSupplier("A"));
		lc.accept("label","value1",new StringSupplier("B"));
	}

	@Test
	public void labeledValueConsumerFunctionTest() {
		LabeledValueConsumer lc = (LabeledValueConsumer)ConfigUtils.stringToLabeledValueConsumerSupplier.apply("function(l,v,b){print(l,v);com.aegisql.conveyor.config.harness.StringSupplier.first(b,v);print(b)}");
		assertNotNull(lc);
		lc.accept("label","value1",new StringSupplier("B"));
	}

	public static Predicate<StringSupplier> predRE = ss -> {
		System.out.println("PREDICATE TEST "+ss.get());
		return true;
	};

	public static BiPredicate<State, StringSupplier> biPredRE = (state,ss)->{
		System.out.println("BI-PREDICATE TEST "+ss.get());
		return true;
	};
	
	@Test
	public void readinessEvaluatorPredicateTest() {

		System.out.println(Predicate.class.isAssignableFrom(predRE.getClass()));
		System.out.println(Predicate.class.isAssignableFrom(biPredRE.getClass()));

		System.out.println(BiPredicate.class.isAssignableFrom(predRE.getClass()));
		System.out.println(BiPredicate.class.isAssignableFrom(biPredRE.getClass()));
		
		Predicate p = (Predicate)ConfigUtils.stringToReadinessEvaluatorSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.predRE");
		assertNotNull(p);
		assertTrue(p.test(new StringSupplier("A")));
		//conveyor.test2.readinessEvaluator = com.aegisql.conveyor.config.ConfigUtilsTest.biPredRE
		//conveyor.test2.readinessEvaluator = com.aegisql.conveyor.config.ConfigUtilsTest.predRE
	}

	@Test
	public void readinessEvaluatorBiPredicateTest() {
		BiPredicate p = (BiPredicate)ConfigUtils.stringToReadinessEvaluatorSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.biPredRE");
		assertNotNull(p);
		assertTrue(p.test(new State(p, 0, 0, 0, 0, 0, null, null),new StringSupplier("A")));
	}

	@Test
	public void readinessEvaluatorBiPredicateFunctionTest() {
		BiPredicate p = (BiPredicate)ConfigUtils.stringToReadinessEvaluatorSupplier.apply("function(s,b){print(s);return true;}");
		assertNotNull(p);
		assertTrue(p.test(new State(p, 0, 0, 0, 0, 0, null, null),new StringSupplier("A")));
	}

	public static Consumer<Cart> cartValidator1 = cart->{
		System.out.println("cart "+cart);
	};
	public static Consumer<Cart> cartValidator2 = cart->{
		System.out.println("cart "+cart);
	};

	@Test
	public void testCartValidator() {
		Consumer<Cart> ta = (Consumer<Cart>) ConfigUtils.stringToConsumerSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.cartValidator1");
		assertNotNull(ta);
		System.out.println(ta.getClass());
		cartValidator1.accept(new ShoppingCart(1,"value1","label"));
		ta.accept(new ShoppingCart(1,"value1","label"));
	}

	public static Consumer<AcknowledgeStatus> beforeEviction = status->{
		System.out.println("ack status "+status);
	};
	
	public static Consumer<AcknowledgeStatus> acknowledgeAction = status->{
		System.out.println("ack action on "+status);
	};

	public static BiConsumer<Integer,String> beforeReschedule = (k,v)->{
		System.out.println("reschedule "+k+" "+v);
	};

	@Test
	public void testBiconsValidator() {
		BiConsumer ta = (BiConsumer) ConfigUtils.stringToBiConsumerSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.beforeReschedule");
		assertNotNull(ta);
		System.out.println(ta.getClass());
		beforeReschedule.accept(1,"a");
		ta.accept(1,"b");
	}

	@Test
	public void labelArrayTest() {
		Object[] arr = (Object[])ConfigUtils.stringToLabelArraySupplier.apply("'A','B','C'");
		assertNotNull(arr);
		assertEquals(3,arr.length);
		assertEquals("A",arr[0]);
		assertEquals("B",arr[1]);
		assertEquals("C",arr[2]);
	}

	@Test
	public void labelArrayTestObjects() {
		Object[] arr = (Object[])ConfigUtils.stringToLabelArraySupplier.apply("com.aegisql.conveyor.config.harness.NameLabel.FIRST,com.aegisql.conveyor.config.harness.NameLabel.LAST");
		assertNotNull(arr);
		assertEquals(2,arr.length);
		assertEquals(NameLabel.FIRST, arr[0]);
		assertEquals(NameLabel.LAST, arr[1]);
	}

	@Test
	public void testAckActionAndEviction() {
		Consumer<AcknowledgeStatus> ta = (Consumer<AcknowledgeStatus>) ConfigUtils.stringToConsumerSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.acknowledgeAction");
		Consumer<AcknowledgeStatus> ev = (Consumer<AcknowledgeStatus>) ConfigUtils.stringToConsumerSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.beforeEviction");
		assertNotNull(ta);
		assertNotNull(ev);
		System.out.println(ta.getClass());
		acknowledgeAction.accept(new AcknowledgeStatus(1,Status.READY,null));
		ta.accept(new AcknowledgeStatus(1,Status.INVALID,null));
		beforeEviction.accept(new AcknowledgeStatus(1,Status.TIMED_OUT,null));
		ev.accept(new AcknowledgeStatus(1,Status.CANCELED,null));
	}

	public final static Function<Cart,Object> payloadFunction = cart->{
		return "VALUE="+cart.getValue();
	};
	
	@Test
	public void cartPayloadAccessTest() {
		Function<Cart,String> pf = (Function<Cart,String>)ConfigUtils.stringToFunctionSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.payloadFunction");
		assertNotNull(pf);
		Object s1 = payloadFunction.apply(new ShoppingCart(1, "test1", "label"));
		Object s2 = pf.apply(new ShoppingCart(1, "test2", "label"));
		System.out.println(s1);
		System.out.println(s2);
		assertEquals("VALUE=test2", s2);		
	}

	@Test
	public void cartFunctionTest() {
		Function<Cart,String> pf = (Function<Cart,String>)ConfigUtils.stringToFunctionSupplier.apply("function(cart){return 10;}");
		assertNotNull(pf);
		Object s2 = pf.apply(new ShoppingCart(1, "test2", "label"));
		System.out.println(s2);
		assertEquals(10, s2);		
	}
	
	
	@Test
	public void forwardTrioWithDefaultTransformer() {
		Trio t = (Trio)ConfigUtils.stringToForwardTrioSupplier.apply("var label = \"A\"; var name = \"test1\";");
		assertNotNull(t);
		assertEquals("A",t.label);
		assertEquals("test1",t.value1);
		assertNull(t.value2);
	}

	@Test
	public void forwardTrioWithFunctionTransformer() {
		Trio t = (Trio)ConfigUtils.stringToForwardTrioSupplier.apply("var label = \"A\"; var name = \"test1\"; var keyTransformer = function(k){return 'X'+k};");
		assertNotNull(t);
		assertEquals("A",t.label);
		assertEquals("test1",t.value1);
		assertNotNull(t.value2);
		assertTrue(t.value2 instanceof Function);
		Function f = (Function)t.value2;
		
		assertEquals("XA", f.apply("A"));
		
	}

public static ResultConsumer test2PostCreation = bin -> {
	System.out.println("---- TEST2 POST consumer "+bin);
};	
	
@Test
public void testConveyorSupplier() {
	Supplier<Conveyor> s = ConfigUtils.stringToConveyorSupplier.apply("new com.aegisql.conveyor.AssemblingConveyor()");
	assertNotNull(s);
	assertNotNull(s.get());
	
}

}
