package com.aegisql.conveyor.config;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.ScrapBin.FailureType;
import com.aegisql.conveyor.config.harness.IntegerSupplier;
import com.aegisql.conveyor.config.harness.StringSupplier;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.result.ResultCounter;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapCounter;

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
		BuilderSupplier bs = (BuilderSupplier) ConfigUtils.stringToBuilderSupplier.apply("new com.aegisql.conveyor.config.harness.StringSupplier('test2')");
		assertNotNull(bs);
		StringSupplier o1 = (StringSupplier) bs.get();
		assertNotNull(o1);
		StringSupplier o2 = (StringSupplier) bs.get();
		assertNotNull(o2);
		assertFalse(o2==o1);
		assertEquals("test2", o1.get());
		assertEquals("test2", o2.get());
	}

	@Test
	public void builderSupplierSupplierTestWithConcurency() {
		BuilderSupplier stringSupplier  = (BuilderSupplier) ConfigUtils.stringToBuilderSupplier.apply("new com.aegisql.conveyor.config.harness.StringSupplier('test2')");
		BuilderSupplier integerSupplier = (BuilderSupplier) ConfigUtils.stringToBuilderSupplier.apply("new com.aegisql.conveyor.config.harness.IntegerSupplier(3)");
		assertNotNull(stringSupplier);
		assertNotNull(integerSupplier);
		StringSupplier s1 = (StringSupplier) stringSupplier.get();
		IntegerSupplier i1 = (IntegerSupplier) integerSupplier.get();
		assertNotNull(s1);
		assertNotNull(i1);
		StringSupplier s2 = (StringSupplier) stringSupplier.get();
		IntegerSupplier i2 = (IntegerSupplier) integerSupplier.get();
		assertNotNull(s2);
		assertNotNull(i2);
		assertFalse(s2==s1);
		assertFalse(i2==i1);
		assertEquals("test2", s1.get());
		assertEquals("test2", s2.get());
		assertEquals(new Integer(3), i1.get());
		assertEquals(new Integer(3), i2.get());
	}

	
	public static ResultCounter rCounter = new ResultCounter<>();
	public static ScrapCounter  sCounter = new ScrapCounter<>();
	
	@Test
	public void resultConsumerSupplierTest() {
		ResultConsumer rc = (ResultConsumer)ConfigUtils.stringToResultConsumerSupplier.apply("new com.aegisql.conveyor.consumers.result.LogResult()");
		assertNotNull(rc);
		rc.accept(new ProductBin(1, "test", 10000, Status.READY, null, null));
	}

	@Test
	public void resultConsumerSupplierTest2() {
		ResultConsumer rc = (ResultConsumer)ConfigUtils.stringToResultConsumerSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.rCounter");
		assertNotNull(rc);
		assertEquals(0, rCounter.get());
		rc.accept(null);
		assertEquals(1, rCounter.get());
	}

	@Test
	public void scrapConsumerSupplierTest() {
		ScrapConsumer rc = (ScrapConsumer)ConfigUtils.stringToScrapConsumerSupplier.apply("new com.aegisql.conveyor.consumers.scrap.LogScrap()");
		assertNotNull(rc);
		rc.accept(new ScrapBin(1, "scrap", "test", null	, FailureType.BUILD_EXPIRED, null, null));
	}

	@Test
	public void scrapConsumerSupplierTest2() {
		ScrapConsumer rc = (ScrapConsumer)ConfigUtils.stringToScrapConsumerSupplier.apply("com.aegisql.conveyor.config.ConfigUtilsTest.sCounter");
		assertNotNull(rc);
		assertEquals(0, sCounter.get());
		rc.accept(null);
		assertEquals(1, sCounter.get());
	}

	
}
