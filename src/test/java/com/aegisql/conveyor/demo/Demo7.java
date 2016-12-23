package com.aegisql.conveyor.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.ShoppingCart;

// TODO: Auto-generated Javadoc
/**
 * The Class Demo7.
 */
public class Demo7 {
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws ParseException the parse exception
	 * @throws InterruptedException the interrupted exception
	 */
	public static void main(String[] args) throws ParseException, InterruptedException {
		
		// I - Create conveyor
		AssemblingConveyor<Integer, PersonBuilderLabel2, Person> conveyor = new AssemblingConveyor<>();
		
		// II - Tell it how to create the Builder
		conveyor.setBuilderSupplier(ReactivePersonBuilder2::new);
		
		// III - Tell it where to send the Product
		conveyor.setResultConsumer( bin-> {} );

		// IV - Set collection interval
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		
		// V - Wrap building parts in the Shopping Cart
		// VI - Add carts to conveyor queue 
		conveyor.add(1, "John", PersonBuilderLabel2.SET_FIRST);
		conveyor.add(1, "Silver", PersonBuilderLabel2.SET_LAST);
		
		Thread.sleep(200);
		
		conveyor.stop();
		
		
	}

}
