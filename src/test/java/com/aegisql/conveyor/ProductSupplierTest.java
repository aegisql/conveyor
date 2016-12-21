package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProductSupplierTest {

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
	public void testE() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST E");
		assertFalse(ps instanceof Expireable);
		ps = ps.expires(()->1000);
		assertTrue(ps instanceof Expireable);
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		ps = ps.expires(()->2000);
		assertTrue(ps instanceof Expireable);
		assertEquals(2000,((Expireable)ps).getExpirationTime());
	}

	@Test
	public void testO() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST O");
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.onTimeout(b->{System.out.println(b.get());});
		assertTrue(ps instanceof TimeoutAction);
		((TimeoutAction)ps).onTimeout();
		ps = ps.onTimeout(b->{System.out.println("TEST OTHER");});
		assertTrue(ps instanceof TimeoutAction);
		((TimeoutAction)ps).onTimeout();
	}

	@Test
	public void testT() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST T");
		assertFalse(ps instanceof Testing);
		ps = ps.readyAlgorithm(b->true);
		assertTrue(ps instanceof Testing);
		assertTrue(((Testing)ps).test());
		ps = ps.readyAlgorithm(b->false);
		assertTrue(ps instanceof Testing);
		assertFalse(((Testing)ps).test());
	}

	@Test
	public void testS() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST S");
		assertFalse(ps instanceof TestingState);
		ps = ps.readyAlgorithm((s,b)->true);
		assertTrue(ps instanceof TestingState);
		assertTrue(((TestingState)ps).test(null));
		ps = ps.readyAlgorithm((s,b)->false);
		assertTrue(ps instanceof TestingState);
		assertFalse(((TestingState)ps).test(null));
	}

	@Test
	public void testTS() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST S");
		assertFalse(ps instanceof Testing);
		assertFalse(ps instanceof TestingState);
		//first state
		ps = ps.readyAlgorithm((s,b)->true);
		assertFalse(ps instanceof Testing);
		assertTrue(ps instanceof TestingState);
		assertTrue(((TestingState)ps).test(null));
		//to testing
		ps = ps.readyAlgorithm(b->false);
		assertTrue(ps instanceof Testing);
		assertFalse(ps instanceof TestingState);
		assertFalse(((Testing)ps).test());
		//and back
		ps = ps.readyAlgorithm((s,b)->true);
		assertFalse(ps instanceof Testing);
		assertTrue(ps instanceof TestingState);
		assertTrue(((TestingState)ps).test(null));

	}

	
	@Test
	public void testOf() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST OE");
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.onTimeout((b)->{System.out.println(b.get());}).expires(()->1000);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		assertEquals("TEST OE",ps.get());
		((TimeoutAction)ps).onTimeout();
		ps = ProductSupplier.of(ps);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
	}

	
	@Test
	public void testOE() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST OE");
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.onTimeout((b)->{System.out.println(b.get());}).expires(()->1000);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		assertEquals("TEST OE",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	@Test
	public void testEO() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST EO");
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.expires(()->1000).onTimeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals("TEST EO",ps.get());
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		((TimeoutAction)ps).onTimeout();
	}

	@Test
	public void testET() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST ET");
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof Testing);
		ps = ps.expires(()->1000).readyAlgorithm(b->true);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof Testing);
		assertEquals("TEST ET",ps.get());
		assertEquals(true,((Testing)ps).test());
	}

	@Test
	public void testTE() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST TE");
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof Testing);
		ps = ps.readyAlgorithm(b->true).expires(()->1000);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof Testing);
		assertEquals("TEST TE",ps.get());
		assertEquals(true,((Testing)ps).test());
	}

	@Test
	public void testOT() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST OT");
		assertFalse(ps instanceof Testing);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.onTimeout((b)->{System.out.println(b.get());}).readyAlgorithm((b)->true);
		assertTrue(ps instanceof Testing);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((Testing)ps).test());
		assertEquals("TEST OT",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	@Test
	public void testTO() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST TO");
		assertFalse(ps instanceof Testing);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.readyAlgorithm((b)->true).onTimeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof Testing);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((Testing)ps).test());
		assertEquals("TEST TO",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	@Test
	public void testES() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST ES");
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TestingState);
		ps = ps.expires(()->1000).readyAlgorithm((s,b)->true);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TestingState);
		assertEquals("TEST ES",ps.get());
		assertEquals(true,((TestingState)ps).test(null));
	}

	@Test
	public void testSE() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST SE");
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TestingState);
		ps = ps.readyAlgorithm((s,b)->true).expires(()->1000);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TestingState);
		assertEquals("TEST SE",ps.get());
		assertEquals(true,((TestingState)ps).test(null));
	}

	@Test
	public void testOS() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST OS");
		assertFalse(ps instanceof TestingState);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.onTimeout((b)->{System.out.println(b.get());}).readyAlgorithm((s,b)->true);
		assertTrue(ps instanceof TestingState);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((TestingState)ps).test(null));
		assertEquals("TEST OS",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	@Test
	public void testSO() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST SO");
		assertFalse(ps instanceof TestingState);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.readyAlgorithm((s,b)->true).onTimeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof TestingState);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((TestingState)ps).test(null));
		assertEquals("TEST SO",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	
	@Test
	public void testETO() {
		ProductSupplier<String> ps = ProductSupplier.of(()->"TEST ETO");
		assertFalse(ps instanceof Testing);
		assertFalse(ps instanceof TimeoutAction);
		assertFalse(ps instanceof Expireable);
		ps = ps.expires(()->1000).readyAlgorithm((b)->true).onTimeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof Testing);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((Testing)ps).test());
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		assertEquals("TEST ETO",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	@Test
	public void testESO() {
		Supplier<String> ss = ()->"TEST ESO";
		ProductSupplier<String> ps = ProductSupplier.of(ss);
		assertFalse(ps instanceof TestingState);
		assertFalse(ps instanceof TimeoutAction);
		assertFalse(ps instanceof Expireable);
		ps = ps.expires(()->1000).readyAlgorithm((s,b)->true).onTimeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof TestingState);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((TestingState)ps).test(null));
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		assertEquals("TEST ESO",ps.get());
		((TimeoutAction)ps).onTimeout();
		assertTrue(ss == ps.getSupplier());
	}

	
}
