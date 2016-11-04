package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExecutorsTest {

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
	public void test() throws InterruptedException {
		ExecutorService es = Executors.newSingleThreadExecutor();
		ArrayList<Future<Integer>> f = new ArrayList<>();
		for(int i = 1; i<100; i++) {
			final int ii = i;
			Future<Integer> fut = es.submit(()->{
				System.out.println("execute "+ii);
				if(ii==50) throw new RuntimeException("i=50");
				try {
					Thread.sleep(1);
				} catch (Exception e) {
					e.printStackTrace();
				}
			},ii);
			f.add(fut);
		}
		System.out.println("===== execute started");
		Thread.sleep(150);
		System.out.println("===== execute complete");
		for(int i = 0; i < f.size(); i++) {
			try {
				Integer n = f.get(i).get();
				System.out.println("done "+n);
			} catch (Exception e) {
				e.getCause().printStackTrace();
			}
		}
		
	}
	
	@Test
	public void testFutureComplete() throws InterruptedException, ExecutionException {
		CompletableFuture<Boolean> cf = new CompletableFuture<>();
		assertFalse(cf.isDone());
		assertFalse(cf.isCancelled());
		assertFalse(cf.isCompletedExceptionally());
		cf.complete(true);
		assertTrue(cf.isDone());
		assertFalse(cf.isCancelled());
		assertFalse(cf.isCompletedExceptionally());
		assertTrue(cf.get());
	}

	@Test(expected=TimeoutException.class)
	public void testFutureTimeout() throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Boolean> cf = new CompletableFuture<>();
		assertFalse(cf.isDone());
		assertFalse(cf.isCancelled());
		assertFalse(cf.isCompletedExceptionally());
		assertTrue(cf.get(10,TimeUnit.MILLISECONDS));
	}

	
	@Test(expected=CancellationException.class)
	public void testFutureCancel() throws InterruptedException, ExecutionException {
		CompletableFuture<Boolean> cf = new CompletableFuture<>();
		assertFalse(cf.isDone());
		assertFalse(cf.isCancelled());
		assertFalse(cf.isCompletedExceptionally());
		cf.cancel(true);
		assertTrue(cf.isDone());
		assertTrue(cf.isCancelled());
		assertTrue(cf.isCompletedExceptionally());
		assertTrue(cf.get());
	}

	@Test(expected=ExecutionException.class)
	public void testFutureFailed() throws InterruptedException, ExecutionException {
		CompletableFuture<Boolean> cf = new CompletableFuture<>();
		assertFalse(cf.isDone());
		assertFalse(cf.isCancelled());
		assertFalse(cf.isCompletedExceptionally());
		cf.completeExceptionally(new RuntimeException());
		assertTrue(cf.isDone());
		assertFalse(cf.isCancelled());
		assertTrue(cf.isCompletedExceptionally());
		assertTrue(cf.get());
	}

	
}
