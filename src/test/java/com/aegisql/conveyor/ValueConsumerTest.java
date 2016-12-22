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
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
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
	public void testComposeAndThen() {
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

	
	@Test
	public void testWhenBiCon() {
		LabeledValueConsumer<String, String, StringBuilder> lvc = (l,v,b) -> {
			b.append(v);
		};

		lvc = lvc.when("a", (b,v)->{
			b.append(v.toUpperCase());
		});
		lvc = lvc.when("b", (b,v)->{
			b.append(v.toUpperCase());
		});

		StringBuilder sb = new StringBuilder();
		
		lvc.accept("a", "x", sb);
		lvc.accept("b", "y", sb);
		lvc.accept("c", "z", sb);
		
		System.out.println(sb);
		
		assertEquals("XYz", sb.toString());
	}

	@Test
	public void testWhenCon() {
		LabeledValueConsumer<String, String, StringBuilder> lvc = (l,v,b) -> {
			b.append(v);
		};

		StringBuilder sb = new StringBuilder();

		lvc = lvc.when("a", (v)->{
			sb.append(v.toUpperCase());
		});
		lvc = lvc.when("b", (v)->{
			sb.append(v.toUpperCase());
		});

		
		lvc.accept("a", "x", sb);
		lvc.accept("b", "y", sb);
		lvc.accept("c", "z", sb);
		
		System.out.println(sb);
		
		assertEquals("XYz", sb.toString());
	}

	@Test
	public void testWhenRunIgnore() {
		LabeledValueConsumer<String, String, StringBuilder> lvc = (l,v,b) -> {
			b.append(v);
		};

		StringBuilder sb = new StringBuilder();

		lvc = lvc.when("a", ()->{
			sb.append("A");
		});
		lvc = lvc.when("b", ()->{
			sb.append("B");
		});

		lvc = lvc.ignore("c");
		
		lvc.accept("a", "x", sb);
		lvc.accept("b", "y", sb);
		lvc.accept("c", "z", sb);
		
		System.out.println(sb);
		
		assertEquals("AB", sb.toString());
	}


}
