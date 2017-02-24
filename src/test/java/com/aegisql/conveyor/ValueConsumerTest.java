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

	
	/**
	 * Test when bi con.
	 */
	@Test
	public void testWhenBiCon() {
		LabeledValueConsumer<String, String, StringBuilder> lvc = (l,v,b) -> {
			b.append(v);
		};

		lvc = lvc.<String>when("a", (b,v)->{
			b.append(v.toUpperCase());
		});
		lvc = lvc.<String>when("b", (b,v)->{
			b.append(v.toUpperCase());
		});

		StringBuilder sb = new StringBuilder();
		
		lvc.accept("a", "x", sb);
		lvc.accept("b", "y", sb);
		lvc.accept("c", "z", sb);
		
		System.out.println(sb);
		
		assertEquals("XYz", sb.toString());
	}

	/**
	 * Test when con.
	 */
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

	/**
	 * Test when run ignore.
	 */
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


	/**
	 * Test accept bi con.
	 */
	@Test
	public void testAcceptBiCon() {
		LabeledValueConsumer<Integer, String, StringBuilder> lvc = (l,v,b) -> {
			b.append(v.toUpperCase());
		};

		StringBuilder sb = new StringBuilder();

		lvc = lvc.<String>filter(l -> l < 5 , (b,v)->{
			b.append(v.toLowerCase());			
		});

		lvc = lvc.ignore(l->l>10);
		
		System.out.println(sb);
		
		lvc.accept(1, "A", sb);
		lvc.accept(2, "B", sb);
		lvc.accept(3, "C", sb);
		lvc.accept(4, "D", sb);
		lvc.accept(5, "e", sb);
		lvc.accept(6, "f", sb);
		lvc.accept(7, "g", sb);

		lvc.accept(11, "x", sb);
		lvc.accept(12, "y", sb);

		
		assertEquals("abcdEFG", sb.toString());
	}

	@Test
	public void testMatch1() {
		LabeledValueConsumer<String, String, StringBuilder> lvc = (l,v,b) -> {
			System.out.println("testing "+b+":"+v);
		};

		StringBuilder sb = new StringBuilder();

		lvc = lvc.<String>match("a+", (b,v)->{
			System.out.println("appending "+b+":"+v);
			b.append(v.toLowerCase());			
		});
		lvc.accept("a", "a", sb);
		lvc.accept("aa", "AA", sb);
		lvc.accept("aba", "aba", sb);
		lvc.accept("b", "b", sb);
		
		System.out.println(sb);
		assertEquals("aaa", sb.toString());
	}

	@Test
	public void testMatch2() {
		LabeledValueConsumer<String, String, StringBuilder> lvc = (l,v,b) -> {
			System.out.println("testing "+b+":"+v);
		};

		StringBuilder sb = new StringBuilder();

		lvc = lvc.<String>match("a+", v->{
			System.out.println("appending:"+v);
			sb.append(v.toLowerCase());			
		});
		lvc.accept("a", "a", sb);
		lvc.accept("aa", "AA", sb);
		lvc.accept("aba", "aba", sb);
		lvc.accept("b", "b", sb);
		
		System.out.println(sb);
		assertEquals("aaa", sb.toString());
	}

	@Test
	public void testMatch3() {
		LabeledValueConsumer<String, String, StringBuilder> lvc = (l,v,b) -> {
			System.out.println("testing "+b+":"+v);
		};

		StringBuilder sb = new StringBuilder();

		lvc = lvc.<String>match("a+", ()->{
			System.out.println("appending: a");
			sb.append("a");			
		});
		lvc.accept("a", "a", sb);
		lvc.accept("aa", "AA", sb);
		lvc.accept("aba", "aba", sb);
		lvc.accept("b", "b", sb);
		
		System.out.println(sb);
		assertEquals("aa", sb.toString());
	}

	@Test
	public void testMatch4() {
		LabeledValueConsumer<String, String, StringBuilder> lvc = (l,v,b) -> {
			System.out.println("testing "+b+":"+v);
		};

		StringBuilder sb = new StringBuilder();

		lvc = lvc.<String>match("a+", v->{
			System.out.println("appending:"+v);
			sb.append(v.toLowerCase());			
		}).<String>match("z+", v->{
			System.out.println("appending:"+v);
			sb.append(v.toLowerCase());			
		});
		lvc.accept("a", "a", sb);
		lvc.accept("aa", "AA", sb);
		lvc.accept("aba", "aba", sb);
		lvc.accept("b", "b", sb);
		lvc.accept("z", "z", sb);
		
		System.out.println(sb);
		assertEquals("aaaz", sb.toString());
	}

}
