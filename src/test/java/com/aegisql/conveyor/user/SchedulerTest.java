package com.aegisql.conveyor.user;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.command.CancelCommand;
import com.aegisql.conveyor.utils.schedule.SchedulableClosure;
import com.aegisql.conveyor.utils.schedule.Schedule;
import com.aegisql.conveyor.utils.schedule.SimpleScheduler;

// TODO: Auto-generated Javadoc
/**
 * The Class SchedulerTest.
 */
public class SchedulerTest {

	/** The s. */
	SimpleScheduler<String> s = new SimpleScheduler<>();
	
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
	 * Test execute once.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testExecuteOnce() throws InterruptedException {
		SchedulableClosure c = ()->{
			System.out.println("EXECUTED ONCE");
		};
		s.add("test1", c , Schedule.EXECUTE_ONCE, 1, TimeUnit.SECONDS);
		Thread.sleep(1300);
		assertEquals(0,s.getCollectorSize());
	}

	/**
	 * Test execute once2.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testExecuteOnce2() throws InterruptedException {
		SchedulableClosure c = ()->{
			System.out.println("EXECUTED ONCE 2");
		};
		s.add("test1", c , Schedule.EXECUTE_ONCE, System.currentTimeMillis()+1000);
		Thread.sleep(1300);
		assertEquals(0,s.getCollectorSize());
	}

	
	/**
	 * Test execute with delay.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testExecuteWithDelay() throws InterruptedException {
		SchedulableClosure c = ()->{
			System.out.println("EXECUTED WITH DELAY");
		};
		s.add("test2", c , Schedule.SCHEDULE_WITH_DELAY, 1, TimeUnit.SECONDS);
		Thread.sleep(2100);
		assertEquals(1,s.getCollectorSize());
		s.addCommand(new CancelCommand<String>("test2"));
		Thread.sleep(100);
		assertEquals(0,s.getCollectorSize());
	}

	/**
	 * Test execute now and with delay.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testExecuteNowAndWithDelay() throws InterruptedException {
		SchedulableClosure c = ()->{
			System.out.println("EXECUTED NOW AND WITH DELAY");
		};
		s.add("test3", c , Schedule.SCHEDULE_AND_EXECUTE_NOW, 1, TimeUnit.SECONDS);
		Thread.sleep(3500);
		assertEquals(1,s.getCollectorSize());
		s.addCommand(new CancelCommand<String>("test3"));
		Thread.sleep(100);
		assertEquals(0,s.getCollectorSize());
	}


	/**
	 * Test execute now and with delay duration.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testExecuteNowAndWithDelayDuration() throws InterruptedException {
		SchedulableClosure c = ()->{
			System.out.println("EXECUTED NOW AND WITH DELAY DURATION");
		};
		s.add("test4", c , Schedule.SCHEDULE_AND_EXECUTE_NOW, Duration.ofSeconds(1));
		Thread.sleep(3500);
		assertEquals(1,s.getCollectorSize());
		s.addCommand(new CancelCommand<String>("test4"));
		Thread.sleep(100);
		assertEquals(0,s.getCollectorSize());
	}

	/**
	 * Test execute once error.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test(expected=NullPointerException.class)
	public void testExecuteOnceError() throws InterruptedException {
		s.add("test1", null , Schedule.EXECUTE_ONCE, 1, TimeUnit.SECONDS);
		Thread.sleep(1300);
		assertEquals(0,s.getCollectorSize());
	}

	/**
	 * Test execute once error2.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test(expected=ClassCastException.class)
	public void testExecuteOnceError2() throws InterruptedException {
		s.add("test1", "value" , Schedule.EXECUTE_ONCE, 1, TimeUnit.SECONDS);
		Thread.sleep(1300);
		assertEquals(0,s.getCollectorSize());
	}

	
}
