package com.aegisql.conveyor.loaders;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import com.aegisql.conveyor.meta.ConveyorMetaInfoBuilder;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import com.aegisql.conveyor.user.UserBuilderEvents;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class CommandLoaderTest {

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

	@Test
	public void testFailMementoKey() {

		CommandLoader cl0 = new CommandLoader<>(c -> {
			System.out.println("Final: " + c);
			CompletableFuture<Boolean> f = new CompletableFuture();
			f.complete(false);
			return f;
		});
		assertThrows(CompletionException.class,()->cl0.id(1).memento().join());
	}
	@Test
	public void testSingleKey() {
		long current = System.currentTimeMillis();

		CommandLoader cl0 = new CommandLoader<>(c->{
			System.out.println("Final: "+c);
			return new CompletableFuture<>();
		});
		
		System.out.println(cl0);

		assertTrue(cl0.creationTime >= current);
		
		current = cl0.creationTime;

		CommandLoader cl1 = cl0.id(1);
		System.out.println(cl1);
        var cl1prop = cl1.addProperties(Map.of("a","A","b","B"));
        System.out.println(cl1prop);

		assertEquals(1,cl1.key);
		assertEquals(cl0.creationTime,cl1.creationTime);

		cl0.creationTime(0).creationTime(Instant.now());

		CommandLoader cl2et = cl1.expirationTime(current+1000);
		CommandLoader cl2in = cl1.expirationTime(Instant.ofEpochMilli(current+1000));
		CommandLoader cl2ttl = cl1.ttl(1000,TimeUnit.MILLISECONDS);
		CommandLoader cl2dur = cl1.ttl(Duration.ofMillis(1000));
		
		System.out.println(cl2et);
		System.out.println(cl2in);
		System.out.println(cl2ttl);
		System.out.println(cl2dur);
		assertEquals(cl2et.creationTime,cl2in.creationTime);
		assertEquals(cl2et.creationTime,cl2ttl.creationTime);
		assertEquals(cl2et.creationTime,cl2dur.creationTime);

		assertEquals(0,cl2in.ttlMsec);
		assertEquals(0,cl2et.ttlMsec);
		assertEquals(1000,cl2ttl.ttlMsec);
		assertEquals(1000,cl2dur.ttlMsec);

		CompletableFuture cancel = cl2in.cancel();
		assertNotNull(cancel);
		CompletableFuture create = cl2in.create();
		assertNotNull(create);
		CompletableFuture create2 = cl2in.create(BuilderSupplier.of(UserBuilder::new));
		assertNotNull(create2);
		CompletableFuture check = cl2in.check();
		assertNotNull(check);
		CompletableFuture reschedule = cl2in.reschedule();
		assertNotNull(reschedule);
		CompletableFuture timeout = cl2in.timeout();
		assertNotNull(timeout);
	}

	@Test
	public void testMultiKey() {
		long current = System.currentTimeMillis();

		MultiKeyCommandLoader cl0 = new MultiKeyCommandLoader<>(c->{
			System.out.println("Final: "+c);
			return new CompletableFuture();
		});
		
		System.out.println(cl0);

		assertTrue(cl0.creationTime >= current);
		
		current = cl0.creationTime;

		System.out.println(cl0);
		
		MultiKeyCommandLoader cl2et = cl0.expirationTime(current+1000);
		MultiKeyCommandLoader cl2in = cl0.expirationTime(Instant.ofEpochMilli(current+1000));
		MultiKeyCommandLoader cl2ttl = cl0.ttl(1000,TimeUnit.MILLISECONDS);
		MultiKeyCommandLoader cl2dur = cl0.ttl(Duration.ofMillis(1000));
		
		System.out.println(cl2et);
		System.out.println(cl2in);
		System.out.println(cl2ttl);
		System.out.println(cl2dur);
		assertEquals(cl2et.creationTime,cl2in.creationTime);
		assertEquals(cl2et.creationTime,cl2ttl.creationTime);
		assertEquals(cl2et.creationTime,cl2dur.creationTime);

		assertEquals(0,cl2in.ttlMsec);
		assertEquals(0,cl2et.ttlMsec);
		assertEquals(1000,cl2ttl.ttlMsec);
		assertEquals(1000,cl2dur.ttlMsec);

		CompletableFuture cancel = cl2in.cancel();
		assertNotNull(cancel);
		CompletableFuture reschedule = cl2in.reschedule();
		assertNotNull(reschedule);
		CompletableFuture timeout = cl2in.timeout();
		assertNotNull(timeout);

	}

	@Test
	public void testMultiKeyCancelCommand() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("testMultiKeyCancelCommand");
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.resultConsumer().first(LogResult.stdOut(c)).set();
		CompletableFuture<Boolean> cf1 = c.build().supplier(UserBuilder::new).id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().supplier(UserBuilder::new).id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().supplier(UserBuilder::new).id(3).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		
		CompletableFuture<User> f1 = c.future().id(1).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f3 = c.future().id(3).get();
		
		assertEquals(3,c.getCollectorSize());
		Thread.sleep(100);
		c.command().foreach().cancel();
		
		try {
			f3.get();
			fail("Not expected future");
		} catch(Exception e) {
			assertEquals(0,c.getCollectorSize());
		}	
	}

	@Test
	public void testMultiKeyRescheduleCommand() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("testMultiKeyRescheduleCommand");
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).set();
		c.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		CompletableFuture<Boolean> cf1 = c.build().id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().id(3).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		
		CompletableFuture<User> f1 = c.future().id(1).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f3 = c.future().id(3).get();
		
		assertEquals(3,c.getCollectorSize());
		
		c.command().foreach().ttl(500, TimeUnit.MILLISECONDS).reschedule();

		Thread.sleep(110);
		assertFalse(f1.isDone());
		assertFalse(f2.isDone());
		assertFalse(f3.isDone());
		Thread.sleep(410);
		assertTrue(f1.isDone());
		assertTrue(f2.isDone());
		assertTrue(f3.isDone());
		
	}

	@Test
	public void testMultiKeyTimeoutCommand() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("testMultiKeyTimeoutCommand");
		c.setBuilderSupplier(UserBuilder::new);
		c.resultConsumer().first(LogResult.stdOut(c)).set();
		c.setDefaultBuilderTimeout(Duration.ofMillis(500));
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		CompletableFuture<Boolean> cf1 = c.build().id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().id(3).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		
		CompletableFuture<User> f1 = c.future().id(1).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f3 = c.future().id(3).get();
		
		assertEquals(3,c.getCollectorSize());
		
		Thread.sleep(110);
		c.command().foreach().timeout();
		Thread.sleep(110);

		assertTrue(f1.isDone());
		assertTrue(f2.isDone());
		assertTrue(f3.isDone());
		
	}

	@Test
	public void testMultiKeyTimeoutFailureDoesNotStopConveyor() throws Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("testMultiKeyTimeoutFailureDoesNotStopConveyor");
		c.setBuilderSupplier(UserBuilder::new);
		c.setDefaultBuilderTimeout(Duration.ofSeconds(5));
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);

		assertTrue(c.build().id(1).create().get());
		assertTrue(c.build().id(2).create().get());
		assertEquals(2, c.getCollectorSize());

		c.addBeforeKeyReschedulingAction((key, expirationTime) -> {
			if (Integer.valueOf(2).equals(key)) {
				throw new RuntimeException("boom");
			}
		});

		CompletableFuture<Boolean> timeoutAll = c.command().foreach().timeout();
		assertThrows(ExecutionException.class, timeoutAll::get);
		assertTrue(c.isRunning(), "Conveyor must stay running after foreach command failure");

		assertTrue(c.build().id(99).create().get());
	}

	@Test
	public void testMultiKeyAddPropertiesCommandCopiesPropertiesToEachKey() throws Exception {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("testMultiKeyAddPropertiesCommandCopiesPropertiesToEachKey");
		c.setBuilderSupplier(UserBuilder::new);
		c.setDefaultBuilderTimeout(Duration.ofSeconds(5));
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);

		assertTrue(c.build().id(1).create().get());
		assertTrue(c.build().id(2).create().get());
		assertTrue(c.command().foreach().addProperties(Map.of("a", "A", "b", "B")).get());

		ProductBin<Integer, User> bin1 = c.command().id(1).peek().get();
		ProductBin<Integer, User> bin2 = c.command().id(2).peek().get();

		assertEquals("A", bin1.properties.get("a"));
		assertEquals("B", bin1.properties.get("b"));
		assertEquals("A", bin2.properties.get("a"));
		assertEquals("B", bin2.properties.get("b"));
	}

    @Test
    public void testCreateCommand() throws InterruptedException, ExecutionException {
        AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
        c.setName("testCreateCommand");
//        c.setBuilderSupplier(UserBuilder::new); // No Builder Supplier
        c.resultConsumer().first(LogResult.stdOut(c)).set();
        assertThrows(Exception.class,()->c.command().id(1).create().join());
    }

    @Test
    public void testCreateCommandWithMetaInfo() throws InterruptedException, ExecutionException {
        AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>(){
            @Override
            public ConveyorMetaInfo<Integer, UserBuilderEvents, User> getMetaInfo() {
                return ConveyorMetaInfoBuilder.of(this)
                        .keyType(Integer.class)
                        .labelType(UserBuilderEvents.class)
                        .productType(User.class)
                        .supportedTypes(UserBuilderEvents.CREATE,Object.class)
                        .supportedTypes(UserBuilderEvents.FAILURE,Object.class)
                        .supportedTypes(UserBuilderEvents.PRINT,Object.class)
                        .supportedTypes(UserBuilderEvents.SET_FIRST,Object.class)
                        .supportedTypes(UserBuilderEvents.SET_LAST,Object.class)
                        .supportedTypes(UserBuilderEvents.SET_YEAR,Object.class)
                        .builderSupplier(UserBuilder::new)
                        .get();
            }
        };
        c.setName("testCreateCommandWithMetaInfo");
        c.resultConsumer().first(LogResult.stdOut(c)).set();
        assertTrue(c.command().id(1).create().join());
    }


}
