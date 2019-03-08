package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.consumers.result.ForwardResult;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.reflection.SimpleConveyor;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingBuilder;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor;
import org.junit.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
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

	abstract class ScalarHolder<T,R> implements Testing, Supplier<R> {
		T value;
		@Override
		public boolean test() {
			return true;
		}
	}

	class ToLower extends ScalarHolder<String,String> {
		public String get() {
			return value.toLowerCase();
		}
	}

	class ToSet extends ScalarHolder<String, Set<String>> {
		public Set<String> get() {
			return Arrays.stream(value.split("\\s+")).collect(Collectors.toSet());
		}
	}

	@Test
	public void testChainedScalarConv() {

		SimpleConveyor<Integer, String> toLower = new SimpleConveyor<>(ToLower::new);
		toLower.setName("toLower");

		SimpleConveyor<Integer, Set<String>> toSet = new SimpleConveyor<>(ToSet::new);
		toSet.setName("toSet");
		toSet.resultConsumer(LogResult.debug(toSet)).set();

		ForwardResult.from(toLower).to(toSet).label("value").bind();

		toLower
				.part()
				.ttl(Duration.ofMillis(1000))
				.id(1)
				.label("value")
				.value("to Be Or not TO be")
				.place();
		toLower.completeAndStop().join();
		toSet.completeAndStop();
	}

}
