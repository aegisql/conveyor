package com.aegisql.conveyor.consumers.result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.ScrapBin.FailureType;
import com.aegisql.conveyor.consumers.scrap.FirstScraps;
import com.aegisql.conveyor.consumers.scrap.LastScrapReference;
import com.aegisql.conveyor.consumers.scrap.LastScraps;
import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.consumers.scrap.PrintStreamScrap;
import com.aegisql.conveyor.consumers.scrap.ScrapCounter;
import com.aegisql.conveyor.consumers.scrap.ScrapMap;
import com.aegisql.conveyor.consumers.scrap.ScrapQueue;
import com.aegisql.conveyor.consumers.scrap.StreamScrap;
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
		sc.resultConsumer().first(LogResult.info(sc,"UnitTestLogger").andThen(b->usr.set(b.product))).set();
		sc.scrapConsumer(LogScrap.error(sc,"UnitTestLogger")).set();
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(usr.get());
	}

	@Test
	public void testCountConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		ResultCounter<String,User> rc = ResultCounter.of(sc); 
		sc.resultConsumer().first(rc.andThen(b->usr.set(b.product))).set();
		ScrapCounter<String> scc = ScrapCounter.of(sc); 
		sc.scrapConsumer(scc).set();
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(usr.get());
		assertEquals(1, rc.get());
		assertEquals(1, scc.get());
	}

	@Test
	public void testCountFilterConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		ResultCounter<String,User> rc = ResultCounter.of(sc);
		
		sc.resultConsumer().first(rc.filterKey(k->"test".equals(k)).andThen(b->usr.set(b.product))).andThen(LogResult.stdOut(sc)).set();
		ScrapCounter<String> scc = ScrapCounter.of(sc); 

		sc.scrapConsumer(scc.filterFailureType(f->f.equals(FailureType.CART_REJECTED))).andThen(LogScrap.error(sc)).set();
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(usr.get());
		assertEquals(1, rc.get());
		assertEquals(1, scc.get());
	}

	
	@Test
	public void testRefConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		LastResultReference<String,User> q = LastResultReference.of(sc);
		LastScrapReference<String> s = LastScrapReference.of(sc);
		sc.setBuilderSupplier(StringToUserBuulder::new);
		sc.resultConsumer().first(q).set();
		sc.scrapConsumer(s).set();
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(q);
		assertNotNull(q.getCurrent());
		assertNotNull(s);
		assertNotNull(s.getCurrent());
		System.out.println(q);
		System.out.println(s);
	}

	@Test
	public void testFirstConsumers() throws InterruptedException, ExecutionException, TimeoutException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setDefaultBuilderTimeout(Duration.ofMillis(100));
		FirstResults<String,User> q = FirstResults.of(sc,2);
		FirstScraps<String> s = FirstScraps.of(sc,2);
		sc.setBuilderSupplier(StringToUserBuulder::new);
		sc.resultConsumer().first(q).set();
		sc.scrapConsumer(s).set();
		String csv = "John,Dow,199";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		sc.part().id("test1").value(csv+"0").place();
		sc.part().id("test2").value(csv+"1").place();
		sc.part().id("test3").value(csv+"2").place();
		CompletableFuture<Boolean> cf = sc.part().id("test4").value(csv+"3").place();
		cf.get();
		sc.completeAndStop().get();
		assertNotNull(q);
		assertNotNull(q.getFirst());
		assertNotNull(s);
		assertNotNull(s.getFirst());
		System.out.println(q);
		System.out.println(s);
	}

	
	@Test
	public void testLastConsumers() throws InterruptedException, ExecutionException, TimeoutException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setDefaultBuilderTimeout(Duration.ofMillis(100));
		LastResults<String,User> q = LastResults.of(sc,2);
		LastScraps<String> s = LastScraps.of(sc,2);
		sc.setBuilderSupplier(StringToUserBuulder::new);
		sc.resultConsumer().first(q).set();
		sc.scrapConsumer(s).set();
		String csv = "John,Dow,199";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		sc.part().id("test1").value(csv+"0").place();
		sc.part().id("test2").value(csv+"1").place();
		sc.part().id("test3").value(csv+"2").place();
		CompletableFuture<Boolean> cf = sc.part().id("test4").value(csv+"3").place();
		cf.get();
		sc.completeAndStop().get();
		assertNotNull(q);
		assertNotNull(q.getLast());
		assertNotNull(s);
		assertNotNull(s.getLast());
		System.out.println(q);
		System.out.println(s);
	}

	
	@Test
	public void testQueueConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		ResultQueue<String,User> q = ResultQueue.of(sc);
		ScrapQueue<String> s = ScrapQueue.of(sc);
		sc.setBuilderSupplier(StringToUserBuulder::new);
		sc.resultConsumer().first(q).set();
		sc.scrapConsumer(s).set();
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(q);
		assertEquals(1,q.size());
		assertNotNull(s);
		assertEquals(1,s.size());
		System.out.println(q);
		System.out.println(s);
	}

	@Test
	public void testMapConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		ResultMap<String,User> q = ResultMap.of(sc);
		ScrapMap<String> m = ScrapMap.of(sc);
		sc.setBuilderSupplier(StringToUserBuulder::new);
		sc.resultConsumer().first(q).set();
		sc.scrapConsumer(m).set();
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(q);
		assertEquals(1,q.size());
		assertTrue(q.containsKey("test"));
		assertNotNull(m);
		assertEquals(1,m.size());
		assertTrue(m.containsKey("bad"));
		assertNotNull(m.get("bad"));
		assertEquals(1,m.get("bad").size());
		System.out.println(q);
		System.out.println(m);
	}

	
	@Test
	public void testPrintStreamConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		sc.resultConsumer().first(PrintStreamResult.of(sc,System.out).andThen(PrintStreamResult.of(sc,System.out,u->u.getFirst()+" "+u.getLast())).andThen(b->usr.set(b.product))).set();
		sc.scrapConsumer(PrintStreamScrap.of(sc,System.err).andThen(PrintStreamScrap.of(sc,System.err,o->{
			return "ERROR: "+o;
		}))).set();
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		sc.completeAndStop().get();
		assertNotNull(usr.get());
	}

	@Test
	public void testStreamConsumers() throws InterruptedException, ExecutionException, IOException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		sc.resultConsumer().first(StreamResult.of(sc,"/tmp/testStreamConsumers.out").andThen(StreamResult.of(sc,bos,b->b.product.getFirst()+" "+b.product.getLast())).andThen(b->usr.set(b.product))).set();
		sc.scrapConsumer(StreamScrap.of(sc,"/tmp/testStreamConsumers.err")).set();
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		sc.completeAndStop().get();
		assertNotNull(usr.get());
		System.out.println(new String(bos.toByteArray()));
	}

}
