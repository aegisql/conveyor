package com.aegisql.conveyor;

import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

// TODO: Auto-generated Javadoc
/**
 * The Class ProductSupplierTest.
 */
public class ProductSupplierTest {

	private static final class TestingTimeoutSupplier
			implements ProductSupplier<String>, Testing, TimeoutAction {
		private final AtomicInteger timeoutCount = new AtomicInteger();

		@Override
		public String get() {
			return "TT";
		}

		@Override
		public Supplier<String> getSupplier() {
			return this::get;
		}

		@Override
		public boolean test() {
			return true;
		}

		@Override
		public void onTimeout() {
			timeoutCount.incrementAndGet();
		}

		int getTimeoutCount() {
			return timeoutCount.get();
		}
	}

	private static final class StateTimeoutSupplier
			implements ProductSupplier<String>, TestingState<Integer, String>, TimeoutAction {
		private final AtomicInteger timeoutCount = new AtomicInteger();

		@Override
		public String get() {
			return "ST";
		}

		@Override
		public Supplier<String> getSupplier() {
			return this::get;
		}

		@Override
		public boolean test(State<Integer, String> state) {
			return state != null && state.previouslyAccepted >= 0;
		}

		@Override
		public void onTimeout() {
			timeoutCount.incrementAndGet();
		}

		int getTimeoutCount() {
			return timeoutCount.get();
		}
	}

	private static final class ExpiringTimeoutSupplier
			implements ProductSupplier<String>, Expireable, TimeoutAction {
		private final long expiration;
		private final AtomicInteger timeoutCount = new AtomicInteger();

		private ExpiringTimeoutSupplier(long expiration) {
			this.expiration = expiration;
		}

		@Override
		public String get() {
			return "E";
		}

		@Override
		public Supplier<String> getSupplier() {
			return this::get;
		}

		@Override
		public long getExpirationTime() {
			return expiration;
		}

		@Override
		public void onTimeout() {
			timeoutCount.incrementAndGet();
		}

		int getTimeoutCount() {
			return timeoutCount.get();
		}
	}

	private static final class ExpiringTestingSupplier
			implements ProductSupplier<String>, Expireable, Testing {
		private final long expiration;

		private ExpiringTestingSupplier(long expiration) {
			this.expiration = expiration;
		}

		@Override
		public String get() {
			return "ET";
		}

		@Override
		public Supplier<String> getSupplier() {
			return this::get;
		}

		@Override
		public long getExpirationTime() {
			return expiration;
		}

		@Override
		public boolean test() {
			return true;
		}
	}

	private static final class ExpiringStateSupplier
			implements ProductSupplier<String>, Expireable, TestingState<Integer, String> {
		private final long expiration;

		private ExpiringStateSupplier(long expiration) {
			this.expiration = expiration;
		}

		@Override
		public String get() {
			return "ES";
		}

		@Override
		public Supplier<String> getSupplier() {
			return this::get;
		}

		@Override
		public long getExpirationTime() {
			return expiration;
		}

		@Override
		public boolean test(State<Integer, String> state) {
			return state != null && state.previouslyAccepted >= 0;
		}
	}

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

	@Test
	public void testOfWithExistingProductSupplierAndDefaultUnwrap() {
		ProductSupplier<String> existing = ProductSupplier.of(() -> new StringBuilder("KEEP"), StringBuilder::toString);
		ProductSupplier<String> wrappedExisting = ProductSupplier.of(existing);
		assertSame(existing, wrappedExisting);

		ProductSupplier<String> plain = ProductSupplier.of(() -> "PLAIN");
		assertEquals("PLAIN", plain.getSupplier().get());
		assertSame(plain, plain.unwrap(ProductSupplier.class));
	}

