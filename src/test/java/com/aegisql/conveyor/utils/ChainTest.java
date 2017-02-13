package com.aegisql.conveyor.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.utils.ScalarConvertingConveyorTest.StringToUserBuulder;
import com.aegisql.conveyor.utils.batch.BatchCollectingBuilder;
import com.aegisql.conveyor.utils.batch.BatchConveyor;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class ChainTest.
 */
public class ChainTest {

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
	 * Test chain.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void testChain() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> scalarConveyor = new ScalarConvertingConveyor<>();
		scalarConveyor.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		String csv1 = "John,Dow,1990";
		String csv2 = "John,Smith,1991";
		
		BatchConveyor<User> batchConveyor = new BatchConveyor<>();
		
		AtomicInteger ai  = new AtomicInteger(0);
		AtomicInteger aii = new AtomicInteger(0);
		
		batchConveyor.setBuilderSupplier( () -> new BatchCollectingBuilder<>(2, 10, TimeUnit.MILLISECONDS) );
		batchConveyor.setScrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
			fail("Scrap unexpected in batch conveyor");
		});
		batchConveyor.setResultConsumer((list)->{
			System.out.println(list.product);
			ai.incrementAndGet();
			aii.addAndGet(list.product.size());
		});
		batchConveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);		
				
		ChainResult<String, User, String> chain = new ChainResult(batchConveyor,batchConveyor.BATCH);

		scalarConveyor.setResultConsumer(chain.andThen(u->{
			System.out.println("RESULT: "+u.product);
			usr.set(u.product);
		}));
		
		scalarConveyor.part().id("test1").value(csv1).place();
		CompletableFuture<Boolean> f = scalarConveyor.part().id("test2").value(csv2).place();

		f.get();
		assertNotNull(usr.get());
	}

}
