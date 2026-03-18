/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadPool {

	private final ExecutorService pool;
	
	public ThreadPool() {
		pool = Executors.newFixedThreadPool(3);
	}

	public ThreadPool(int n) {
		pool = Executors.newFixedThreadPool(n);
	}

	public Future<?> runAsynchWithDelay(long delay, Runnable runnable) {
		return pool.submit(()->{
			try {
				Thread.sleep(delay);
			} catch (Exception e) {}
			runnable.run();
			}
		);
	}

	public Future<?> runAsynchWithRandomDelay(int bound, Runnable runnable) {
		Random r = new Random();
		return runAsynchWithDelay(r.nextInt(bound)+1,runnable);
	}

	public Future<?> runAsynchWithRandomDelay(Runnable runnable) {
		return runAsynchWithRandomDelay(100,runnable);
	}

	public Future<?> runAsynch(Runnable runnable) {
		return pool.submit(runnable);
	}

	public void shutdown() {
		pool.shutdown();
	}
	
}
