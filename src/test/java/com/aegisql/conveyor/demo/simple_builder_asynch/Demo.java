package com.aegisql.conveyor.demo.simple_builder_asynch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
		
		ExecutorService pool    = Executors.newFixedThreadPool(3);
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		
		PersonBuilder builder   = new PersonBuilder();
		
		pool.submit(()->{
			try {
				Thread.sleep(10);
			} catch (Exception e) {}
			builder.setFirstName("John");
			}
		);
		pool.submit(()->{
			try {
				Thread.sleep(100);
			} catch (Exception e) {}
			builder.setLastName("Silver");
			}
		);
		pool.submit(()->{
			try {
				Thread.sleep(1000);
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
