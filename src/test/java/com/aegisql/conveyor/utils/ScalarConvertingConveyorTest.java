package com.aegisql.conveyor.utils;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.utils.scalar.ScalarCart;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingBuilder;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class ScalarConvertingConveyorTest.
 */
public class ScalarConvertingConveyorTest {

	
	/**
	 * The Class StringToUserBuulder.
	 */
	static class StringToUserBuulder extends ScalarConvertingBuilder<String,User> {
		
		/* (non-Javadoc)
		 * @see java.util.function.Supplier#get()
		 */
		@Override
		public User get() {
			String[] fields = scalar.split(",");
			return new User(fields[0], fields[1], Integer.parseInt(fields[2]));
		}
		
	}
	
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
	 * Test scalar converting conveyor.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testScalarConvertingConveyor() throws InterruptedException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		sc.setResultConsumer(u->{
			System.out.println("RESULT: "+u);
			usr.set(u.product);
		});
		String csv = "John,Dow,1990";
		
		ScalarCart<String,String> c = new ScalarCart<String, String>("test", csv);
		
		sc.add(c);
		Thread.sleep(20);
		assertNotNull(usr.get());
	}
	

}
