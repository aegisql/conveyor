package com.aegisql.conveyor.loaders;

import org.junit.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

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
					assertEquals(100L, l.priority);
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
		PartLoader pl4in  = pl3.expirationTime(Instant.ofEpochMilli(current+1000))
				.priority(99).increasePriority();
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
	public void testProperties() {
		PartLoader pl0 = new PartLoader<>(
				l->{
					System.out.println("Final: "+l);
					return new CompletableFuture();
				}
				);
		System.out.println(pl0);
		assertEquals(0, pl0.getAllProperties().size());
		PartLoader pl1 = pl0.addProperty("A", 1);
		System.out.println(pl1);
		assertEquals(1, pl1.getAllProperties().size());

		PartLoader pl2 = pl1.addProperty("B", "X");
		System.out.println(pl2);
		assertEquals(0, pl0.getAllProperties().size());
		assertEquals(1, pl1.getAllProperties().size());
		assertEquals(2, pl2.getAllProperties().size());

		PartLoader pl31 = pl2.clearProperties();
		assertEquals(0, pl31.getAllProperties().size());

		PartLoader pl32 = pl2.clearProperty("A");
		assertEquals(1, pl32.getAllProperties().size());
		PartLoader pl33 = pl32.creationTime(1).creationTime(Instant.now()).
				addProperties(new HashMap<String,Object>(){{put("test","val");}});
		Object val = pl33.getProperty("test", String.class);
		assertEquals("val",val);
	}
	
}
