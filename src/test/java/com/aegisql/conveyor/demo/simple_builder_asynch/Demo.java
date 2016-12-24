/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.simple_builder_asynch;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.Test;

import com.aegisql.conveyor.demo.ThreadPool;

public class Demo {
	
	public static void main(String[] args) throws ParseException, InterruptedException {
		
		ThreadPool pool         = new ThreadPool();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		PersonBuilder builder   = new PersonBuilder();
		// << Builder is created, but it is empty. 
		//    Needs three pieces of data to build the person
		//    Adding building parts asynchronously in three separate threads
		pool.runAsynchWithDelay(10,()->{
			builder.setFirstName("John");
			}
		);
		pool.runAsynchWithDelay(100,()->{
			builder.setLastName("Silver");
			}
		);
		pool.runAsynchWithDelay(1000,()->{
			try {
				builder.setDateOfBirth( format.parse("1695-11-10") );
			} catch (Exception e) {}
			}
		);
		// Most likely not ready
		Person person = builder.get();
		System.out.println( "0\t"+ person );
		Thread.sleep(20);
		// not ready
		person = builder.get();
		System.out.println( "20\t"+ person );
		Thread.sleep(100);
		// still not ready
		person = builder.get();
		System.out.println( "120\t"+ person );
		Thread.sleep(1000);
		// Hopefully ready now
		person = builder.get();
		System.out.println( "1120\t"+ person );
		
		pool.shutdown();
		
	}

	@Test
	public void test() throws Exception {
		main(null);
	}

}
