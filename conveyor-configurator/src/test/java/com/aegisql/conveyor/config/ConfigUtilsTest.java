package com.aegisql.conveyor.config;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Status;

public class ConfigUtilsTest {

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
	public void testTimeToMillsConverter() {

		Long time = (Long) ConfigUtils.timeToMillsConverter.apply("1");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply("1000000 NANOSECONDS //comment");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply("1000 MICROSECONDS");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);
		
		time = (Long) ConfigUtils.timeToMillsConverter.apply("1 MILLISECONDS");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply("1 SECONDS");
		assertNotNull(time);
		assertEquals(Long.valueOf(1000), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply(" 1 MINUTES ");
		assertNotNull(time);
		assertEquals(Long.valueOf(60*1000), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply(" 1 HOURS ");
		assertNotNull(time);
		assertEquals(Long.valueOf(60*60*1000), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply(" 1 DAYS ");
		assertNotNull(time);
		assertEquals(Long.valueOf(24*60*60*1000), time);

		// FRACTION
		
		time = (Long) ConfigUtils.timeToMillsConverter.apply("1");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply("1000000.1 NANOSECONDS //comment");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply("1000.9 MICROSECONDS");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);
		
		time = (Long) ConfigUtils.timeToMillsConverter.apply("1.1 MILLISECONDS");
		assertNotNull(time);
		assertEquals(Long.valueOf(1), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply("1.5 SECONDS");
		assertNotNull(time);
		assertEquals(Long.valueOf(1500), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply(" 1.5 MINUTES ");
		assertNotNull(time);
		assertEquals(Long.valueOf(90*1000), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply(" 1.5 HOURS ");
		assertNotNull(time);
		assertEquals(Long.valueOf(90*60*1000), time);

		time = (Long) ConfigUtils.timeToMillsConverter.apply(" 1.5 DAYS ");
		assertNotNull(time);
		assertEquals(Long.valueOf(36*60*60*1000), time);

		
		
	}
	
	@Test
	public void statusConverterTest() {
		Status[] s = (Status[]) ConfigUtils.stringToStatusConverter.apply("READY");
		assertNotNull(s);
		assertEquals(1, s.length);
		assertEquals(Status.READY, s[0]);

		s = (Status[]) ConfigUtils.stringToStatusConverter.apply("READY,TIMED_OUT");
		assertNotNull(s);
		assertEquals(2, s.length);
		assertEquals(Status.READY, s[0]);
		assertEquals(Status.TIMED_OUT, s[1]);

	}

	@Test
	public void builderSupplierSupplierTest() {
		BuilderSupplier bs = (BuilderSupplier) ConfigUtils.stringToBuilderSupplier.apply("new com.aegisql.conveyor.config.StringSupplier()");
		assertNotNull(bs);
		StringSupplier o1 = (StringSupplier) bs.get();
		assertNotNull(o1);
		StringSupplier o2 = (StringSupplier) bs.get();
		assertNotNull(o2);
		assertFalse(o2==o1);
	}
	
}
