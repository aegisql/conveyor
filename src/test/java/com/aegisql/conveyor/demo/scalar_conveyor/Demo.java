/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.scalar_conveyor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.demo.ThreadPool;

public class Demo {
	
	public static void main(String[] args) throws ParseException, InterruptedException, ExecutionException {

		ThreadPool pool                   = new ThreadPool();
		SimpleDateFormat format           = new SimpleDateFormat("yyyy-MM-dd");
		
		// I - Create conveyor
		Conveyor<Integer, String, Person> conveyor = new AssemblingConveyor<>();
		
		// II - Tell it how to create the Builder
		conveyor.setBuilderSupplier(PersonBuilder::new);
		
		// III - Explain conveyor how to process Building Parts
		conveyor.setDefaultCartConsumer(Conveyor.getConsumerFor(conveyor)
				.filter((l)->true, (builder,value)->{
					try {
						PersonBuilder personBuilder = (PersonBuilder)builder;
						String[] parts = value.toString().split("\\|");
						personBuilder.setFirstName(parts[0]);
						personBuilder.setLastName(parts[1]);
						personBuilder.setDateOfBirth((Date) format.parse(parts[2]));
					} catch (Exception e) {
						throw new RuntimeException();
					}
				})
			);
		
		// IV - How to evaluate readiness - accepted three different pieces of data
		conveyor.setReadinessEvaluator((state,builder)->{
			return true;
		});
		
		// V - Tell it where to send the Product
		conveyor.setResultConsumer( bin-> {/*nowhere. we'll call future.get()*/} );
		
		// VI - Retrieve completable future of the build
		CompletableFuture<Person> future = conveyor.createBuildFuture(1);
		
		// VII - Send data to conveyor queue (asynchronously)
		pool.runAsynchWithDelay(10,()->{
			conveyor.add(1, "John|Silver|1695-11-10", "CSV");
			}
		);
		
		Person person = future.get();
		
		System.out.println( person );
		
		pool.shutdown();
		conveyor.stop();
	}
	
	@Test
	public void test() throws Exception {
		main(null);
	}
}