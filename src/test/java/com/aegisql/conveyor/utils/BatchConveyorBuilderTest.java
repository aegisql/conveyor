package com.aegisql.conveyor.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.utils.batch.BatchCollectingBuilder;
import com.aegisql.conveyor.utils.batch.BatchConveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class BatchConveyorBuilderTest.
 */
public class BatchConveyorBuilderTest {

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
	 * Test batch collecting builder.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testBatchCollectingBuilder() throws InterruptedException {
		BatchCollectingBuilder<Integer> b = new BatchCollectingBuilder<>(10, 100, TimeUnit.MILLISECONDS);
		
		assertFalse(b.test());
		assertTrue(b.toDelayed().getDelay(TimeUnit.MILLISECONDS) > 0);
		System.out.println(b.toDelayed().getDelay(TimeUnit.MILLISECONDS));
		Thread.sleep(101);
		System.out.println(b.toDelayed().getDelay(TimeUnit.MILLISECONDS));
		assertTrue(b.toDelayed().getDelay(TimeUnit.MILLISECONDS) < 0);
		
	}
	
	/**
	 * Test batch conveyor.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testBatchConveyor() throws InterruptedException {

		BatchConveyor<Integer> b = new BatchConveyor<>();
		
		List<List<Integer>> l = new ArrayList<>();
		
		b.setBuilderSupplier( () -> new BatchCollectingBuilder<>(10, 50, TimeUnit.MILLISECONDS) );
		b.setScrapConsumer((obj)->{
			System.out.println(obj);
			l.add(null);
		});
		b.setResultConsumer((list)->{
			System.out.println(list);
			l.add(list.product);
		});
		b.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 102; i++) {
			b.part().value(i).place();
		}
		
		Thread.sleep(40);
		assertEquals(10,l.size());
		System.out.println(l);
		
		Thread.sleep(200);
		assertEquals(11,l.size());
		System.out.println(l);
		System.out.println("COL:"+b.getCollectorSize());
		System.out.println("DEL:"+b.getDelayedQueueSize());
		System.out.println("IN :"+b.getInputQueueSize());
		assertEquals(0,b.getCollectorSize());
		assertEquals(0,b.getDelayedQueueSize());
		assertEquals(0,b.getInputQueueSize());
	}

	/**
	 * Test batch conveyor with named cart.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testBatchConveyorWithNamedCart() throws InterruptedException {

		BatchConveyor<Integer> b = new BatchConveyor<>();
		
		AtomicInteger ai = new AtomicInteger(0);
		AtomicInteger aii = new AtomicInteger(0);
		
		b.setBuilderSupplier( () -> new BatchCollectingBuilder<>(10, 20, TimeUnit.MILLISECONDS) );
		b.setScrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		});
		b.setResultConsumer((list)->{
			System.out.println(list);
			ai.incrementAndGet();
			aii.addAndGet(list.product.size());
		});
		b.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 102; i++) {
			if(i % 2 == 0) {
				b.part().id("A").value(i).place();
			} else {
				b.part().id("B").value(i).place();
			}
		}
		Thread.sleep(20);
		assertEquals(10, ai.get());
		assertEquals(100, aii.get());
		Thread.sleep(200);
		System.out.println("COL:"+b.getCollectorSize());
		System.out.println("DEL:"+b.getDelayedQueueSize());
		System.out.println("IN :"+b.getInputQueueSize());
		assertEquals(12, ai.get());
		assertEquals(102, aii.get());
		assertEquals(0,b.getCollectorSize());
		assertEquals(0,b.getDelayedQueueSize());
		assertEquals(0,b.getInputQueueSize());
	}

}
