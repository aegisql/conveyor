package com.aegisql.conveyor.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.utils.collection.CollectionBuilder;
import com.aegisql.conveyor.utils.collection.CollectionConveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class CollectionBuilderTest.
 */
public class CollectionBuilderTest {

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
	 * Test collection conveyor.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testCollectionConveyor() throws InterruptedException {

		CollectionConveyor<Integer,Integer> b = new CollectionConveyor<>();
		
		AtomicInteger ai = new AtomicInteger(0);
		AtomicInteger aii = new AtomicInteger(0);
		
		b.setBuilderSupplier( () -> new CollectionBuilder<>() );
		b.setScrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		});
		b.resultConsumer((list)->{
			System.out.println(list);
			ai.incrementAndGet();
			aii.addAndGet(list.product.size());
		}).set();
		b.setOnTimeoutAction((builder)->{
			System.out.println("TIMEOUT:"+builder.get());
		});
		b.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 100; i++) {
			b.part().id(1).value(i).place();
		}

		b.part().id(1).label(b.COMPLETE).place();

		Thread.sleep(110);
		assertEquals(1, ai.get());
		assertEquals(100, aii.get());
		System.out.println("COL:"+b.getCollectorSize());
		System.out.println("DEL:"+b.getDelayedQueueSize());
		System.out.println("IN :"+b.getInputQueueSize());
		assertEquals(0,b.getCollectorSize());
		assertEquals(0,b.getDelayedQueueSize());
		assertEquals(0,b.getInputQueueSize());
	}

	/**
	 * Test collection conveyor expire.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testCollectionConveyorExpire() throws InterruptedException {

		CollectionConveyor<Integer,Integer> b = new CollectionConveyor<>();
		
		AtomicInteger ai = new AtomicInteger(0);
		AtomicInteger aii = new AtomicInteger(0);
		
		b.setBuilderSupplier( BuilderSupplier.of(new CollectionBuilder<Integer>()).expire(100, TimeUnit.MILLISECONDS) );
		b.setScrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		});
		b.resultConsumer((list)->{
			System.out.println(list);
			ai.incrementAndGet();
			aii.addAndGet(list.product.size());
		}).set();
		b.setOnTimeoutAction((builder)->{
			System.out.println("TIMEOUT:"+builder.get());
		});
		b.setIdleHeartBeat(50, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 100; i++) {
			b.part().id(1).value(i).place();
		}

		Thread.sleep(1500);
		System.out.println("COL:"+b.getCollectorSize());
		System.out.println("DEL:"+b.getDelayedQueueSize());
		System.out.println("IN :"+b.getInputQueueSize());
		assertEquals(0,b.getCollectorSize());
		assertEquals(0,b.getDelayedQueueSize());
		assertEquals(0,b.getInputQueueSize());
	}

}
