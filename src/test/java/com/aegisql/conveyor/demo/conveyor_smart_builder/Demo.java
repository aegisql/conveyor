/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.conveyor_smart_builder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.demo.ThreadPool;

public class Demo {
	
	public static void main(String[] args) throws ParseException, InterruptedException, ExecutionException {
		ThreadPool pool                   = new ThreadPool();
		SimpleDateFormat format           = new SimpleDateFormat("yyyy-MM-dd");
		
		// I - Create labels describing building steps
		final SmartLabel<PersonBuilder> FIRST_NAME    = SmartLabel.of("FirtsName",PersonBuilder::setFirstName);
		final SmartLabel<PersonBuilder> LAST_NAME     = SmartLabel.of("LastName",PersonBuilder::setLastName);
		final SmartLabel<PersonBuilder> DATE_OF_BIRTH = SmartLabel.of("DateOfBirth",PersonBuilder::setDateOfBirth);
		
		// II - Create conveyor
		Conveyor<Integer, SmartLabel<PersonBuilder>, Person> conveyor = new AssemblingConveyor<>();
		
		// III - Tell it how to create the Builder
		conveyor.setBuilderSupplier(PersonBuilder::new);
		
		// IV - Tell it where to put the Product (asynchronously)
		conveyor.setResultConsumer( bin-> {/*nowhere. we'll use future*/});
		
		// V - Set default timeout and heartbeat intervals
		conveyor.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		
		// IV - Add data to conveyor queue 
		pool.runAsynchWithDelay(10,()->{
			conveyor.part().id(1).value("John").label(FIRST_NAME).place();
			}
		);
		pool.runAsynchWithDelay(20,()->{
			conveyor.part().id(1).value("Silver").label(LAST_NAME).place();
			}
		);
		pool.runAsynchWithDelay(500,()->{
			try {
				conveyor.part().id(1).value(format.parse("1695-11-10")).label(DATE_OF_BIRTH).place();
			} catch (Exception e) {}
			}
		);
		
		// V - Optionally - get future of existing build
		CompletableFuture<Person> future = conveyor.getFuture(1);
		Person person = future.get();
		System.out.println( person );

		Thread.sleep(600);

		pool.shutdown();
		conveyor.stop();
	}

	@Test
	public void test() throws Exception {
		main(null);
	}

	
}
