package com.aegisql.conveyor.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.ShoppingCart;

// TODO: Auto-generated Javadoc
/**
 * The Class Demo5.
 */
public class Demo5 {
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws ParseException the parse exception
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException 
	 */
	public static void main(String[] args) throws ParseException {
		
		// I - Create conveyor
		AssemblingConveyor<Integer, PersonBuilderLabel1, Person> conveyor = new AssemblingConveyor<>();
		
		// II - Tell it how to create the Builder
		conveyor.setBuilderSupplier(ReactivePersonBuilder1::new);
		
		// III - Tell it where to send the Product
		conveyor.setResultConsumer( bin -> {} );

		// IV - Set default timeout
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);

		// IV - Set collection interval
		conveyor.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
				
		// VI - Add carts to conveyor queue 
		conveyor.add(1, "John", PersonBuilderLabel1.SET_FIRST);
		conveyor.add(1, "Silver", PersonBuilderLabel1.SET_LAST);
		
		CompletableFuture<Person> future = conveyor.getFuture(1);
		
		try {
			future.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		conveyor.stop();
		
		
	}

}
