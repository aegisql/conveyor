package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.persistence.converters.BigDecimalToBytesConverter;
import com.aegisql.conveyor.persistence.converters.BigIntegerToBytesConverter;
import com.aegisql.conveyor.persistence.converters.BooleanToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ByteToBytesConverter;
import com.aegisql.conveyor.persistence.converters.CharToBytesConverter;
import com.aegisql.conveyor.persistence.converters.DateToBytesConverter;
import com.aegisql.conveyor.persistence.converters.DoubleToBytesConverter;
import com.aegisql.conveyor.persistence.converters.FloatToBytesConverter;
import com.aegisql.conveyor.persistence.converters.IntegerToBytesConverter;
import com.aegisql.conveyor.persistence.converters.LongToBytesConverter;
import com.aegisql.conveyor.persistence.converters.SerializableToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ShortToBytesConverter;
import com.aegisql.conveyor.persistence.converters.StringToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.IntPrimToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.IntegersToBytesConverter;
import com.aegisql.conveyor.persistence.converters.sql.SqlDateToBytesConverter;
import com.aegisql.conveyor.persistence.converters.sql.SqlTimeToBytesConverter;
import com.aegisql.conveyor.persistence.converters.sql.SqlTimestampToBytesConverter;

public class ConvertersTest {

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
	public void testByte() {
		ByteToBytesConverter ic = new ByteToBytesConverter();
		byte[] b = ic.toPersistence((byte) 100);
		assertNotNull(b);
		assertEquals(1, b.length);
		byte x = ic.fromPersistence(b);
		assertEquals(100, x);
	}

	@Test
	public void testChar() {
		CharToBytesConverter ic = new CharToBytesConverter();
		byte[] b = ic.toPersistence('A');
		assertNotNull(b);
		assertEquals(2, b.length);
		char x = ic.fromPersistence(b);
		assertEquals('A', x);
	}

	@Test
	public void testShort() {
		ShortToBytesConverter ic = new ShortToBytesConverter();
		byte[] b = ic.toPersistence((short) 5);
		assertNotNull(b);
		assertEquals(2, b.length);
		short x = ic.fromPersistence(b);
		assertEquals(5, x);
	}

	@Test
	public void testInteger() {
		IntegerToBytesConverter ic = new IntegerToBytesConverter();
		byte[] b = ic.toPersistence(100);
		assertNotNull(b);
		assertEquals(4, b.length);
		int x = ic.fromPersistence(b);
		assertEquals(100, x);
	}

	@Test
	public void testFloat() {
		FloatToBytesConverter ic = new FloatToBytesConverter();
		byte[] b = ic.toPersistence(3.14f);
		assertNotNull(b);
		assertEquals(4, b.length);
		float x = ic.fromPersistence(b);
		assertEquals(3.14, x, 0.00001);
	}

	@Test
	public void testLong() {
		LongToBytesConverter ic = new LongToBytesConverter();
		byte[] b = ic.toPersistence(100L);
		assertNotNull(b);
		assertEquals(8, b.length);
		long x = ic.fromPersistence(b);
		assertEquals(100, x);
	}

	@Test
	public void testDate() {
		DateToBytesConverter ic = new DateToBytesConverter();
		Date now = new Date();
		byte[] b = ic.toPersistence(now);
		assertNotNull(b);
		assertEquals(8, b.length);
		Date x = ic.fromPersistence(b);
		assertEquals(now, x);
	}

	@Test
	public void testSqlDate() {
		SqlDateToBytesConverter ic = new SqlDateToBytesConverter();
		java.sql.Date now = java.sql.Date.valueOf("2017-09-01");
		byte[] b = ic.toPersistence(now);
		assertNotNull(b);
		assertEquals(8, b.length);
		java.sql.Date x = ic.fromPersistence(b);
		assertEquals(now, x);
	}

	@Test
	public void testSqlTime() {
		SqlTimeToBytesConverter ic = new SqlTimeToBytesConverter();
		java.sql.Time now = java.sql.Time.valueOf("12:00:00");
		byte[] b = ic.toPersistence(now);
		assertNotNull(b);
		assertEquals(8, b.length);
		java.sql.Time x = ic.fromPersistence(b);
		assertEquals(now, x);
	}

