package com.aegisql.conveyor;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderSmart;

public class SmartLabelFunctionalTest {

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
	public void testRunnable() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(()->{
			System.out.println("SL RUNNABLE");
		});
		assertNotNull(l1);
		l1.get().accept(b, "TEST");		
	}

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
	public void testBiConsumer() {
		UserBuilderSmart b = new UserBuilderSmart();
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		assertNotNull(l1);
		l1.get().accept(b, "TEST");	
		User u = b.get();
		assertEquals("TEST",u.getFirst());
	}

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

}