	@Test
	public void testExpiresCoversTestingTimeoutAndStateTimeoutBranches() {
		TestingTimeoutSupplier tt = new TestingTimeoutSupplier();
		ProductSupplier<String> ttExpired = tt.expires(() -> 777L);
		assertTrue(ttExpired instanceof Expireable);
		assertTrue(ttExpired instanceof Testing);
		assertTrue(ttExpired instanceof TimeoutAction);
		assertEquals("TT", ttExpired.getSupplier().get());
		assertTrue(((Testing) ttExpired).test());
		((TimeoutAction) ttExpired).onTimeout();
		assertEquals(1, tt.getTimeoutCount());
		assertEquals(777L, ((Expireable) ttExpired).getExpirationTime());

		StateTimeoutSupplier st = new StateTimeoutSupplier();
		ProductSupplier<String> stExpired = st.expires(() -> 888L);
		assertTrue(stExpired instanceof Expireable);
		assertTrue(stExpired instanceof TestingState);
		assertTrue(stExpired instanceof TimeoutAction);
		State<Integer, String> state = new State<>(1, 1L, 2L, 3L, 4L, 0, Map.of(), Collections.emptyList());
		assertTrue(((TestingState<Integer, String>) stExpired).test(state));
		((TimeoutAction) stExpired).onTimeout();
		assertEquals(1, st.getTimeoutCount());
		assertEquals(888L, ((Expireable) stExpired).getExpirationTime());
	}

	@Test
	public void testOnTimeoutReadyAlgorithmAndStateReadyAlgorithmForExpireableTimeoutSupplier() {
		ExpiringTimeoutSupplier supplier = new ExpiringTimeoutSupplier(1_000L);
		AtomicInteger timeoutCalls = new AtomicInteger();

		ProductSupplier<String> withTimeoutAction = supplier.onTimeout(s -> timeoutCalls.incrementAndGet());
		assertTrue(withTimeoutAction instanceof Expireable);
		assertTrue(withTimeoutAction instanceof TimeoutAction);
		assertEquals("E", withTimeoutAction.getSupplier().get());
		assertEquals(1_000L, ((Expireable) withTimeoutAction).getExpirationTime());
		((TimeoutAction) withTimeoutAction).onTimeout();
		assertEquals(1, timeoutCalls.get());

		ExpiringTestingSupplier testingSupplier = new ExpiringTestingSupplier(2_000L);
		ProductSupplier<String> testingTimeout = testingSupplier.onTimeout(s -> timeoutCalls.incrementAndGet());
		assertTrue(testingTimeout instanceof Expireable);
		assertTrue(testingTimeout instanceof Testing);
		assertTrue(testingTimeout instanceof TimeoutAction);
		assertTrue(((Testing) testingTimeout).test());
		((TimeoutAction) testingTimeout).onTimeout();
		assertEquals(2_000L, ((Expireable) testingTimeout).getExpirationTime());

		ExpiringStateSupplier stateSupplier = new ExpiringStateSupplier(3_000L);
		ProductSupplier<String> stateTimeout = stateSupplier.onTimeout(s -> timeoutCalls.incrementAndGet());
		assertTrue(stateTimeout instanceof Expireable);
		assertTrue(stateTimeout instanceof TestingState);
		assertTrue(stateTimeout instanceof TimeoutAction);
		State<Integer, String> timeoutState = new State<>(7, 1L, 2L, 3L, 4L, 0, Map.of(), Collections.emptyList());
		assertTrue(((TestingState<Integer, String>) stateTimeout).test(timeoutState));
		assertEquals(3_000L, ((Expireable) stateTimeout).getExpirationTime());
		((TimeoutAction) stateTimeout).onTimeout();

		ProductSupplier<String> ready = supplier.readyAlgorithm(s -> s.get().equals("E"));
		assertTrue(ready instanceof Expireable);
		assertTrue(ready instanceof Testing);
		assertTrue(ready instanceof TimeoutAction);
		assertTrue(((Testing) ready).test());
		((TimeoutAction) ready).onTimeout();
		assertEquals(1, supplier.getTimeoutCount());

		ProductSupplier<String> readyState = supplier.readyAlgorithm(
				(state, ps) -> state != null && Integer.valueOf(11).equals(state.key) && ps.get().equals("E")
		);
		assertTrue(readyState instanceof Expireable);
		assertTrue(readyState instanceof TestingState);
		assertTrue(readyState instanceof TimeoutAction);
		State<Integer, String> state = new State<>(11, 1L, 2L, 3L, 4L, 0, Map.of(), Collections.emptyList());
		assertTrue(((TestingState<Integer, String>) readyState).test(state));
		((TimeoutAction) readyState).onTimeout();
		assertEquals(2, supplier.getTimeoutCount());
	}

}
