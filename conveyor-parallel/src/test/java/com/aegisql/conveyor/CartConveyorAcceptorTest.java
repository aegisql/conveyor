package com.aegisql.conveyor;

import com.aegisql.conveyor.parallel.ConveyorAcceptor;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CartConveyorAcceptorTest {

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
	public void simpleNegativeTest() {
		AssemblingConveyor<String, String, String> c = new AssemblingConveyor<>();
		ConveyorAcceptor<String, String, String> cpt = new ConveyorAcceptor<>(c);
		Map<String,Object> properties = PropertiesBuilder.putFirst("version", 1).get(); 
		assertFalse(cpt.test(properties));
		assertNotNull(cpt.getConveyor());
	}

	@Test
	public void simpleAcceptingTest() {
		AssemblingConveyor<String, String, String> c = new AssemblingConveyor<>();
		ConveyorAcceptor<String, String, String> cpt = new ConveyorAcceptor<>(c);
		
		cpt.addTestingPredicate("version",val->val.equals(1L));
		
		Map<String,Object> properties = PropertiesBuilder.putFirst("version", 1L).get(); 
		assertTrue(cpt.test(properties));
	}

	@Test
	public void doubleFailingTestExpectsBothKeys() {
		AssemblingConveyor<String, String, String> c = new AssemblingConveyor<>();
		ConveyorAcceptor<String, String, String> cpt = new ConveyorAcceptor<>(c);
		
		cpt.addTestingPredicate("version",val->val.equals(1L));
		cpt.addTestingPredicate("abtest",val->"A".equalsIgnoreCase(val.toString()));
		
		Map<String,Object> properties = PropertiesBuilder.putFirst("version", 1L).get(); 
		assertFalse(cpt.test(properties));
	}

	@Test
	public void doubleAcceptingTestExpectsBothKeys() {
		AssemblingConveyor<String, String, String> c = new AssemblingConveyor<>();
		ConveyorAcceptor<String, String, String> cpt = new ConveyorAcceptor<>(c);
		
		cpt.addTestingPredicate("version",val->val.equals(1L));
		cpt.addTestingPredicate("abtest",val->"A".equalsIgnoreCase(val.toString()));
		
		Map<String,Object> properties = PropertiesBuilder
				.putFirst("version", 1L)
				.put("abtest", "a")
				.get(); 
		assertTrue(cpt.test(properties));
	}
	
}
