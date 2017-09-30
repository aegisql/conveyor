package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.consumers.result.LastResultReference;

public class PayloadAccessorTest {
	
	public interface SmartPayload<B> extends SmartLabel<B> {
		@Override
		default Object getPayload(Cart cart) {
			return cart;
		}
	}
	
	public static class A {
		public String a = "A";
	}

	public static class ABuilder implements Supplier<A> {
		String s;
		@Override
		public A get() {
			A a= new A();
			a.a = s;
			return a;
		}
	}
	
	SmartPayload<ABuilder> SET_PAYLOAD = new SmartPayload<ABuilder>() {
		@Override
		public BiConsumer<ABuilder, Object> get() {
			return (b,v)->b.s=((Cart) v).getValue().toString();
		}
	};

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
	public void testSmartPayload() {
		AssemblingConveyor<Integer, SmartPayload<ABuilder>,A> c = new AssemblingConveyor<>();
		LastResultReference<Integer, A> ref = new LastResultReference<>();
		c.setBuilderSupplier(ABuilder::new);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(SET_PAYLOAD));
		c.resultConsumer(ref).set();
		c.part().id(1).label(SET_PAYLOAD).value("X").place().join();
		assertEquals("X", ref.getCurrent().a);
	}

	@Test
	public void testDefaultPayload() {
		AssemblingConveyor<Integer, String,A> c = new AssemblingConveyor<>();
		LastResultReference<Integer, A> ref = new LastResultReference<>();
		c.setBuilderSupplier(ABuilder::new);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(1));
		c.resultConsumer(ref).set();
		c.setCartPayloadAccessor(cart->cart);
		c.setDefaultCartConsumer((l,v,b)->{
			((ABuilder)b).s = ((Cart)v).getValue().toString();
		});
		c.part().id(1).label("SET_PAYLOAD").value("X").place().join();
		assertEquals("X", ref.getCurrent().a);
		
	}
	
}
