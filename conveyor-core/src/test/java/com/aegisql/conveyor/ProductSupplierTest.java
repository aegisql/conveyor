package com.aegisql.conveyor;

import org.junit.jupiter.api.*;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

// TODO: Auto-generated Javadoc
/**
 * The Class ProductSupplierTest.
 */
public class ProductSupplierTest {

	/**
	 * Sets the up before class.
	 *
	 */
	@BeforeAll
	public static void setUpBeforeClass() {
	}

	/**
	 * Tear down after class.
	 *
	 */
	@AfterAll
	public static void tearDownAfterClass() {
	}

	/**
	 * Sets the up.
	 *
	 */
	@BeforeEach
	public void setUp() {
	}

	/**
	 * Tear down.
	 *
	 */
	@AfterEach
	public void tearDown() {
	}

	/**
	 * Test E.
	 */
	@Test
	public void testE() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST E"),StringBuilder::toString);
		assertFalse(ps instanceof Expireable);
		ps = ps.expires(()->1000);
		assertTrue(ps instanceof Expireable);
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		ps = ps.expires(()->2000);
		assertTrue(ps instanceof Expireable);
		assertEquals(2000,((Expireable)ps).getExpirationTime());
		assertTrue(ps==ps.identity());
	}

	/**
	 * Test O.
	 */
	@Test
	public void testO() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST O"),StringBuilder::toString);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.onTimeout(b->{System.out.println(b.get());});
		assertTrue(ps instanceof TimeoutAction);
		((TimeoutAction)ps).onTimeout();
		ps = ps.onTimeout(b->{System.out.println("TEST OTHER");});
		assertTrue(ps instanceof TimeoutAction);
		((TimeoutAction)ps).onTimeout();
	}

	/**
	 * Test T.
	 */
	@Test
	public void testT() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST T"),StringBuilder::toString);
		assertFalse(ps instanceof Testing);
		ps = ps.readyAlgorithm(b->true);
		assertTrue(ps instanceof Testing);
		assertTrue(((Testing)ps).test());
		ps = ps.readyAlgorithm(b->false);
		assertTrue(ps instanceof Testing);
		assertFalse(((Testing)ps).test());
		assertNotNull(ps.getSupplier());
		ps=ps.expires(new Expireable() {
			@Override
			public long getExpirationTime() {
				return 10000;
			}
		});
		long expirationTime = ((ProductSupplier.PET) ps).getExpirationTime();
		assertEquals(10000,expirationTime);
	}

	/**
	 * Test S.
	 */
	@Test
	public void testS() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST S"),StringBuilder::toString);
		assertFalse(ps instanceof TestingState);
		ps = ps.readyAlgorithm((s,b)->true);
		assertTrue(ps instanceof TestingState);
		assertTrue(((TestingState)ps).test(null));
		ps = ps.readyAlgorithm((s,b)->false);
		assertTrue(ps instanceof TestingState);
		assertFalse(((TestingState)ps).test(null));
	}

	/**
	 * Test TS.
	 */
	@Test
	public void testTS() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST TS"),StringBuilder::toString);
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

	
	/**
	 * Test of.
	 */
	@Test
	public void testOf() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.onTimeout((b)->{System.out.println(b.get());}).expires(()->1000);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		ps.unwrap(StringBuilder.class).append(" OE");
		assertEquals("TEST OE",ps.get());
		((TimeoutAction)ps).onTimeout();
		//ps = ProductSupplier.of(ps);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
	}

	
	/**
	 * Test OE.
	 */
	@Test
	public void testOE() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.onTimeout((b)->{System.out.println(b.get());}).expires(()->1000);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		ps.unwrap(StringBuilder.class).append(" OE");

		assertEquals("TEST OE",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	/**
	 * Test EO.
	 */
	@Test
	public void testEO() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.expires(()->1000).onTimeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		ps.unwrap(StringBuilder.class).append(" EO");

		assertEquals("TEST EO",ps.get());
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		((TimeoutAction)ps).onTimeout();
	}

	/**
	 * Test ET.
	 */
	@Test
	public void testET() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof Testing);
		ps = ps.expires(()->1000).readyAlgorithm(b->true);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof Testing);
		ps.unwrap(StringBuilder.class).append(" ET");

		assertEquals("TEST ET",ps.get());
		assertEquals(true,((Testing)ps).test());
	}

	/**
	 * Test TE.
	 */
	@Test
	public void testTE() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof Testing);
		ps = ps.readyAlgorithm(b->true).expires(()->1000);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof Testing);
		ps.unwrap(StringBuilder.class).append(" TE");

		assertEquals("TEST TE",ps.get());
		assertEquals(true,((Testing)ps).test());
	}

	/**
	 * Test OT.
	 */
	@Test
	public void testOT() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof Testing);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.onTimeout((b)->{System.out.println(b.get());}).readyAlgorithm((b)->true);
		assertTrue(ps instanceof Testing);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((Testing)ps).test());
		ps.unwrap(StringBuilder.class).append(" OT");

		assertEquals("TEST OT",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	/**
	 * Test TO.
	 */
	@Test
	public void testTO() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof Testing);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.readyAlgorithm((b)->true).onTimeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof Testing);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((Testing)ps).test());
		ps.unwrap(StringBuilder.class).append(" TO");

		assertEquals("TEST TO",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	/**
	 * Test ES.
	 */
	@Test
	public void testES() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TestingState);
		ps = ps.expires(()->1000).readyAlgorithm((s,b)->true);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TestingState);
		ps.unwrap(StringBuilder.class).append(" ES");

		assertEquals("TEST ES",ps.get());
		assertEquals(true,((TestingState)ps).test(null));
	}

	/**
	 * Test SE.
	 */
	@Test
	public void testSE() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof Expireable);
		assertFalse(ps instanceof TestingState);
		ps = ps.readyAlgorithm((s,b)->true).expires(()->1000);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TestingState);
		ps.unwrap(StringBuilder.class).append(" SE");

		assertEquals("TEST SE",ps.get());
		assertEquals(true,((TestingState)ps).test(null));
	}

	/**
	 * Test OS.
	 */
	@Test
	public void testOS() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof TestingState);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.onTimeout((b)->{System.out.println(b.get());}).readyAlgorithm((s,b)->true);
		assertTrue(ps instanceof TestingState);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((TestingState)ps).test(null));
		ps.unwrap(StringBuilder.class).append(" OS");

		assertEquals("TEST OS",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	/**
	 * Test SO.
	 */
	@Test
	public void testSO() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof TestingState);
		assertFalse(ps instanceof TimeoutAction);
		ps = ps.readyAlgorithm((s,b)->true).onTimeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof TestingState);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((TestingState)ps).test(null));
		ps.unwrap(StringBuilder.class).append(" SO");

		assertEquals("TEST SO",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	
	/**
	 * Test ETO.
	 */
	@Test
	public void testETO() {
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof Testing);
		assertFalse(ps instanceof TimeoutAction);
		assertFalse(ps instanceof Expireable);
		ps = ps.expires(()->1000).readyAlgorithm((b)->true).onTimeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof Testing);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((Testing)ps).test());
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		ps.unwrap(StringBuilder.class).append(" ETO");

		assertEquals("TEST ETO",ps.get());
		((TimeoutAction)ps).onTimeout();
	}

	/**
	 * Test ESO.
	 */
	@Test
	public void testESO() {
		Supplier<String> ss = ()->"TEST ESO";
		ProductSupplier<String> ps = ProductSupplier.of(()->new StringBuilder("TEST"),StringBuilder::toString);
		assertFalse(ps instanceof TestingState);
		assertFalse(ps instanceof TimeoutAction);
		assertFalse(ps instanceof Expireable);
		ps = ps.expires(()->1000).readyAlgorithm((s,b)->true).onTimeout((b)->{System.out.println(b.get());});
		assertTrue(ps instanceof TestingState);
		assertTrue(ps instanceof Expireable);
		assertTrue(ps instanceof TimeoutAction);
		assertEquals(true,((TestingState)ps).test(null));
		assertEquals(1000,((Expireable)ps).getExpirationTime());
		ps.unwrap(StringBuilder.class).append(" ESO");
		assertEquals("TEST ESO",ps.get());
		((TimeoutAction)ps).onTimeout();
		//assertTrue(ss == ps.getProductSupplier());
	}

}
