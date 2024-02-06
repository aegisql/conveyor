/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.reflection;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.demo.ThreadPool;
import com.aegisql.conveyor.reflection.SimpleConveyor;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Demo {

	public static void main(String[] args) throws InterruptedException, ExecutionException {

		ThreadPool pool                   = new ThreadPool();
		SimpleDateFormat format           = new SimpleDateFormat("yyyy-MM-dd");
		
		// I - Create conveyor
		// Generic types:
		// Integer - type of the unique build ID
		// Person  - type of the Product
		// II - Tell conveyor how to create the Builder		
		SimpleConveyor<Integer,Person> conveyor = new SimpleConveyor<>(PersonBuilder::new);		
		
		// III - Build is ready when builder accepted three different pieces of data
		conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted("firstName","lastName","dateOfBirth"));
		
		// IV - Tell conveyor where to put created Person object
		//     Product receiver should not block the thread 
		LastResultReference<Integer,Person> personRef = LastResultReference.of(conveyor);
		conveyor.resultConsumer().first( personRef ).set();
		
		// V - Optionally: retrieve completable future of the build
		CompletableFuture<Person> future = conveyor.build().id(1).createFuture();
		
		// VI - Send data to conveyor asynchronously
		pool.runAsynchWithRandomDelay(10,()->{
			conveyor
				.part()
				.value("John")
				.id(1)
				.label("firstName")
				.place();
			}
		);
		pool.runAsynchWithRandomDelay(10,()->{
			conveyor
				.part()
				.value("Silver")
				.id(1)
				.label("lastName")
				.place();
			}
		);
		pool.runAsynchWithRandomDelay(10,()->{
			try {
				conveyor
					.part()
					.id(1)
					.value(format.parse("1695-11-10"))
					.label("dateOfBirth")
					.place();
			} catch (Exception e) {}
			}
		);
		// No guess work. Method get() of the future
		// Will return result as soon as its built
		// and sent to the consumer
		Person person = future.get();
		
		System.out.println( "Person from asynchronous source: "+personRef.getCurrent() );
		System.out.println( "Person synchronized: "+person );
		
		pool.shutdown();
		conveyor.stop();
	}
	
	@Test
	public void test() throws Exception {
		main(null);
	}


}
