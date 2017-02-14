package com.aegisql.conveyor.utils;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.utils.map.MapBuilder;
import com.aegisql.conveyor.utils.map.MapConveyor;

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
	 * @throws ExecutionException 
	 */
	@Test
	public void testMapBuilder() throws InterruptedException, ExecutionException {
		MapConveyor<Integer, String, String> mc = new MapConveyor<>();
		
		mc.setBuilderSupplier( ()-> new MapBuilder<>() );
		
		mc.setResultConsumer(bin->{
			System.out.println(bin);
		});
		mc.setScrapConsumer(bin->fail("Failde "+bin));
		
		PartLoader<Integer, String,?,?,?> pl = mc.part().id(1);
		pl.label("FIRST").value("ONE").place();
		pl.label("SECOND").value("TWO").place();
		pl.label("THIRD").value("THREE").place();
		CompletableFuture<Boolean> last = (CompletableFuture<Boolean>) pl.label(null).value(null).place();
		assertTrue(last.get());
		Thread.sleep(10);
	}

}
