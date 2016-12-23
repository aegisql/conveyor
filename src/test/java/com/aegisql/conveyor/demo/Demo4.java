package com.aegisql.conveyor.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Class Demo4.
 */
public class Demo4 {
	
	private static ExecutorService pool = Executors.newFixedThreadPool(3);

	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws ParseException the parse exception
	 * @throws InterruptedException the interrupted exception
	 */
	public static void main(String[] args) throws ParseException, InterruptedException {
		final SimpleDateFormat format     = new SimpleDateFormat("yyyy-MM-dd");
		AtomicReference<Person> personRef = new AtomicReference<>();
		
		// I - Create labels describing building steps
		SmartLabel<ReactivePersonBuilder1> SET_FIRST = SmartLabel.of(ReactivePersonBuilder1::setFirstName);
		SmartLabel<ReactivePersonBuilder1> SET_LAST  = SmartLabel.of(ReactivePersonBuilder1::setLastName);
		SmartLabel<ReactivePersonBuilder1> SET_DOB   = SmartLabel.of(ReactivePersonBuilder1::setDateOfBirth);
		
		// II - Create conveyor
		AssemblingConveyor<Integer, SmartLabel<ReactivePersonBuilder1>, Person> conveyor = new AssemblingConveyor<>();
		
		// III - Tell it how to create the Builder
		conveyor.setBuilderSupplier(ReactivePersonBuilder1::new);
		
		// IV - Tell it where to send the Product
		conveyor.setResultConsumer( bin-> personRef.set(bin.product) );
		
		// IV - Add data to conveyor queue 
		pool.submit(()->conveyor.add(1, "John", SET_FIRST));
		pool.submit(()->conveyor.add(1, "Silver", SET_LAST));
		pool.submit(()->conveyor.add(1, format.parse("1695-11-10"), SET_DOB));
		
		Thread.sleep(100);
		
		System.out.println( personRef.get() );
		pool.shutdown();
		conveyor.stop();
		
		
	}

}
