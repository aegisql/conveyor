package com.aegisql.conveyor.delay;

import org.junit.jupiter.api.*;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

// TODO: Auto-generated Javadoc
/**
 * The Class SomeDelayTests.
 */
public class SomeDelayTests {
	
	
	/**
	 * The Class D.
	 */
	public static class D implements Delayed {

		/** The d. */
		String d;
		
		/** The c. */
		int c = -1;
		
		/** The e. */
		long e = 1;
		
		/**
		 * Instantiates a new d.
		 *
		 * @param d the d
		 */
		public D(String d) {
			this.d = d;
		}

		/**
		 * Instantiates a new d.
		 *
		 * @param d the d
		 * @param c the c
		 * @param e the e
		 */
		public D(String d,int c, long e) {
			this.d = d;
			this.c = c;
			this.e = e;
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(Delayed o) {
			System.out.println("compareTo "+d);
			return c;
		}

		/* (non-Javadoc)
		 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
		 */
		@Override
		public long getDelay(TimeUnit unit) {
			System.out.println("getDelay "+d);
			return e;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "D [" + d + "]";
		}
		
	}

	/**
	 * Sets the up before class.
	 *
	 */
	@BeforeAll
	public static void setUpBeforeClass() {
	}

	/**
	 * Tear down after class.
	 *
	 */
	@AfterAll
	public static void tearDownAfterClass() {
	}

	/**
	 * Sets the up.
	 *
	 */
	@BeforeEach
	public void setUp() {
	}

	/**
	 * Tear down.
	 *
	 */
	@AfterEach
	public void tearDown() {
	}

	/**
	 * Test.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
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
