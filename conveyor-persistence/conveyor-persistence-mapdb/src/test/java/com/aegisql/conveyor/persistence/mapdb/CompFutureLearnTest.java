package com.aegisql.conveyor.persistence.mapdb;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CompFutureLearnTest {

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
	public void testSuccess() throws InterruptedException, ExecutionException {
		CompletableFuture<Integer> f1 = new CompletableFuture<Integer>();
		
		
		CompletableFuture<String> f2 = f1.handle((x,e)->{
			String res = "X="+x;
			System.out.println(res);
			return res;
		});
		
		f1.complete(10);
		
		assertEquals(Integer.valueOf(10), f1.get());
		assertEquals("X=10", f2.get());
	
	}

	
	@Test
	public void testError() throws InterruptedException, ExecutionException {
		CompletableFuture<Integer> f1 = new CompletableFuture<Integer>();
		
		
		CompletableFuture<String> f2 = f1.handle((x,e)->{
			String res = "X="+x;
			System.out.println(res + " "+e);
			return res;
		});
		
		f1.completeExceptionally(new RuntimeException());
		
		assertEquals("X=null", f2.get());
	
	}

	@Test(expected=ExecutionException.class)
	public void testErrorReThrow() throws InterruptedException, ExecutionException {
		CompletableFuture<Integer> f1 = new CompletableFuture<Integer>();
		
		
		CompletableFuture<String> f2 = f1.handle((x,e)->{
			if(e != null)
				throw new RuntimeException(e);
			else 
				return "X="+x;
		});
		
		f1.completeExceptionally(new RuntimeException());
		
		assertEquals("X=null", f2.get());
	
	}
	
}
