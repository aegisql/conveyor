package com.aegisql.conveyor.utils;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.utils.batch.BatchCart;
import com.aegisql.conveyor.utils.batch.BatchCollectingBuilder;
import com.aegisql.conveyor.utils.batch.BatchConveyor;

public class BatchConveyorBuilderTest {

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
	public void testBatchCollectingBuilder() throws InterruptedException {
		BatchCollectingBuilder<Integer> b = new BatchCollectingBuilder<>(10, 100, TimeUnit.MILLISECONDS);
		
		assertFalse(b.test());
		assertTrue(b.getDelay(TimeUnit.MILLISECONDS) > 0);
		System.out.println(b.getDelay(TimeUnit.MILLISECONDS));
		Thread.sleep(101);
		System.out.println(b.getDelay(TimeUnit.MILLISECONDS));
		assertTrue(b.getDelay(TimeUnit.MILLISECONDS) < 0);
		
	}
	
	@Test
	public void testBatchConveyor() throws InterruptedException {

		BatchConveyor<Integer> b = new BatchConveyor<>();
		
		AtomicInteger ai = new AtomicInteger(0);
		AtomicInteger aii = new AtomicInteger(0);
		
		b.setBuilderSupplier( () -> new BatchCollectingBuilder<>(10, 10, TimeUnit.MILLISECONDS) );
		b.setScrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		});
		b.setResultConsumer((list)->{
			System.out.println(list);
			ai.incrementAndGet();
			aii.addAndGet(list.product.size());
		});
		b.setExpirationCollectionInterval(100, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 102; i++) {
			b.add(new BatchCart<Integer>(i));
		}

		Thread.sleep(90);
		assertEquals(10, ai.get());
		assertEquals(100, aii.get());
		Thread.sleep(200);
		System.out.println("COL:"+b.getCollectorSize());
		System.out.println("DEL:"+b.getDelayedQueueSize());
		System.out.println("IN :"+b.getInputQueueSize());
		assertEquals(11, ai.get());
		assertEquals(102, aii.get());
		assertEquals(0,b.getCollectorSize());
		assertEquals(0,b.getDelayedQueueSize());
		assertEquals(0,b.getInputQueueSize());
	}

	@Test
	public void testBatchConveyorWithNamedCart() throws InterruptedException {

		BatchConveyor<Integer> b = new BatchConveyor<>();
		
		AtomicInteger ai = new AtomicInteger(0);
		AtomicInteger aii = new AtomicInteger(0);
		
		b.setBuilderSupplier( () -> new BatchCollectingBuilder<>(10, 10, TimeUnit.MILLISECONDS) );
		b.setScrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		});
		b.setResultConsumer((list)->{
			System.out.println(list);
			ai.incrementAndGet();
			aii.addAndGet(list.product.size());
		});
		b.setExpirationCollectionInterval(100, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 102; i++) {
			if(i % 2 == 0) {
				b.add(new BatchCart<Integer>("A",i));
			} else {
				b.add(new BatchCart<Integer>("B",i));
			}
		}
		Thread.sleep(50);
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
