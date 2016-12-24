/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {

	private final ExecutorService pool;
	
	public ThreadPool() {
		pool = Executors.newFixedThreadPool(3);
	}

	public ThreadPool(int n) {
		pool = Executors.newFixedThreadPool(n);
	}

	public void runAsynchWithDelay(long delay, Runnable runnable) {
		pool.submit(()->{
			try {
				Thread.sleep(delay);
			} catch (Exception e) {}
			runnable.run();
			}
		);
	}

	public void runAsynch(Runnable runnable) {
		pool.submit(runnable);
	}

	public void shutdown() {
		pool.shutdown();
	}
	
}
