/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.simple_conveyor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.demo.ThreadPool;

public class Demo {

	public static void main(String[] args) throws ParseException, InterruptedException, ExecutionException {

		ThreadPool pool                   = new ThreadPool();
		SimpleDateFormat format           = new SimpleDateFormat("yyyy-MM-dd");
		
		// I - Create conveyor
		// Generic types:
		// Integer - type of the unique build ID
		// String  - type of the message labels
		// Person  - type of the Product
		Conveyor<Integer, String, Person> conveyor = new AssemblingConveyor<>();
		
		// II - Tell conveyor how to create the Builder
		conveyor.setBuilderSupplier(PersonBuilder::new);
		
		// III - Explain conveyor how to process Building Parts
		conveyor.setDefaultCartConsumer(Conveyor.getConsumerFor(conveyor,PersonBuilder.class)
			//We use when method if we know how our labels look like. 
			.<String>when("FirstName", (builder,firstName) -> builder.setFirstName(firstName))
			//We can also use regular expressions to match complex label patterns to some action
			//This example will math the "LastName" label
			.<String>match("^L.*e$", (builder,lastName) -> builder.setLastName(lastName))
			//or we can use custom filter. It will be accepted every time when
			//corresponding predicate returns true
			.<Date>filter((l)->"dateofbirth".equalsIgnoreCase(l), (builder,dateOfBirth)->builder.setDateOfBirth(dateOfBirth))
		);
		
		// IV - Build is ready when builder accepted three different pieces of data
		conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(3));
		
		// V - Tell conveyor where to put created Person object
		//     Product receiver should not block the thread 
		LastResultReference<Integer,Person> personRef = LastResultReference.of(conveyor);
		conveyor.resultConsumer().first( personRef ).set();
		
		// VI - Optionally: retrieve completable future of the build
		CompletableFuture<Person> future = conveyor.build().id(1).createFuture();
		
		// VII - Send data to conveyor asynchronously
		pool.runAsynchWithDelay(10,()->{
			conveyor
				.part()
				.value("John")
				.id(1)
				.label("FirstName")
				.place();
			}
		);
		pool.runAsynchWithDelay(10,()->{
			conveyor
				.part()
				.value("Silver")
				.id(1)
				.label("LastName")
				.place();
			}
		);
		pool.runAsynchWithDelay(10,()->{
			try {
				conveyor
					.part()
					.id(1)
					.value(format.parse("1695-11-10"))
					.label("DateOfBirth")
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
