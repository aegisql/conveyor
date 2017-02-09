package com.aegisql.conveyor.loaders;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BuilderLoaderTest {

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
		
		long current = System.currentTimeMillis();

		BuilderLoader bl0 = new BuilderLoader<>(p->{
			return new CompletableFuture();
		}, fp->{
			return new CompletableFuture();

		});
		System.out.println(bl0);
		assertTrue(bl0.creationTime >= current);
		
		current = bl0.creationTime;
	}

}
