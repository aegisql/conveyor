package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.utils.collection.CollectionBuilder;
import com.aegisql.conveyor.utils.collection.CollectionConveyor;
import org.junit.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

// TODO: Auto-generated Javadoc
/**
 * The Class CollectionBuilderTest.
 */
public class CollectionBuilderTest {

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
		b.scrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		}).set();
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
		b.scrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		}).set();
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
