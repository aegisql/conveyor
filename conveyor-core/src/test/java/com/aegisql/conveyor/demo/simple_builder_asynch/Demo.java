/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.simple_builder_asynch;

import com.aegisql.conveyor.demo.ThreadPool;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Demo {
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		
		ThreadPool pool         = new ThreadPool();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		PersonBuilder builder   = new PersonBuilder();
		// << Builder is created, but it is empty. 
		//    Needs three pieces of data to build the person
		//    Adding building parts asynchronously in three separate threads
		Future<?> f1 = pool.runAsynchWithDelay(10,()->{
			builder.setFirstName("John");
			}
		);
		Future<?> f2 = pool.runAsynchWithDelay(100,()->{
			builder.setLastName("Silver");
			}
		);
		Future<?> f3 = pool.runAsynchWithDelay(1000,()->{
			try {
				builder.setDateOfBirth( format.parse("1695-11-10") );
			} catch (Exception e) {}
			}
		);
		// Most likely not ready
		Person person = builder.get();
		System.out.println( "0\t"+ person );
		f1.get();
		// not ready
		person = builder.get();
		System.out.println( "10\t"+ person );
		f2.get();
		// still not ready
		person = builder.get();
		System.out.println( "100\t"+ person );
		f3.get();
		// Hopefully ready now
		person = builder.get();
		System.out.println( "1000\t"+ person );
		
		pool.shutdown();
		
	}

	@Test
	public void test() throws Exception {
		main(null);
	}

}
