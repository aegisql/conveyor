package com.aegisql.conveyor.consumers.result;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.consumers.scrap.PrintStreamScrap;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.utils.ScalarConvertingConveyorTest.StringToUserBuulder;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor;

public class ResultConsumerTest {


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
	public void testLogConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		sc.setResultConsumer(LogResult.info(sc).andThen(b->usr.set(b.product)));
		sc.setScrapConsumer(LogScrap.error(sc));
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(usr.get());
	}

	@Test
	public void testPrintStreamConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		sc.setResultConsumer(PrintStreamResult.of(sc,System.out).andThen(PrintStreamResult.of(sc,System.out,u->u.getFirst()+" "+u.getLast())).andThen(b->usr.set(b.product)));
		sc.setScrapConsumer(PrintStreamScrap.of(sc,System.err).andThen(PrintStreamScrap.of(sc,System.err,o->{
			return "ERROR: "+o;
		})));
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(usr.get());
	}

}
