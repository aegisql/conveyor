package com.aegisql.conveyor.config;

import org.junit.*;

import java.io.IOException;

public class OrderedPropertiesTest {

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
	public void test() throws IOException {
		OrderedProperties op = new OrderedProperties();
		op.load("src/test/resources/test2.properties");
		op.forEach(p->System.out.println(p));
	}

}
