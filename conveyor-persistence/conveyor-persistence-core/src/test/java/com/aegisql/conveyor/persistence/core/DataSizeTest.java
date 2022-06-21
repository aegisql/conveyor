package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.persistence.utils.DataSize;

public class DataSizeTest {

	@BeforeClass
	public static void setUpBeforeClass() {
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testLong() {
		assertEquals(1024, DataSize.B.convert(1, DataSize.KB).longValue());
		assertEquals(2, DataSize.KB.convert(2048, DataSize.B).longValue());
		assertEquals(1024,DataSize.KB.toBytes(1).longValue());
		assertEquals(1024,DataSize.toBytes("1KB").longValue());
		assertEquals(10240,DataSize.toBytes("10 kb").longValue());
		assertEquals(10,DataSize.toKBytes("10240").longValue());
	}

	
	@Test
	public void testDouble() {
		assertEquals(1536, DataSize.B.convert(1.5, DataSize.KB).longValue());
		assertEquals(1536, DataSize.KB.toBytes(1.5).longValue());
		assertEquals(1.5, DataSize.KB.convert(1536, DataSize.B).doubleValue(),0.0000001);
		assertEquals(1.5, DataSize.B.toKBytes(1536).doubleValue(),0.0000001);
		assertEquals(1536, DataSize.toBytes(" 1.5 kb ").longValue());
	}

}
