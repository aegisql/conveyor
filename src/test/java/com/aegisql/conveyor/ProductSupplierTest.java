package com.aegisql.conveyor;

import static org.junit.Assert.*;

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
	public void testOE() {
		ProductSupplier<String> ps = ()->"TEST OE";
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.timeout((b)->{System.out.println(b.get());}).expire(()->1000);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		assertEquals("TEST OE",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	@Test
	public void testEO() {
		ProductSupplier<String> ps = ()->"TEST EO";
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.expire(()->1000).timeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals("TEST EO",ps.get());
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		((TimeoutAction)ps).onTimeout();
	}

	@Test
	public void testET() {
		ProductSupplier<String> ps = ()->"TEST ET";
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof Testing);
		ps = ps.expire(()->1000).testing(b->true);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof Testing);
		assertEquals("TEST ET",ps.get());
		assertEquals(true,((Testing)ps).test());
	}

	@Test
	public void testTE() {
		ProductSupplier<String> ps = ()->"TEST TE";
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof Testing);
		ps = ps.testing(b->true).expire(()->1000);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof Testing);
		assertEquals("TEST TE",ps.get());
		assertEquals(true,((Testing)ps).test());
	}

	@Test
	public void testOT() {
		ProductSupplier<String> ps = ()->"TEST OT";
		assertFalse(ps instanceof Testing);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.timeout((b)->{System.out.println(b.get());}).testing((b)->true);
		assertTrue(ps instanceof Testing);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((Testing)ps).test());
		assertEquals("TEST OT",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	@Test
	public void testTO() {
		ProductSupplier<String> ps = ()->"TEST TO";
		assertFalse(ps instanceof Testing);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.testing((b)->true).timeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof Testing);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((Testing)ps).test());
		assertEquals("TEST TO",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	@Test
	public void testES() {
		ProductSupplier<String> ps = ()->"TEST ES";
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TestingState);
		ps = ps.expire(()->1000).testingState((s,b)->true);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TestingState);
		assertEquals("TEST ES",ps.get());
		assertEquals(true,((TestingState)ps).test(null));
	}

	@Test
	public void testSE() {
		ProductSupplier<String> ps = ()->"TEST SE";
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TestingState);
		ps = ps.testingState((s,b)->true).expire(()->1000);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TestingState);
		assertEquals("TEST SE",ps.get());
		assertEquals(true,((TestingState)ps).test(null));
	}

	@Test
	public void testOS() {
		ProductSupplier<String> ps = ()->"TEST OS";
		assertFalse(ps instanceof TestingState);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.timeout((b)->{System.out.println(b.get());}).testingState((s,b)->true);
		assertTrue(ps instanceof TestingState);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((TestingState)ps).test(null));
		assertEquals("TEST OS",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	@Test
	public void testSO() {
		ProductSupplier<String> ps = ()->"TEST SO";
		assertFalse(ps instanceof TestingState);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.testingState((s,b)->true).timeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof TestingState);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((TestingState)ps).test(null));
		assertEquals("TEST SO",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	
	@Test
	public void testETO() {
		ProductSupplier<String> ps = ()->"TEST ETO";
		assertFalse(ps instanceof Testing);
		assertFalse(ps instanceof TimeoutAction);
		assertFalse(ps instanceof Expireable);
		ps = ps.expire(()->1000).testing((b)->true).timeout((b)->{System.out.println(b.get());});
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
		ProductSupplier<String> ps = ()->"TEST ESO";
		assertFalse(ps instanceof TestingState);
		assertFalse(ps instanceof TimeoutAction);
		assertFalse(ps instanceof Expireable);
		ps = ps.expire(()->1000).testingState((s,b)->true).timeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof TestingState);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((TestingState)ps).test(null));
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		assertEquals("TEST ESO",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	
}
