package com.aegisql.conveyor.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.consumers.scrap.ScrapQueue;
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
				
		ResultQueue<String, List<Integer>> lq = ResultQueue.of(b);
		ScrapQueue<String> ls = ScrapQueue.of(b);
		
		b.setBuilderSupplier( () -> new BatchCollectingBuilder<>(10, 50, TimeUnit.MILLISECONDS) );
		b.scrapConsumer(ls).set();
		b.resultConsumer(lq).andThen(LogResult.stdOut(b)).set();
		b.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 102; i++) {
			b.part().value(i).place();
		}
		
		Thread.sleep(40);
		assertEquals(10,lq.size());
		System.out.println(lq);
		
		Thread.sleep(200);
		assertEquals(11,lq.size());
		assertEquals(0,ls.size());
		System.out.println(lq);
		System.out.println("COL:"+b.getCollectorSize());
		System.out.println("DEL:"+b.getDelayedQueueSize());
		System.out.println("IN :"+b.getInputQueueSize());
		assertEquals(0,b.getCollectorSize());
		assertEquals(0,b.getDelayedQueueSize());
		assertEquals(0,b.getInputQueueSize());
	}

	@Test
	public void testBatchConveyorIterable() throws InterruptedException {

		BatchConveyor<Integer> b = new BatchConveyor<>();
				
		ResultQueue<String, List<Integer>> lq = ResultQueue.of(b);
		ScrapQueue<String> ls = ScrapQueue.of(b);
		
		b.setBuilderSupplier( () -> new BatchCollectingBuilder<>(10, 50, TimeUnit.MILLISECONDS) );
		b.scrapConsumer(ls).set();
		b.resultConsumer(lq).andThen(LogResult.stdOut(b)).set();
		b.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		List<Integer> l = new ArrayList<>();
		for(int i = 0; i < 102; i++) {
			l.add(i);
			if(l.size() == 5) {
				b.part().value(l).place();
				l = new ArrayList<>();
			}
		}
		CompletableFuture<Boolean> f = b.part().value(l).place();
		f.join();
		assertEquals(10,lq.size());
		System.out.println(lq);
		
		Thread.sleep(200);
		assertEquals(11,lq.size());
		assertEquals(0,ls.size());
		System.out.println(lq);
		System.out.println("COL:"+b.getCollectorSize());
		System.out.println("DEL:"+b.getDelayedQueueSize());
		System.out.println("IN :"+b.getInputQueueSize());
		assertEquals(0,b.getCollectorSize());
		assertEquals(0,b.getDelayedQueueSize());
		assertEquals(0,b.getInputQueueSize());
	}

	
	@Test
	public void testBatchConveyorComplete() throws InterruptedException, ExecutionException {

		BatchConveyor<Integer> b = new BatchConveyor<>();
				
		ResultQueue<String, List<Integer>> lq = ResultQueue.of(b);
		ScrapQueue<String> ls = ScrapQueue.of(b);
		
		b.setBuilderSupplier( () -> new BatchCollectingBuilder<>(10, 50, TimeUnit.MILLISECONDS) );
		b.scrapConsumer(ls).set();
		b.resultConsumer(lq).andThen(LogResult.stdOut(b)).set();
		b.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 102; i++) {
			b.part().value(i).place();
		}
		b.completeBatch().get();
		assertEquals(11,lq.size());
		assertEquals(0,ls.size());
		System.out.println(lq);
		System.out.println("COL:"+b.getCollectorSize());
		System.out.println("DEL:"+b.getDelayedQueueSize());
		System.out.println("IN :"+b.getInputQueueSize());
		assertEquals(0,b.getCollectorSize());
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
		b.scrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		}).set();
		b.resultConsumer((list)->{
			System.out.println(list);
			ai.incrementAndGet();
			aii.addAndGet(list.product.size());
		}).set();
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
