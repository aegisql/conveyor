package com.aegisql.conveyor.builder;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
		
		b.setBuilderSupplier( () -> new BatchCollectingBuilder<>(10, 100, TimeUnit.MILLISECONDS) );
		b.setScrapConsumer((exp,obj)->{
			System.out.println(exp+" "+obj);
		});
		b.setResultConsumer((list)->{
			System.out.println(list);
		});
		b.setExpirationCollectionInterval(100, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 102; i++) {
			b.add(new BatchCart<Integer>(i));
		}
		Thread.sleep(1000);
		System.out.println("COL:"+b.getCollectorSize());
		System.out.println("DEL:"+b.getDelayedQueueSize());
		System.out.println("IN :"+b.getInputQueueSize());
	}

}
