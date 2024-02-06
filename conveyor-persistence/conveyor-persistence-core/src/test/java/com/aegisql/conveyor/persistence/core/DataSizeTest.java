package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.persistence.utils.DataSize;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataSizeTest {

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
