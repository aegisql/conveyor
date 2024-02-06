package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import org.junit.jupiter.api.*;

public class BinaryLogConfigTest {

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
	public void test() {
		BinaryLogConfiguration bc = BinaryLogConfiguration.builder().path("/tmp").partTableName("test").build();
		
		String file = bc.getFilePath();
		String fileStamped = bc.getStampedFilePath();
		System.out.println(file);
		System.out.println(fileStamped);
		
		
	}

}
