package com.aegisql.conveyor;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ValueConsumerTest {

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
