package com.aegisql.conveyor.utils;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.utils.collection.CollectionBuilder;
import com.aegisql.conveyor.utils.collection.CollectionCompleteCart;
import com.aegisql.conveyor.utils.collection.CollectionConveyor;
import com.aegisql.conveyor.utils.collection.CollectionItemCart;

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
	 * Test collection builder.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testCollectionBuilder() throws InterruptedException {
		CollectionBuilder<Integer> b = new CollectionBuilder<>(100, TimeUnit.MILLISECONDS);
		
		assertFalse(b.test());
		assertTrue(b.toDelayed().getDelay(TimeUnit.MILLISECONDS) > 0);
		System.out.println(b.toDelayed().getDelay(TimeUnit.MILLISECONDS));
		Thread.sleep(101);
		System.out.println(b.toDelayed().getDelay(TimeUnit.MILLISECONDS));
		assertTrue(b.toDelayed().getDelay(TimeUnit.MILLISECONDS) < 0);
		
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
		
		b.setBuilderSupplier( () -> new CollectionBuilder<>(100, TimeUnit.MILLISECONDS) );
		b.setScrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		});
		b.setResultConsumer((list)->{
			System.out.println(list);
			ai.incrementAndGet();
			aii.addAndGet(list.product.size());
		});
		b.setOnTimeoutAction((builder)->{
			System.out.println("TIMEOUT:"+builder.get());
		});
		b.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 100; i++) {
			b.place(new CollectionItemCart<Integer,Integer>(1,i));
		}

		b.place(new CollectionCompleteCart<Integer,Integer>(1));

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
		
		b.setBuilderSupplier( () -> new CollectionBuilder<>(100, TimeUnit.MILLISECONDS) );
		b.setScrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		});
		b.setResultConsumer((list)->{
			System.out.println(list);
			ai.incrementAndGet();
			aii.addAndGet(list.product.size());
		});
		b.setOnTimeoutAction((builder)->{
			System.out.println("TIMEOUT:"+builder.get());
		});
		b.setIdleHeartBeat(50, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 100; i++) {
			b.place(new CollectionItemCart<Integer,Integer>(1,i));
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
