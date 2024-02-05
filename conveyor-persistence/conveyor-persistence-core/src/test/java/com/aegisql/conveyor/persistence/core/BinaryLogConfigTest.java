package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import org.junit.*;

public class BinaryLogConfigTest {

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
	public void test() {
		BinaryLogConfiguration bc = BinaryLogConfiguration.builder().path("/tmp").partTableName("test").build();
		
		String file = bc.getFilePath();
		String fileStamped = bc.getStampedFilePath();
		System.out.println(file);
		System.out.println(fileStamped);
		
		
	}

}
