package com.aegisql.conveyor.utils;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.utils.collection.CollectionBuilder;
import com.aegisql.conveyor.utils.collection.CollectionCompleteCart;
import com.aegisql.conveyor.utils.collection.CollectionConveyor;
import com.aegisql.conveyor.utils.collection.CollectionItemCart;

public class CollectionBuilderTest {

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
	public void testCollectionBuilder() throws InterruptedException {
		CollectionBuilder<Integer> b = new CollectionBuilder<>(100, TimeUnit.MILLISECONDS);
		
		assertFalse(b.test());
		assertTrue(b.getDelay(TimeUnit.MILLISECONDS) > 0);
		System.out.println(b.getDelay(TimeUnit.MILLISECONDS));
		Thread.sleep(101);
		System.out.println(b.getDelay(TimeUnit.MILLISECONDS));
		assertTrue(b.getDelay(TimeUnit.MILLISECONDS) < 0);
		
	}
	
	@Test
	public void testCollectionConveyor() throws InterruptedException {

		CollectionConveyor<Integer,Integer> b = new CollectionConveyor<>();
		
		AtomicInteger ai = new AtomicInteger(0);
		AtomicInteger aii = new AtomicInteger(0);
		
		b.setBuilderSupplier( () -> new CollectionBuilder<>(100, TimeUnit.MILLISECONDS) );
		b.setScrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		});
		b.setResultConsumer((list)->{
			System.out.println(list);
			ai.incrementAndGet();
			aii.addAndGet(list.size());
		});
		b.setExpirationCollectionInterval(100, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 100; i++) {
			b.add(new CollectionItemCart<Integer,Integer>(1,i));
		}

		b.add(new CollectionCompleteCart<Integer,Integer>(1));

		Thread.sleep(50);
		assertEquals(1, ai.get());
		assertEquals(100, aii.get());
		System.out.println("COL:"+b.getCollectorSize());
		System.out.println("DEL:"+b.getDelayedQueueSize());
		System.out.println("IN :"+b.getInputQueueSize());
		assertEquals(0,b.getCollectorSize());
		assertEquals(0,b.getDelayedQueueSize());
		assertEquals(0,b.getInputQueueSize());
	}

	@Test
	public void testCollectionConveyorExpire() throws InterruptedException {

		CollectionConveyor<Integer,Integer> b = new CollectionConveyor<>();
		
		AtomicInteger ai = new AtomicInteger(0);
		AtomicInteger aii = new AtomicInteger(0);
		
		b.setBuilderSupplier( () -> new CollectionBuilder<>(100, TimeUnit.MILLISECONDS) );
		b.setScrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		});
		b.setResultConsumer((list)->{
			System.out.println(list);
			ai.incrementAndGet();
			aii.addAndGet(list.size());
		});
		b.setOnTimeoutAction((builder)->{
			System.out.println("TIMEOUT:"+builder.get());
		});
		b.setExpirationCollectionInterval(50, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 100; i++) {
			b.add(new CollectionItemCart<Integer,Integer>(1,i));
		}

		Thread.sleep(150);
		System.out.println("COL:"+b.getCollectorSize());
		System.out.println("DEL:"+b.getDelayedQueueSize());
		System.out.println("IN :"+b.getInputQueueSize());
		assertEquals(0,b.getCollectorSize());
		assertEquals(0,b.getDelayedQueueSize());
		assertEquals(0,b.getInputQueueSize());
	}

}
