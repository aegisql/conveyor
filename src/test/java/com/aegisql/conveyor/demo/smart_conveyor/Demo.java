package com.aegisql.conveyor.demo.smart_conveyor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.demo.ThreadPool;

// TODO: Auto-generated Javadoc
/**
 * The Class Demo4.
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
		ThreadPool pool                   = new ThreadPool();
		SimpleDateFormat format           = new SimpleDateFormat("yyyy-MM-dd");
		AtomicReference<Person> personRef = new AtomicReference<>();
		
		// I - Create labels describing building steps
		final SmartLabel<PersonBuilder> FIRST_NAME    = SmartLabel.of(PersonBuilder::setFirstName);
		final SmartLabel<PersonBuilder> LAST_NAME     = SmartLabel.of(PersonBuilder::setLastName);
		final SmartLabel<PersonBuilder> DATE_OF_BIRTH = SmartLabel.of(PersonBuilder::setDateOfBirth);
		
		// II - Create conveyor
		AssemblingConveyor<Integer, SmartLabel<PersonBuilder>, Person> conveyor = new AssemblingConveyor<>();
		
		// III - Tell it how to create the Builder
		conveyor.setBuilderSupplier(PersonBuilder::new);
		
		// IV - Tell it where to put the Product (asynchronously)
		conveyor.setResultConsumer( bin-> personRef.set(bin.product) );
		
		// IV - Add data to conveyor queue 
		pool.runAsynchWithDelay(10,()->{
			conveyor.add(1, "John", FIRST_NAME);
			}
		);
		pool.runAsynchWithDelay(20,()->{
			conveyor.add(1, "Silver", LAST_NAME);
			}
		);
		pool.runAsynchWithDelay(50,()->{
			try {
				conveyor.add(1, format.parse("1695-11-10"), DATE_OF_BIRTH);
			} catch (Exception e) {}
			}
		);
		
		// V - Optionally - get future of existing build
		CompletableFuture<Person> future = conveyor.getFuture(1);
		
		Person person = future.get();
		
		System.out.println( person );
		
		pool.shutdown();
		conveyor.stop();
		
		
	}

}
