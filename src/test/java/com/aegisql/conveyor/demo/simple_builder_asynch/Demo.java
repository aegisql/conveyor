package com.aegisql.conveyor.demo.simple_builder_asynch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.aegisql.conveyor.demo.ThreadPool;

// TODO: Auto-generated Javadoc
/**
 * The Class Demo1.
 */
public class Demo {
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws ParseException the parse exception
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws ParseException, InterruptedException {
		
		ThreadPool pool         = new ThreadPool();
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		
		PersonBuilder builder   = new PersonBuilder();
		
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
		
		Person person = builder.get();
		System.out.println( "0\t"+ person );
		Thread.sleep(20);
		person = builder.get();
		System.out.println( "20\t"+ person );
		Thread.sleep(100);
		person = builder.get();
		System.out.println( "120\t"+ person );
		Thread.sleep(1000);
		person = builder.get();
		System.out.println( "1120\t"+ person );
		
		pool.shutdown();
		
	}

}
