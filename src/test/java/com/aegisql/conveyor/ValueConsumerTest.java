/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

// TODO: Auto-generated Javadoc
/**
 * The Class ValueConsumerTest.
 */
public class ValueConsumerTest {

	/**
	 * Sets the up before class.
	 *
	 * @throws Exception the exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * Tear down after class.
	 *
	 * @throws Exception the exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Sets the up.
	 *
	 * @throws Exception the exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Tear down.
	 *
	 * @throws Exception the exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test.
	 */
	@Test
	public void test() {
		LabeledValueConsumer<String, String, StringBuilder> lvc = (l,v,b) -> {
			b.append(l).append("-").append(v);
		};
		
		lvc = lvc.andThen((l,v,b) -> {
			b.append(" END");
			
		});
		
		lvc = lvc.compose((l,v,b) -> {
			b.append("START ");
		});
		
		StringBuilder sb = new StringBuilder();
		
		lvc.accept("*", "x", sb);
		
		System.out.println(sb);
		
		assertEquals("START *-x END", sb.toString());
	}

}
