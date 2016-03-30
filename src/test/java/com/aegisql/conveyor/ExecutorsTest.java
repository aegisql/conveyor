package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

}
