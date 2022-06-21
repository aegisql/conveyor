package com.aegisql.conveyor.config;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
