package com.aegisql.conveyor.persistence.archive;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.persistence.utils.DataSize;

public class FileArchiverTest {

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
	public void binaryLogConfigTest() {

	BinaryLogConfiguration blc = BinaryLogConfiguration.builder()
			.bucketSize(1000)
			.maxFileSize(1, DataSize.KB)
			.partTableName("part")
			.path("./test")
			.zipFile(true)
			.build();
	
	assertNotNull(blc);
	assertTrue(blc.isZipFile());
	assertEquals(1000, blc.getBucketSize());
	assertEquals(1024, blc.getMaxSize());
	assertEquals("./test/", blc.getPath());
	assertEquals("./test/part.blog", blc.getFilePath());
	assertTrue(blc.getStampedFilePath().startsWith("./test/part.20"));
	assertTrue(blc.getStampedFilePath().endsWith(".blog"));
	
	}

	@Test
	public void FileArchiverTest() {
		
	}
	
}
