package com.aegisql.conveyor.cart.command;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import org.junit.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.junit.Assert.*;

// TODO: Auto-generated Javadoc

/**
 * The Class CommandTest.
 */
public class CommandTest {

	/**
	 * The Class TestOut.
	 */
	public static class TestOut implements Supplier<String> {
		
		/** The ready. */
		boolean ready = false;
		
		/** The out. */
		String out = "TestOut";
		
		/**
		 * Instantiates a new test out.
		 */
		public TestOut() {
			
		}

		/* (non-Javadoc)
		 * @see java.util.function.Supplier#get()
		 */
		@Override
		public String get() {
			return out;
		}
	}

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
	 * Test check command.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException from the future
	 */
	@Test
	public void testCheckCommand() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, String> ac = new AssemblingConveyor<>();

		TestOut testOut = new TestOut();
		
		ac.setBuilderSupplier(()->{
			return testOut;
		});
		ac.setDefaultCartConsumer((label, value, builder) -> {
			TestOut to = (TestOut) builder;
			to.out = value.toString();
			System.out.println("OUT set "+to.out);
		});
		
		ac.setReadinessEvaluator((toBuilder)->{
			TestOut to = (TestOut)toBuilder;
			return to.ready;
		});
		
		StringBuilder out = new StringBuilder();
		
		ac.resultConsumer().first((bin)->{
			System.out.println(bin);
			out.append(bin.product);
		}).set();
		
		Cart<Integer,String,String> c = new ShoppingCart<>(1, "Test", "");
		
		GeneralCommand<Integer,String> check = new GeneralCommand<>(1,"",CommandLabel.CHECK_BUILD,0L);
		
		ac.place(c);
		
		Thread.sleep(100);
		CompletableFuture<Boolean> chk1 = ac.command().id(1).check();
		assertTrue(chk1.get());
		assertEquals(0, out.length());
		testOut.ready = true;
		ac.command(check);
		Thread.sleep(100);
		
		assertEquals("Test",out.toString());
		ac.command(check);
		Thread.sleep(50);
		CompletableFuture<Boolean> chk2 = ac.command().id(2).check();
		assertFalse(chk2.get());
		
	}

}