	@Test
	public void testSqlTimestamp() {
		SqlTimestampToBytesConverter ic = new SqlTimestampToBytesConverter();
		java.sql.Timestamp now = java.sql.Timestamp.valueOf("2017-01-01 12:00:00");
		byte[] b = ic.toPersistence(now);
		assertNotNull(b);
		assertEquals(8, b.length);
		java.sql.Timestamp x = ic.fromPersistence(b);
		assertEquals(now, x);
	}

	@Test
	public void testDouble() {
		DoubleToBytesConverter ic = new DoubleToBytesConverter();
		byte[] b = ic.toPersistence(3.14);
		assertNotNull(b);
		assertEquals(8, b.length);
		double x = ic.fromPersistence(b);
		assertEquals(3.14, x, 0.00001);
	}

	@Test
	public void testString() {
		StringToBytesConverter ic = new StringToBytesConverter();
		byte[] b = ic.toPersistence("test");
		assertNotNull(b);
		assertEquals("test".getBytes().length, b.length);
		String x = ic.fromPersistence(b);
		assertEquals("test", x);
	}

	@Test
	public void testSerializedString() {
		SerializableToBytesConverter<String> ic = new SerializableToBytesConverter<>();
		byte[] b = ic.toPersistence("test");
		System.out.println(new String(b));
		assertNotNull(b);
		String x = ic.fromPersistence(b);
		assertEquals("test", x);
	}

	@Test
	public void testSerializedLong() {
		SerializableToBytesConverter<Long> ic = new SerializableToBytesConverter<>();
		byte[] b = ic.toPersistence(100L);
		System.out.println(new String(b));
		assertNotNull(b);
		long x = ic.fromPersistence(b);
		assertEquals(100L, x);
	}

	@Test
	public void testBigInt() {
		BigIntegerToBytesConverter ic = new BigIntegerToBytesConverter();
		byte[] b = ic.toPersistence(new BigInteger("12345678900987654321"));
		assertNotNull(b);
		assertEquals(9, b.length);
		BigInteger x = ic.fromPersistence(b);
		assertEquals(new BigInteger("12345678900987654321"), x);
	}

	@Test
	public void testBigDec() {
		BigDecimalToBytesConverter ic = new BigDecimalToBytesConverter();
		byte[] b = ic.toPersistence(new BigDecimal("12345678900987654321"));
		assertNotNull(b);
		assertEquals(9, b.length);
		BigDecimal x = ic.fromPersistence(b);
		assertEquals(new BigDecimal("12345678900987654321"), x);
	}

	@Test
	public void testBoolean1() {
		BooleanToBytesConverter ic = new BooleanToBytesConverter();
		byte[] b = ic.toPersistence(true);
		assertNotNull(b);
		assertEquals(1, b.length);
		boolean x = ic.fromPersistence(b);
		assertTrue(x);
	}

	@Test
	public void testBoolean2() {
		BooleanToBytesConverter ic = new BooleanToBytesConverter();
		byte[] b = ic.toPersistence(false);
		assertNotNull(b);
		assertEquals(1, b.length);
		boolean x = ic.fromPersistence(b);
		assertFalse(x);
	}

	@Test
	public void testIntegerArray() {
		Integer[] ia = new Integer[]{1,2,3};
		System.out.println(ia.getClass().getName());
		
		IntegersToBytesConverter bc = new IntegersToBytesConverter();
		byte[] b = bc.toPersistence(ia);
		assertNotNull(b);
		assertEquals(12, b.length);
		Integer[] ia2 = bc.fromPersistence(b);
		assertNotNull(ia2);
		assertEquals(3, ia2.length);
		assertEquals(ia[0], ia2[0]);
		assertEquals(ia[1], ia2[1]);
		assertEquals(ia[2], ia2[2]);

	}

	
	@Test
	public void testIntArray() {
		int[] ia = new int[]{1,2,3};
		System.out.println(ia.getClass().getName());
		
		IntPrimToBytesConverter bc = new IntPrimToBytesConverter();
		byte[] b = bc.toPersistence(ia);
		assertNotNull(b);
		assertEquals(12, b.length);
		int[] ia2 = bc.fromPersistence(b);
		assertNotNull(ia2);
		assertEquals(3, ia2.length);
		assertEquals(ia[0], ia2[0]);
		assertEquals(ia[1], ia2[1]);
		assertEquals(ia[2], ia2[2]);

	}
	
}
