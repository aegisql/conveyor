package com.aegisql.conveyor.user;

import com.aegisql.conveyor.utils.schedule.SchedulableClosure;
import com.aegisql.conveyor.utils.schedule.Schedule;
import com.aegisql.conveyor.utils.schedule.SimpleScheduler;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The Class SchedulerTest.
 */
public class SchedulerTest {

	/**
	 * The s.
	 */
	SimpleScheduler<String> s = new SimpleScheduler<>();

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
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testExecuteOnceError() throws InterruptedException, ExecutionException {
		assertThrows(ExecutionException.class,
				()->s.part().id("test1").label(Schedule.EXECUTE_ONCE).ttl(1, TimeUnit.SECONDS).place().get()
		);
	}

	/**
	 * Test execute once error2.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testExecuteOnceError2() throws InterruptedException, ExecutionException {
		assertThrows(ExecutionException.class,
				()->s.part().id("test1").value("value").label(Schedule.EXECUTE_ONCE).ttl(1, TimeUnit.SECONDS).place().get()
		);
	}

	/**
	 * Closure test.
	 */
	@Test
	public void closureTest() {

		StringBuilder sb = new StringBuilder();

		SchedulableClosure sc1 = ()->sb.append("A");
		SchedulableClosure sc2 = sc1.andThen(()->sb.append("B"));
		SchedulableClosure sc3 = sc2.compose(()->sb.append("C"));
		sc3.apply();
		System.out.println(sb);
		assertEquals("CAB",sb.toString());
	}
}
