package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderSmart;

// TODO: Auto-generated Javadoc
/**
 * The Class SmartLabelFunctionalTest.
 */
public class SmartLabelFunctionalTest {

	/**
	 * Sets the up before class.
	 *
	 * @throws Exception the exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * Tear down after class.
	 *
	 * @throws Exception the exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Sets the up.
	 *
	 * @throws Exception the exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Tear down.
	 *
	 * @throws Exception the exception
	 */
	@After
	public void tearDown() throws Exception {
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
		l1 = l1.intercept(Exception.class, (error)->{
			System.out.println("Intercepted Error "+error.getMessage());			
		});
		assertNotNull(l1);
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
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		assertNotNull(l1);
		l1 = l1.before((builder,value)->{
			System.out.println("Before "+value);
		});
		assertNotNull(l1);
		l1 = l1.andThen((builder,value)->{
			System.out.println("After "+value);			
		});
		assertNotNull(l1);
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
		
		CompletableFuture<User> f = c.createBuildFuture(1,BuilderSupplier.of(UserBuilderSmart::new).readyAlgorithm(new ReadinessTester().accepted(3)));
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		SmartLabel<UserBuilderSmart> l2 = SmartLabel.of(UserBuilderSmart::setLast);
		SmartLabel<UserBuilderSmart> l3 = SmartLabel.of(UserBuilderSmart::setYearOfBirth);
		c.add(1,"FIRST",l1);
		c.add(1,"LAST",l2);
		c.add(1,1999,l3);
		assertNotNull(l1);
		assertNotNull(l2);
		assertNotNull(l3);
		User u = f.get();
		assertEquals("FIRST",u.getFirst());
		assertEquals("LAST",u.getLast());
		assertEquals(1999,u.getYearOfBirth());
	}


	/**
	 * Test before after bi consumer wrapped.
	 */
	@Test
	public void testBeforeAfterBiConsumerWrapped() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		assertNotNull(l1);
		l1 = l1.before((builder,value)->{
			System.out.println("Before "+value);
		});
		assertNotNull(l1);
		l1 = l1.andThen((builder,value)->{
			System.out.println("After "+value);			
		});
		assertNotNull(l1);
		SmartWrapper<String, UserBuilderSmart, ?> w1 = new SmartWrapper<>("LABEL", l1.get());
		w1.get().accept(b, "TEST");	
		User u = b.get();
		assertEquals("TEST",u.getFirst());
		System.out.println(w1);
	}

}
