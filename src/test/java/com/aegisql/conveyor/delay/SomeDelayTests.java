package com.aegisql.conveyor.delay;

import static org.junit.Assert.*;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SomeDelayTests {
	
	
	public static class D implements Delayed {

		String d;
		int c = -1;
		long e = 1;
		
		public D(String d) {
			this.d = d;
		}

		public D(String d,int c, long e) {
			this.d = d;
			this.c = c;
			this.e = e;
		}

		@Override
		public int compareTo(Delayed o) {
			System.out.println("compareTo "+d);
			return c;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			System.out.println("getDelay "+d);
			return e;
		}

		@Override
		public String toString() {
			return "D [" + d + "]";
		}
		
	}

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
		
		
		D d1 = new D("1",1,1);
		D d2 = new D("2",1,1);
		D d3 = new D("3",1,1);
		D d4 = new D("4",1,1);
		D d5 = new D("5",1,1);
		
		long t = 0;
		DelayQueue<D> dq = new DelayQueue<>(); 
		
		dq.add(d1);
		dq.add(d2);
		dq.add(d3);
		
		D e = dq.poll();
		System.out.println(e+" "+dq.size() );

		dq.add(d4);
		dq.add(d5);
		TimeUnit.MILLISECONDS.sleep(110);
		e = dq.poll();
		System.out.println(e+" "+dq.size());
		e = dq.poll();
		System.out.println(e+" "+dq.size());
		e = dq.poll();
		System.out.println(e+" "+dq.size());
		
		d1.e = 0;
		e = dq.poll();
		System.out.println(e+" "+dq.size());

		d3.e = 0;
		e = dq.poll();
		System.out.println(e+" "+dq.size());

		d2.e = 0;
		e = dq.poll();
		System.out.println(e+" "+dq.size());

		d4.e = 0;
		e = dq.poll();
		System.out.println(e+" "+dq.size());

		d5.e = 0;
		e = dq.poll();
		System.out.println(e+" "+dq.size());
		e = dq.poll();
		System.out.println(e+" "+dq.size());
		e = dq.poll();
		System.out.println(e+" "+dq.size());
		e = dq.poll();
		System.out.println(e+" "+dq.size());
		e = dq.poll();
		System.out.println(e+" "+dq.size());

	}

}
