package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.ScrapBin.FailureType;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.consumers.scrap.*;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.utils.ScalarConvertingConveyorTest.StringToUserBuilder;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ResultConsumerTest {


	@BeforeAll
	public static void setUpBeforeClass() {
	}

	@AfterAll
	public static void tearDownAfterClass() {
	}

	@BeforeEach
	public void setUp() {
	}

	@AfterEach
	public void tearDown() {
	}

	public static <K,V> ProductBin getProductBin(K key, V val) {
		return new ProductBin(null,key,val,0,Status.READY,new HashMap<>(), null);
	}

	@Test
	public void testLogConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuilder::new);
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
	public void testAsynchResultConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuilder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		AtomicLong threadId = new AtomicLong();
		CompletableFuture<Object> f = new CompletableFuture<Object>();
		sc.resultConsumer().first(LogResult.info(sc,"UnitTestLogger").andThen(b->{
			usr.set(b.product);
			threadId.set(Thread.currentThread().threadId());
			f.complete(null);
		}).async()).set();
		sc.scrapConsumer(LogScrap.error(sc,"UnitTestLogger")).set();
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		f.get();
		assertNotNull(usr.get());
		assertFalse(Thread.currentThread().threadId() == threadId.get() );
		System.out.println("Current: "+Thread.currentThread().threadId()+" consumer's: "+threadId);
	}

	@Test
	public void testAsynchScrapConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuilder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		AtomicLong threadId = new AtomicLong();
		CompletableFuture<Object> f = new CompletableFuture<Object>();
		sc.resultConsumer().first(LogResult.info(sc,"UnitTestLogger").andThen(b->{
			usr.set(b.product);
		})).set();
		sc.scrapConsumer(LogScrap.error(sc,"UnitTestLogger").andThen(s->{
			threadId.set(Thread.currentThread().threadId());
			f.complete(null);
		}).async()).set();
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		f.get();
		assertNotNull(usr.get());
		assertFalse(Thread.currentThread().threadId() == threadId.get() );
		System.out.println("Current: "+Thread.currentThread().threadId()+" consumer's: "+threadId);
	}

	
	
	@Test
	public void testCountConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuilder::new);
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
		sc.setBuilderSupplier(StringToUserBuilder::new);
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
		sc.setBuilderSupplier(StringToUserBuilder::new);
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
	public void testFirstConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setDefaultBuilderTimeout(Duration.ofMillis(100));
		FirstResults<String,User> q = FirstResults.of(sc,2);
		FirstScraps<String> s = FirstScraps.of(sc,2);
		sc.setBuilderSupplier(StringToUserBuilder::new);
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
	public void testObservableConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setDefaultBuilderTimeout(Duration.ofMillis(100));
		ObservableResultConsumer<String,User> q = ObservableResultConsumer.of(sc);
		ObservableScrapConsumer<String> s = ObservableScrapConsumer.of(sc);

		assertNotNull(q);
		assertNotNull(s);

		sc.setBuilderSupplier(StringToUserBuilder::new);
		sc.resultConsumer().first(q).set();
		sc.scrapConsumer(s).set();
		String csv = "John,Dow,199";
		CompletableFuture<ScrapBin<String, Object>> cf = s.waitFor("bad");
		CompletableFuture<User> test1 = q.waitFor("test1");
		CompletableFuture<User> test2 = q.waitFor("test2");
		CompletableFuture<User> test3 = q.waitFor("test3");
		CompletableFuture<User> test4 = q.waitFor("test4");
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		sc.part().id("test1").value(csv+"0").place();
		sc.part().id("test2").value(csv+"1").place();
		sc.part().id("test3").value(csv+"2").place();
		sc.part().id("test4").value(csv+"3").place();
		sc.completeAndStop().get();
		ScrapBin<String, Object> bin = cf.get();

		assertNotNull(test1.get());
		assertNotNull(test2.get());
		assertNotNull(test3.get());
		assertNotNull(test4.get());
		assertNotNull(bin);

		System.out.println(bin);
	}


	@Test
	public void testLastConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setDefaultBuilderTimeout(Duration.ofMillis(100));
		LastResults<String,User> q = LastResults.of(sc,2);
		LastScraps<String> s = LastScraps.of(sc,2);
		sc.setBuilderSupplier(StringToUserBuilder::new);
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
		sc.setBuilderSupplier(StringToUserBuilder::new);
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
		sc.setBuilderSupplier(StringToUserBuilder::new);
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
		sc.setBuilderSupplier(StringToUserBuilder::new);
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
		sc.setBuilderSupplier(StringToUserBuilder::new);
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

	@Test
	public void demoTest() {
		ResultConsumer<Integer, String> rc = bin->{
			System.out.println("key="+bin.key);
		};
		rc = rc.andThen(bin->{
			System.out.println("product="+bin.product);
		});
		ResultCounter<Integer, String> counter = new ResultCounter<>();
		rc = rc.andThen(counter);
		rc = rc.filterStatus(status->status.equals(Status.TIMED_OUT));
	}

	@Test
	public void functionalTest() {
		ResultConsumer<Integer,String> rc = bin->{
			System.out.println(bin);
		};
		ResultConsumer<Integer,String> rc2 = rc.filter(bin->true);
		ResultConsumer<Integer,String> rc3 = rc.filterResult(res->true);
		ResultConsumer<Integer,String> rc4 = rc.filterStatus(status->true);
		assertNotNull(rc2);
		assertNotNull(rc3);
		assertNotNull(rc4);
		rc.accept(getProductBin(1,"test 1"));
		rc2.accept(getProductBin(2,"test 2"));
		rc3.accept(getProductBin(3,"test 3"));
		rc4.accept(getProductBin(4,"test 4"));
	}

	@Test
	public void  filterByPropertyTest() {

		AtomicBoolean res1 = new AtomicBoolean(false);

		ResultConsumer<Integer,String> rc1 = bin->{
			res1.set(true);
		};
		rc1 = rc1.filterProperty("test",s->s.equals("TEST"));

		ProductBin<Integer,String> bin1 = getProductBin(1,"val");
		bin1.properties.put("test","BEST");
		rc1.accept(bin1);
		assertFalse(res1.get());
		bin1.properties.put("test","TEST");
		rc1.accept(bin1);
		assertTrue(res1.get());
	}

	@Test
	public void  filterByPropertyEqualsTest() {

		AtomicBoolean res1 = new AtomicBoolean(false);

		ResultConsumer<Integer,String> rc1 = bin->{
			res1.set(true);
		};
		rc1 = rc1.propertyEquals("test","TEST");

		ProductBin<Integer,String> bin1 = getProductBin(1,"val");
		bin1.properties.put("test","BEST");
		rc1.accept(bin1);
		assertFalse(res1.get());
		bin1.properties.put("test","TEST");
		rc1.accept(bin1);
		assertTrue(res1.get());
	}


}
