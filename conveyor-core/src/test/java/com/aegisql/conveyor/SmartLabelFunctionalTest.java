package com.aegisql.conveyor;

import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderSmart;
import org.junit.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.junit.Assert.*;

// TODO: Auto-generated Javadoc
/**
 * The Class SmartLabelFunctionalTest.
 */
public class SmartLabelFunctionalTest {

	/**
	 * Sets the up before class.
	 *
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
	}

	/**
	 * Tear down after class.
	 *
	 */
	@AfterClass
	public static void tearDownAfterClass() {
	}

	/**
	 * Sets the up.
	 *
	 */
	@Before
	public void setUp() {
	}

	/**
	 * Tear down.
	 *
	 */
	@After
	public void tearDown() {
	}

	/**
	 * Test runnable.
	 */
	@Test
	public void testRunnable() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(()->{
			System.out.println("SL RUNNABLE");
		});
		assertNotNull(l1);
		l1.get().accept(b, "TEST");		
	}

	@Test
	public void testRunnableWithName() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of("TEST_LABEL",()->{
			System.out.println("SL RUNNABLE");
		});
		assertNotNull(l1);
		l1.get().accept(b, "TEST");		
		assertEquals("TEST_LABEL",l1.toString());
	}

	/**
	 * Test consumer.
	 */
	@Test
	public void testConsumer() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of((builder)->{
			System.out.println("SL CONSUMER");
			UserBuilderSmart.setFirst(builder, "FIRST");
		});
		assertNotNull(l1);
		l1.get().accept(b, "TEST");	
		User u = b.get();
		assertEquals("FIRST",u.getFirst());
	}

	@Test
	public void testConsumerWithName() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of("TEST_LABEL",(builder)->{
			System.out.println("SL CONSUMER");
			UserBuilderSmart.setFirst(builder, "FIRST");
		});
		assertNotNull(l1);
		l1.get().accept(b, "TEST");	
		User u = b.get();
		assertEquals("FIRST",u.getFirst());
		assertEquals("TEST_LABEL",l1.toString());
	}

	/**
	 * Test bi consumer.
	 */
	@Test
	public void testBiConsumer() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		assertNotNull(l1);
		l1.get().accept(b, "TEST");	
		User u = b.get();
		assertEquals("TEST",u.getFirst());
	}

	@Test
	public void testBiConsumerWithName() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of("TEST_LABEL",UserBuilderSmart::setFirst);
		assertNotNull(l1);
		l1.get().accept(b, "TEST");	
		User u = b.get();
		assertEquals("TEST",u.getFirst());
		assertEquals("TEST_LABEL",l1.toString());
		System.out.println(l1);
	}

	/**
	 * Test intercept runnable.
	 */
	@Test
	public void testInterceptRunnable() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		assertNotNull(l1);
		l1 = l1.intercept(Integer.class, ()->{
			System.out.println("Intercepted Integer");			
		});
		assertNotNull(l1);
		l1 = l1.intercept(Exception.class, ()->{
			System.out.println("Intercepted Error");			
		});
		assertNotNull(l1);

		l1.get().accept(b, null);
		l1.get().accept(b, "TEST");
		l1.get().accept(b, 1);
		l1.get().accept(b, new Exception("error"));
		User u = b.get();
		assertEquals("TEST",u.getFirst());
	}

	/**
	 * Test intercept consumer.
	 */
	@Test
	public void testInterceptConsumer() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		assertNotNull(l1);
		l1 = l1.intercept(Integer.class, (value)->{
			System.out.println("Intercepted Integer "+value);
			
		});
		assertNotNull(l1);
		assertNotNull(l1.identity());
		l1 = l1.intercept(Exception.class, (error)->{
			System.out.println("Intercepted Error "+error.getMessage());			
		});
		assertNotNull(l1);
		l1.get().accept(b, null);
		l1.get().accept(b, "TEST");
		l1.get().accept(b, 1);
		l1.get().accept(b, new Exception("error"));
		User u = b.get();
		assertEquals("TEST",u.getFirst());
	}

	/**
	 * Test intercept bi consumer.
	 */
	@Test
	public void testInterceptBiConsumer() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		assertNotNull(l1);
		l1 = l1.intercept(Integer.class, (builder,value)->{
			System.out.println("Intercepted Integer "+value);
			UserBuilderSmart.setYearOfBirth(builder, value);
		});
		assertNotNull(l1);
		l1 = l1.intercept(Exception.class, (builder,value)->{
			System.out.println("Intercepted Error "+value.getMessage());			
		});
		assertNotNull(l1);
		l1.get().accept(b, null);
		l1.get().accept(b, "TEST");
		l1.get().accept(b, 1);
		l1.get().accept(b, new Exception("error"));
		User u = b.get();
		assertEquals("TEST",u.getFirst());
		assertEquals(1, u.getYearOfBirth());
	}

	/**
	 * Test before after bi consumer.
	 */
	@Test
	public void testBeforeAfterBiConsumer() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of("L1",UserBuilderSmart::setFirst);
		assertNotNull(l1);
		System.out.println(l1);
		l1 = l1.before((builder,value)->{
			System.out.println("Before "+value);
		});
		assertNotNull(l1);
		System.out.println(l1);
		l1 = l1.andThen((builder,value)->{
			System.out.println("After "+value);			
		});
		assertNotNull(l1);
		System.out.println(l1);
		l1.get().accept(b, "TEST");	
		User u = b.get();
		assertEquals("TEST",u.getFirst());
	}

	/**
	 * Test before after consumer.
	 */
	@Test
	public void testBeforeAfterConsumer() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		assertNotNull(l1);
		l1 = l1.before((value)->{
			System.out.println("Before "+value);
		});
		assertNotNull(l1);
		l1 = l1.andThen((value)->{
			System.out.println("After "+value);			
		});
		assertNotNull(l1);
		l1.get().accept(b, "TEST");	
		User u = b.get();
		assertEquals("TEST",u.getFirst());
	}

	/**
	 * Test before after runnable.
	 */
	@Test
	public void testBeforeAfterRunnable() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		assertNotNull(l1);
		l1 = l1.before(()->{
			System.out.println("Before");
		});
		assertNotNull(l1);
		l1 = l1.andThen(()->{
			System.out.println("After");			
		});
		assertNotNull(l1);
		l1.get().accept(b, "TEST");	
		User u = b.get();
		assertEquals("TEST",u.getFirst());
	}

	
	/**
	 * Test bi consumer conv.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void testBiConsumerConv() throws InterruptedException, ExecutionException {
		
		AssemblingConveyor<Integer, SmartLabel<UserBuilderSmart>, User> c = new AssemblingConveyor<>();
		
		CompletableFuture<User> f = c.build().id(1).supplier(BuilderSupplier.of(UserBuilderSmart::new).readyAlgorithm(new ReadinessTester().accepted(3))).createFuture();
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		SmartLabel<UserBuilderSmart> l2 = SmartLabel.of(UserBuilderSmart::setLast);
		SmartLabel<UserBuilderSmart> l3 = SmartLabel.of(UserBuilderSmart::setYearOfBirth);
		c.part().value("FIRST").id(1).label(l1).place();
		c.part().value("LAST").id(1).label(l2).place();
		c.part().value(1999).id(1).label(l3).place();
		assertNotNull(l1);
		assertNotNull(l2);
		assertNotNull(l3);
		User u = f.get();
		assertEquals("FIRST",u.getFirst());
		assertEquals("LAST",u.getLast());
		assertEquals(1999,u.getYearOfBirth());
	}

	@Test
	public void testLabelName() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		assertNotNull(l1);
		l1 = l1.labelName("L");
		assertNotNull(l1);
		System.out.println(l1);
		assertNotNull(l1.get());
		l1.get().accept(b, "A");
		assertNotNull(b.get());
		assertEquals(b.get().getFirst(), "A");
		
	}

	
	@Test
	public void testBareName() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.bare("L");
		assertNotNull(l1);
		System.out.println(l1);
		assertNotNull(l1.get());
		l1.get().accept(b, "A");
		assertNotNull(b.get());
		assertNull(b.get().getFirst());
		
	}

	@Test
	public void testBareNoName() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.bare();
		assertNotNull(l1);
		l1 = l1.labelName("L");
		assertNotNull(l1);
		System.out.println(l1);
		assertNotNull(l1.get());
		l1.get().accept(b, "A");
		assertNotNull(b.get());
		assertNull(b.get().getFirst());
		
	}
	
	@Test
	public void peekTest() throws InterruptedException, ExecutionException {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.peek();
		assertNotNull(l1);
		l1 = l1.labelName("P");
		assertNotNull(l1);
		System.out.println(l1);
		assertNotNull(l1.get());
		CompletableFuture<User> f = new CompletableFuture<>();
		l1.get().accept(b, f);
		assertNotNull(b.get());
		assertNull(b.get().getFirst());
		assertTrue(f.isDone());
		System.out.println(f.get());
	}

	@Test
	public void peekNamedTest() throws InterruptedException, ExecutionException {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.peek("name");
		assertNotNull(l1);
		l1 = l1.labelName("P");
		assertNotNull(l1);
		System.out.println(l1);
		assertNotNull(l1.get());
		CompletableFuture<User> f = new CompletableFuture<>();
		l1.get().accept(b, f);
		assertNotNull(b.get());
		assertNull(b.get().getFirst());
		assertTrue(f.isDone());
		System.out.println(f.get());
	}

	static class Failing implements Supplier<String> {

		@Override
		public String get() {
			throw new RuntimeException("fail!");
		}
	}

	@Test
	public void peekFailTest() {
		Failing b = new Failing();
		SmartLabel<Failing> l1 = SmartLabel.peek();
		assertNotNull(l1);
		l1 = l1.labelName("P");
		assertNotNull(l1);
		System.out.println(l1);
		assertNotNull(l1.get());
		CompletableFuture<User> f = new CompletableFuture<>();
		l1.get().accept(b, f);
		try {
			assertNotNull(b.get());
			fail("not expected");
		} catch (Exception e) {

		}
		assertTrue(f.isDone());
		assertTrue(f.isCompletedExceptionally());
	}

	@Test
	public void peekNamedFailTest() {
		Failing b = new Failing();
		SmartLabel<Failing> l1 = SmartLabel.peek("peek");
		assertNotNull(l1);
		l1 = l1.labelName("P");
		assertNotNull(l1);
		System.out.println(l1);
		assertNotNull(l1.get());
		CompletableFuture<User> f = new CompletableFuture<>();
		l1.get().accept(b, f);
		try {
			assertNotNull(b.get());
			fail("not expected");
		} catch (Exception e) {

		}
		assertTrue(f.isDone());
		assertTrue(f.isCompletedExceptionally());
	}


}
