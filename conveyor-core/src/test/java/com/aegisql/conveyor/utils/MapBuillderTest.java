package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.utils.map.MapBuilder;
import com.aegisql.conveyor.utils.map.MapConveyor;
import org.junit.*;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// TODO: Auto-generated Javadoc

/**
 * The Class MapBuillderTest.
 */
public class MapBuillderTest {

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
	 * Test map builder.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException   the execution exception
	 */
	@Test
	public void testMapBuilder() throws InterruptedException, ExecutionException {
		MapConveyor<Integer, String, String> mc = new MapConveyor<>();
		
		mc.setBuilderSupplier( ()-> new MapBuilder<>() );
		
		mc.resultConsumer(LogResult.stdOut(mc)).set();
		mc.scrapConsumer(bin->fail("Failed "+bin)).set();
		
		PartLoader<Integer, String> pl = mc.part().id(1);
		pl.label("FIRST").value("ONE").place();
		pl.label("SECOND").value("TWO").place();
		pl.label("THIRD").value("THREE").place();
		CompletableFuture<Boolean> last = (CompletableFuture<Boolean>) pl.label(null).value(null).place();
		assertTrue(last.get());
		Thread.sleep(10);
	}

	@Test
	public void constructorTests() {
		MapBuilder mb1 = new MapBuilder(1000);
		MapBuilder mb2 = new MapBuilder(1000, TimeUnit.SECONDS);
		MapBuilder mb3 = new MapBuilder(new HashMap(),1000);
		MapBuilder mb4 = new MapBuilder(new HashMap(),1000, TimeUnit.SECONDS);
		MapBuilder mb5 = new MapBuilder(new HashMap());
	}
}
