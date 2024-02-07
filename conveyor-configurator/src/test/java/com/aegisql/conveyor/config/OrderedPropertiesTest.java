package com.aegisql.conveyor.config;

import org.junit.jupiter.api.*;

import java.io.IOException;

public class OrderedPropertiesTest {

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
	public void test() throws IOException {
		OrderedProperties op = new OrderedProperties();
		op.load("src/test/resources/test2.properties");
		op.forEach(p->System.out.println(p));
	}

}
