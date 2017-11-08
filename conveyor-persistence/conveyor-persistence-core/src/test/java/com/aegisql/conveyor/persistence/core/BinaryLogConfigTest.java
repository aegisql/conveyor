package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;

public class BinaryLogConfigTest {

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
	public void test() {
		BinaryLogConfiguration bc = BinaryLogConfiguration.builder().path("/tmp").partTableName("test").build();
		
		String file = bc.getFilePath();
		String fileStamped = bc.getStampedFilePath();
		System.out.println(file);
		System.out.println(fileStamped);
		
		
	}

}
