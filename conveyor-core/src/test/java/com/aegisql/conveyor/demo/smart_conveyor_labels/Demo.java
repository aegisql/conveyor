/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.smart_conveyor_labels;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.demo.ThreadPool;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.aegisql.conveyor.demo.smart_conveyor_labels.PersonBuilderLabel.*;

public class Demo {
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		ThreadPool pool                   = new ThreadPool();
		SimpleDateFormat format           = new SimpleDateFormat("yyyy-MM-dd");
		
		// II - Create conveyor
		Conveyor<Integer, PersonBuilderLabel, Person> conveyor = new AssemblingConveyor<>();
		
		// III - Tell it how to create the Builder
		conveyor.setBuilderSupplier(PersonBuilder::new);
		
		// IV - Tell it where to put the Product (asynchronously)
		LastResultReference<Integer,Person> personRef = LastResultReference.of(conveyor);
		conveyor.resultConsumer().first( personRef ).set();
		
		// IV - Add data to conveyor queue 
		pool.runAsynchWithDelay(10,()->{
			conveyor.part().id(1).value("John").label(SET_FIRST).place();
			}
		);
		pool.runAsynchWithDelay(20,()->{
			conveyor.part().id(1).value("Silver").label(SET_LAST).place();
			}
		);
		pool.runAsynchWithDelay(50,()->{
			try {
				conveyor.part().id(1).value(format.parse("1695-11-10")).label(SET_YEAR).place();
			} catch (Exception e) {}
			}
		);
		
		// V - Optionally - get future of existing build
		CompletableFuture<Person> future = conveyor.future().id(1).get();
		
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
