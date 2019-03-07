package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.consumers.result.ForwardResult;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.result.ResultMap;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingBuilder;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor;
import org.junit.*;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;

// TODO: Auto-generated Javadoc

/**
 * The Class ScalarConvertingConveyorTest.
 */
public class ScalarConvertingConveyorTest {


	/**
	 * The Class StringToUserBuulder.
	 */
	public static class StringToUserBuulder extends ScalarConvertingBuilder<String,User> {
		
		public StringToUserBuulder() {
			super(1000,TimeUnit.MILLISECONDS);
		}
		
		/* (non-Javadoc)
		 * @see java.util.function.Supplier#get()
		 */
		@Override
		public User get() {
			String[] fields = scalar.split(",");
			return new User(fields[0], fields[1], Integer.parseInt(fields[2]));
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
	 * Test scalar converting conveyor.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException from the future
	 */
	@Test
	public void testScalarConvertingConveyorWithPart() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		sc.resultConsumer(u->{
			System.out.println("RESULT: "+u);
			usr.set(u.product);
		}).set();
		String csv = "John,Dow,1990";
				
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(usr.get());
	}

	class ToLower extends ScalarConvertingBuilder<String,String> {
		public String get() {
			return scalar.toLowerCase();
		}
	}

	class ToSet extends ScalarConvertingBuilder<String, Set<String>> {
		public Set<String> get() {
			return Arrays.stream(scalar.split("\\s+")).collect(Collectors.toSet());
		}
	}

	@Test
	public void testChainedScalarConv() {

		ResultMap<Integer,Set<String>> res = new ResultMap<>();

		ScalarConvertingConveyor<Integer, String, String> toLower = new ScalarConvertingConveyor<>();
		toLower.setBuilderSupplier(ToLower::new);
		toLower.setName("toLower");

		ScalarConvertingConveyor<Integer, String, Set<String>> toSet = new ScalarConvertingConveyor<>();
		toSet.setBuilderSupplier(ToSet::new);
		toSet.setName("toSet");
		toSet.resultConsumer(res).andThen(LogResult.stdOut(toSet)).set();

		ForwardResult.from(toLower).to(toSet).label("RESULT").bind();

		CompletableFuture<Set<String>> future = toSet.build().id(1).createFuture();
		toLower.part().id(1).value("to Be Or not TO be").place();
		future.join();

	}

}
