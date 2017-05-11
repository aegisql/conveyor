package com.aegisql.conveyor.utils;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.utils.delay_line.DelayLineConveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class DelayLineConveyorTest.
 */
public class DelayLineConveyorTest {

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
	 * Test.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException 
	 */
	@Test
	public void test() throws InterruptedException, ExecutionException {
		
		List<Integer> res = new ArrayList<>();
		
		DelayLineConveyor<Integer, Integer> c = new DelayLineConveyor<>();
		c.resultConsumer(bin->{
			System.out.println("++ "+bin);
			res.add(bin.product);
		}).set();
		c.setScrapConsumer(bin->{
			System.out.println("-- "+bin);
		});
		c.setIdleHeartBeat(50, TimeUnit.MILLISECONDS);
				
		c.part().id(1).value(1).ttl(Duration.ofMillis(11)).place();
		c.part().id(4).value(4).ttl(Duration.ofMillis(14)).place();
		c.part().id(2).value(2).ttl(Duration.ofMillis(12)).place();
		c.part().id(3).value(3).ttl(Duration.ofMillis(13)).place();
		CompletableFuture<Integer> last = c.future().ttl(Duration.ofMillis(15)).id(5).get();
		c.part().id(5).value(5).place();
		
		assertEquals(new Integer(5), last.get());
		
		assertEquals(1, res.get(0).intValue());
		assertEquals(2, res.get(1).intValue());
		assertEquals(3, res.get(2).intValue());
		assertEquals(4, res.get(3).intValue());
		assertEquals(5, res.get(4).intValue());
		
	}
	

}
