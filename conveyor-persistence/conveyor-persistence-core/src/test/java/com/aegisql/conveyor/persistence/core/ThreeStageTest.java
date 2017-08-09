package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.consumers.result.ResultConsumer;

public class ThreeStageTest {

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

		AtomicInteger bc = new AtomicInteger(0);
		AtomicInteger mc = new AtomicInteger(0);
		AtomicInteger ac = new AtomicInteger(0);
		
		ResultConsumer<String,String> rc = new ThreeStageResultConsumer<String,String>(
				bin->{
					bc.incrementAndGet();
					System.out.println("BEFORE "+bc.get());
					},
				bin->{
					mc.incrementAndGet();
					System.out.println("MAIN "+mc.get());
					},
				bin->{
					ac.incrementAndGet();
					System.out.println("AFTER "+ac.get());
					}
		);
		
		rc.accept(null);
		rc=rc.andThen(bin->{
			mc.incrementAndGet();
			System.out.println("MAIN "+mc.get());
		});
		rc.accept(null);
		rc=rc.filter(bin->{
			mc.incrementAndGet();
			System.out.println("MAIN "+mc.get());
			return bin==null;
		});
		rc.accept(null);
		assertEquals(3,bc.get());
		assertEquals(6,mc.get());
		assertEquals(3,ac.get());
	}

}
