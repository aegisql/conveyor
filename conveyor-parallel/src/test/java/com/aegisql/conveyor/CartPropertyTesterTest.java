package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.parallel.PropertyTester;

public class CartPropertyTesterTest {

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
	public void simpleNegativeTest() {
		AssemblingConveyor<String, String, String> c = new AssemblingConveyor<>();
		PropertyTester<String, String, String> cpt = new PropertyTester<>(c);
		Map<String,Object> properties = PropertiesBuilder.putFirst("version", 1).get(); 
		assertFalse(cpt.test(properties));
		assertNotNull(cpt.getConveyor());
	}

	@Test
	public void simpleAcceptingTest() {
		AssemblingConveyor<String, String, String> c = new AssemblingConveyor<>();
		PropertyTester<String, String, String> cpt = new PropertyTester<>(c);
		
		cpt.addTestingPredicate("version",val->val.equals(1L));
		
		Map<String,Object> properties = PropertiesBuilder.putFirst("version", 1L).get(); 
		assertTrue(cpt.test(properties));
	}

	@Test
	public void doubleFailingTestExpectsBothKeys() {
		AssemblingConveyor<String, String, String> c = new AssemblingConveyor<>();
		PropertyTester<String, String, String> cpt = new PropertyTester<>(c);
		
		cpt.addTestingPredicate("version",val->val.equals(1L));
		cpt.addTestingPredicate("abtest",val->"A".equalsIgnoreCase(val.toString()));
		
		Map<String,Object> properties = PropertiesBuilder.putFirst("version", 1L).get(); 
		assertFalse(cpt.test(properties));
	}

	@Test
	public void doubleAcceptingTestExpectsBothKeys() {
		AssemblingConveyor<String, String, String> c = new AssemblingConveyor<>();
		PropertyTester<String, String, String> cpt = new PropertyTester<>(c);
		
		cpt.addTestingPredicate("version",val->val.equals(1L));
		cpt.addTestingPredicate("abtest",val->"A".equalsIgnoreCase(val.toString()));
		
		Map<String,Object> properties = PropertiesBuilder
				.putFirst("version", 1L)
				.put("abtest", "a")
				.get(); 
		assertTrue(cpt.test(properties));
	}
	
}
