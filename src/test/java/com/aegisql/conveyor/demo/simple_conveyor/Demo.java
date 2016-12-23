package com.aegisql.conveyor.demo.simple_conveyor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class Demo3.
 */
public class Demo {
	
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws ParseException the parse exception
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException 
	 */
	public static void main(String[] args) throws ParseException, InterruptedException, ExecutionException {

		ExecutorService pool              = Executors.newFixedThreadPool(3);
		SimpleDateFormat format           = new SimpleDateFormat("yyyy-MM-dd");
		AtomicReference<Person> personRef = new AtomicReference<>();
		
		// I - Create conveyor
		AssemblingConveyor<Integer, String, Person> conveyor = new AssemblingConveyor<>();
		
		// II - Tell it how to create the Builder
		conveyor.setBuilderSupplier(PersonBuilder::new);
		
		// III - Explain conveyor how to process Building Parts
		conveyor.setDefaultCartConsumer(Conveyor.getConsumerFor(conveyor)
				.when("FirstName", (builder,value)->{
					((PersonBuilder) builder).setFirstName((String)value);
				})
				.when("LastName", (builder,value)->{
					((PersonBuilder) builder).setLastName((String)value);
				})
				.filter((l)->"dateofbirth".equalsIgnoreCase(l), (builder,value)->{
					((PersonBuilder) builder).setDateOfBirth((Date) value);
				})
			);
		
		// IV - How to evaluate readiness - accepted three different pieces of data
		conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(3));
		
		// V - Tell it where to send the Product
		conveyor.setResultConsumer( bin-> personRef.set(bin.product) );
		
		// VI - Optionally: retrieve completable future of the build
		CompletableFuture<Person> future = conveyor.createBuildFuture(1);
		
		// VII - Send data to conveyor queue (asynchronously)
		pool.submit(()->{
			try {
				Thread.sleep(10);
			} catch (Exception e) {}
			conveyor.add(1, "John", "FirstName");
			}
		);
		pool.submit(()->{
			try {
				Thread.sleep(100);
			} catch (Exception e) {}
			conveyor.add(1, "Silver", "LastName");
			}
		);
		pool.submit(()->{
			try {
				Thread.sleep(50);
				conveyor.add(1, format.parse("1695-11-10"), "DateOfBirth");
			} catch (Exception e) {}
			}
		);
		
		Person person = future.get();
		
		System.out.println( "Person from asynchronous source: "+personRef.get() );
		System.out.println( "Person synchronized: "+person );
		
		pool.shutdown();
		conveyor.stop();
	}

}
