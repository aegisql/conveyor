package com.aegisql.conveyor.demo.caching_conveyor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.Test;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.demo.ThreadPool;
import com.aegisql.conveyor.utils.caching.CachingConveyor;

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
		
		// I - Create labels describing building steps
		final SmartLabel<PersonBuilder> FIRST_NAME    = SmartLabel.of(PersonBuilder::setFirstName);
		final SmartLabel<PersonBuilder> LAST_NAME     = SmartLabel.of(PersonBuilder::setLastName);
		final SmartLabel<PersonBuilder> DATE_OF_BIRTH = SmartLabel.of(PersonBuilder::setDateOfBirth);
		
		// II - Create conveyor
		CachingConveyor<Integer, SmartLabel<PersonBuilder>, Person> conveyor = new CachingConveyor<>();
		
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.HOURS);
		conveyor.setIdleHeartBeat(1, TimeUnit.SECONDS);
		
		// III - Tell it how to create the Builder
		conveyor.setBuilderSupplier(PersonBuilder::new);
		
		// IV - Tell it where to put the Product (asynchronously)
		conveyor.setResultConsumer( bin-> {/*no destination for the cache*/} );
		
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
		
		Thread.sleep(100);
		
		Supplier<? extends Person> personSupplier = conveyor.getProductSupplier(1);
				
		System.out.println( personSupplier.get() );
		
		pool.shutdown();
		conveyor.stop();
		
		
	}

	@Test
	public void test() throws Exception {
		main(null);
	}

	
}
