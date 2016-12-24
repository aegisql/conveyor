/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.conveyor_timeout_action;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.SmartWrapper;
import com.aegisql.conveyor.demo.ThreadPool;

public class Demo {
	
	public static void main(String[] args) throws ParseException, InterruptedException, ExecutionException {
		ThreadPool pool                   = new ThreadPool();
		SimpleDateFormat format           = new SimpleDateFormat("yyyy-MM-dd");
		AtomicReference<Person> personRef = new AtomicReference<>();
		
		// I - Create labels describing building steps
		final SmartLabel<PersonBuilder> FIRST_NAME    = new SmartWrapper<String, PersonBuilder, String>("FirtsName",PersonBuilder::setFirstName);
		final SmartLabel<PersonBuilder> LAST_NAME     = new SmartWrapper<String, PersonBuilder, String>("LastName",PersonBuilder::setLastName);
		final SmartLabel<PersonBuilder> DATE_OF_BIRTH = new SmartWrapper<String, PersonBuilder, Date>("DateOfBirth",PersonBuilder::setDateOfBirth);
		
		// II - Create conveyor
		Conveyor<Integer, SmartLabel<PersonBuilder>, Person> conveyor = new AssemblingConveyor<>();
		
		// III - Tell it how to create the Builder
		conveyor.setBuilderSupplier(PersonBuilder::new);
		
		// IV - Tell it where to put the Product (asynchronously)
		conveyor.setResultConsumer( bin-> personRef.set(bin.product) );
		
		// V - Set default timeout and heartbeat intervals
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		conveyor.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		
		conveyor.setOnTimeoutAction(b->{
			PersonBuilder builder = (PersonBuilder)b;
			if(builder.getFirstName() != null && builder.getLastName() != null) {
				builder.forceReady();
			}
			
		});
		
		// IV - Add data to conveyor queue 
		pool.runAsynchWithDelay(10,()->{
			conveyor.add(1, "John", FIRST_NAME);
			}
		);
		pool.runAsynchWithDelay(20,()->{
			conveyor.add(1, "Silver", LAST_NAME);
			}
		);
		pool.runAsynchWithDelay(500,()->{
			try {
				conveyor.add(1, format.parse("1695-11-10"), DATE_OF_BIRTH);
			} catch (Exception e) {}
			}
		);
		
		// V - Optionally - get future of existing build
		CompletableFuture<Person> future = conveyor.getFuture(1);
		Person person = future.get();
		System.out.println( person );

		Thread.sleep(1000);

		pool.shutdown();
		conveyor.stop();
	}

	@Test
	public void test() throws Exception {
		main(null);
	}
}
