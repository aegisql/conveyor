package com.aegisql.conveyor.user;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
		s.part().id("test1").value(c).label(Schedule.EXECUTE_ONCE).ttl(1, TimeUnit.SECONDS).place();
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
		s.part().id("test1").value(c).label(Schedule.EXECUTE_ONCE).expirationTime(System.currentTimeMillis()+1000).place();
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
		s.part().id("test2").value(c).label(Schedule.SCHEDULE_WITH_DELAY).ttl(1, TimeUnit.SECONDS).place();
		Thread.sleep(2100);
		assertEquals(1,s.getCollectorSize());
		s.command().id("test2").cancel();
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
		s.part().id("test3").value(c).label(Schedule.SCHEDULE_AND_EXECUTE_NOW).ttl(1, TimeUnit.SECONDS).place();
		Thread.sleep(3500);
		assertEquals(1,s.getCollectorSize());
		s.command().id("test3").cancel();
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
		s.part().id("test4").value(c).label(Schedule.SCHEDULE_AND_EXECUTE_NOW).ttl(Duration.ofSeconds(1)).place();
		Thread.sleep(3500);
		assertEquals(1,s.getCollectorSize());
		s.command().id("test4").cancel();
		Thread.sleep(100);
		assertEquals(0,s.getCollectorSize());
	}

	/**
	 * Test execute once error.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException 
	 */
	@Test(expected=ExecutionException.class)
	public void testExecuteOnceError() throws InterruptedException, ExecutionException {
		s.part().id("test1").label(Schedule.EXECUTE_ONCE).ttl(1, TimeUnit.SECONDS).place().get();
	}

	/**
	 * Test execute once error2.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException 
	 */
	@Test(expected=ExecutionException.class)
	public void testExecuteOnceError2() throws InterruptedException, ExecutionException {
		s.part().id("test1").value("value").label(Schedule.EXECUTE_ONCE).ttl(1, TimeUnit.SECONDS).place().get();
	}

	
}
