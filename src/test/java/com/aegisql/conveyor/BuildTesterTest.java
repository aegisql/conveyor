package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BuildTesterTest {

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
	public void testTrivialCase() {
		BuildTester<String, String, String> bt = new BuildTester<>();
		assertTrue(bt.test(null,null));
	}

	@Test
	public void testAcceptedTimes() {
		BuildTester<String, String, String> bt1 = new BuildTester<String, String, String>().accepted(2);
		BuildTester<String, String, String> bt2 = new BuildTester<String, String, String>().accepted(3);
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, null, null);
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
	}

	@Test
	public void testAcceptedLabel() {
		BuildTester<String, String, String> bt1 = new BuildTester<String, String, String>().accepted("A");
		BuildTester<String, String, String> bt2 = new BuildTester<String, String, String>().accepted("B");
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, new HashMap<String,Integer>(){{
			put("A",1);
			put("C",2);
		}}, null);
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
	}

	@Test
	public void testAcceptedLabelTimes() {
		BuildTester<String, String, String> bt1 = new BuildTester<String, String, String>().accepted("A",1).accepted("C") ;
		BuildTester<String, String, String> bt2 = new BuildTester<String, String, String>().accepted("C",1);
		State<String, String> st = new State<String, String>("1", 0, 0, 0, 0, 2, new HashMap<String,Integer>(){{
			put("A",1);
			put("C",2);
		}}, null);
		assertTrue(bt1.test(st,null));
		assertFalse(bt2.test(st,null));
	}

}
