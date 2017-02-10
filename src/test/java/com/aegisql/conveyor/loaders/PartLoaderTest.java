package com.aegisql.conveyor.loaders;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.multichannel.UserBuilder;
import com.aegisql.conveyor.multichannel.UserBuilderEvents;
import com.aegisql.conveyor.user.User;

public class PartLoaderTest {

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
	public void testSingleKey() {
		long current = System.currentTimeMillis();
		PartLoader pl0 = new PartLoader<>(
				l->{
					System.out.println("Final: "+l);
					assertNotNull(l);
					assertEquals(1, l.key);
					assertEquals("test", l.label);
					assertEquals("value", l.partValue);
					assertTrue(l.expirationTime > 0);
					assertTrue(l.creationTime > 0);
					assertTrue(l.expirationTime > l.creationTime);
					return new CompletableFuture();
				}
				);
		System.out.println(pl0);
		assertTrue(pl0.creationTime >= current);
		
		current = pl0.creationTime;
		
		PartLoader pl1 = pl0.id(1);
		System.out.println(pl1);
		assertEquals(pl0.creationTime, pl1.creationTime);
		assertEquals(1, pl1.key);

		PartLoader pl2 = pl1.label("test");
		System.out.println(pl2);
		assertEquals(pl1.creationTime, pl2.creationTime);
		assertEquals(1, pl2.key);
		assertEquals("test", pl2.label);

		PartLoader pl3 = pl2.value("value");
		System.out.println(pl3);
		assertEquals(pl3.creationTime, pl2.creationTime);
		assertEquals(1, pl3.key);
		assertEquals("test", pl3.label);
		assertEquals("value", pl3.partValue);

		PartLoader pl4et  = pl3.expirationTime(current+1000);
		PartLoader pl4in  = pl3.expirationTime(Instant.ofEpochMilli(current+1000));
		PartLoader pl4ttl = pl3.ttl(1000, TimeUnit.MILLISECONDS);
		PartLoader pl4dur = pl3.ttl(Duration.ofMillis(1000));
		System.out.println(pl4et);
		System.out.println(pl4in);
		System.out.println(pl4ttl);
		System.out.println(pl4dur);
		assertEquals(pl4et.creationTime,pl4in.creationTime);
		assertEquals(pl4et.creationTime,pl4ttl.creationTime);
		assertEquals(pl4et.creationTime,pl4dur.creationTime);

		assertEquals(0,pl4in.ttlMsec);
		assertEquals(0,pl4et.ttlMsec);
		assertEquals(1000,pl4ttl.ttlMsec);
		assertEquals(1000,pl4dur.ttlMsec);

		CompletableFuture f = pl4in.place();
		assertNotNull(f);
	}

	@Test
	public void testMultiKey() {
		long current = System.currentTimeMillis();
		MultiKeyPartLoader pl0 = new MultiKeyPartLoader<>(
				l->{
					System.out.println("Final: "+l);
					assertNotNull(l);
					assertEquals("test", l.label);
					assertEquals("value", l.partValue);
					assertTrue(l.expirationTime > 0);
					assertTrue(l.creationTime > 0);
					assertTrue(l.expirationTime > l.creationTime);
					assertTrue(l.filter.test(123));
					return new CompletableFuture();
				}
				);
		System.out.println(pl0);
		assertTrue(pl0.creationTime >= current);
		
		current = pl0.creationTime;

		MultiKeyPartLoader pl1 = pl0;
		System.out.println(pl1);
		assertEquals(pl0.creationTime, pl1.creationTime);

		MultiKeyPartLoader pl2 = pl1.label("test");
		System.out.println(pl2);
		assertEquals(pl1.creationTime, pl2.creationTime);
		assertEquals("test", pl2.label);

		MultiKeyPartLoader pl3 = pl2.value("value");
		System.out.println(pl3);
		assertEquals(pl3.creationTime, pl2.creationTime);
		assertEquals("test", pl3.label);
		assertEquals("value", pl3.partValue);

		MultiKeyPartLoader pl4et  = pl3.expirationTime(current+1000);
		MultiKeyPartLoader pl4in  = pl3.expirationTime(Instant.ofEpochMilli(current+1000));
		MultiKeyPartLoader pl4ttl = pl3.ttl(1000, TimeUnit.MILLISECONDS);
		MultiKeyPartLoader pl4dur = pl3.ttl(Duration.ofMillis(1000));
		System.out.println(pl4et);
		System.out.println(pl4in);
		System.out.println(pl4ttl);
		System.out.println(pl4dur);
		assertEquals(pl4et.creationTime,pl4in.creationTime);
		assertEquals(pl4et.creationTime,pl4ttl.creationTime);
		assertEquals(pl4et.creationTime,pl4dur.creationTime);

		assertEquals(0,pl4in.ttlMsec);
		assertEquals(0,pl4et.ttlMsec);
		assertEquals(1000,pl4ttl.ttlMsec);
		assertEquals(1000,pl4dur.ttlMsec);

		MultiKeyPartLoader pl4each = pl4in.foreach();
		MultiKeyPartLoader pl4123 = pl4in.foreach(x->{return (Integer)x==123;});
		
		CompletableFuture f = pl4each.place();
		assertNotNull(f);
		CompletableFuture f2 = pl4123.place();
		assertNotNull(f2);

		
		
	}
	
	@Test
	public void multyKeyParts() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("multyKeyParts");
		c.setBuilderSupplier(UserBuilder::new);
		c.setResultConsumer(bin->{
			System.out.println(bin);
		});
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		CompletableFuture<Boolean> cf1 = c.build().id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().id(3).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		
		CompletableFuture<User> f1 = c.future().id(1).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f3 = c.future().id(3).get();

		c.multiKeyPart().foreach().label(UserBuilderEvents.SET_FIRST).value("A").place();
		c.multiKeyPart().foreach().label(UserBuilderEvents.SET_LAST).value("B").place();
		c.multiKeyPart().foreach().label(UserBuilderEvents.SET_YEAR).value(2000).place();
		
		User u1 = f1.get();
		User u2 = f2.get();
		User u3 = f3.get();
	}

	@Test
	public void multyKeyPartsWithFilter() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
		c.setName("multyKeyParts");
		c.setBuilderSupplier(UserBuilder::new);
		c.setResultConsumer(bin->{
			System.out.println(bin);
		});
		c.setDefaultBuilderTimeout(500, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(UserBuilderEvents.SET_FIRST,UserBuilderEvents.SET_LAST,UserBuilderEvents.SET_YEAR));
		CompletableFuture<Boolean> cf1 = c.build().id(1).create();
		CompletableFuture<Boolean> cf2 = c.build().id(2).create();
		CompletableFuture<Boolean> cf3 = c.build().id(3).create();
		assertTrue(cf1.get());
		assertTrue(cf2.get());
		assertTrue(cf3.get());
		
		CompletableFuture<User> f1 = c.future().id(1).get();
		CompletableFuture<User> f2 = c.future().id(2).get();
		CompletableFuture<User> f3 = c.future().id(3).get();

		c.multiKeyPart().foreach(k->k>1).label(UserBuilderEvents.SET_FIRST).value("A").place();
		c.multiKeyPart().foreach(k->k>1).label(UserBuilderEvents.SET_LAST).value("B").place();
		c.multiKeyPart().foreach(k->k>1).label(UserBuilderEvents.SET_YEAR).value(2000).place();
		
		User u2 = f2.get();
		User u3 = f3.get();
		try {
			User u1 = f1.get();
			fail("Unexpected");
		} catch(Exception e) {
			
		}

		c.stop();
	}

}
