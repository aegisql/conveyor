package com.aegisql.conveyor.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.utils.delay_line.DelayLineCart;
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
	 */
	@Test
	public void test() throws InterruptedException {
		
		List<Integer> res = new ArrayList<>();
		
		DelayLineConveyor<Integer, Integer> c = new DelayLineConveyor<>();
		c.setResultConsumer(bin->{
			System.out.println("++ "+bin);
			res.add(bin.product);
		});
		c.setScrapConsumer(bin->{
			System.out.println("-- "+bin);
		});
		c.setIdleHeartBeat(50, TimeUnit.MILLISECONDS);
		
		DelayLineCart<Integer, Integer> c1 = new DelayLineCart<>(1, 1, 10+1, TimeUnit.MILLISECONDS);
		DelayLineCart<Integer, Integer> c2 = new DelayLineCart<>(2, 2, 10+2, TimeUnit.MILLISECONDS);
		DelayLineCart<Integer, Integer> c3 = new DelayLineCart<>(3, 3, 10+3, TimeUnit.MILLISECONDS);
		DelayLineCart<Integer, Integer> c4 = new DelayLineCart<>(4, 4, 10+4, TimeUnit.MILLISECONDS);
		DelayLineCart<Integer, Integer> c5 = new DelayLineCart<>(5, 5, 10+5, TimeUnit.MILLISECONDS);
		
		c.add(c1);
		c.add(c4);
		c.add(c2);
		c.add(c3);
		c.add(c5);
		
		Thread.sleep(100);
		
		assertEquals(1, res.get(0).intValue());
		assertEquals(2, res.get(1).intValue());
		assertEquals(3, res.get(2).intValue());
		assertEquals(4, res.get(3).intValue());
		assertEquals(5, res.get(4).intValue());
		
	}
	

}
