package com.aegisql.conveyor.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.ShoppingCart;

public class Demo6 {
	
	public static void main(String[] args) throws ParseException, InterruptedException {
		final SimpleDateFormat format     = new SimpleDateFormat("yyyy-MM-dd");
		AtomicReference<Person> personRef = new AtomicReference<>();
		
		// I - Create conveyor
		AssemblingConveyor<Integer, PersonBuilderLabel1, Person> conveyor = new AssemblingConveyor<>();
		
		// II - Tell it how to create the Builder
		conveyor.setBuilderSupplier(ReactivePersonBuilder1::new);
		
		// III - Tell it where to send the Product
		conveyor.setResultConsumer( bin-> personRef.set(bin.product) );

		// IV - Set default timeout
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);

		// V - Set collection interval
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);

		// VI - Last chance to finalize build on timeout
		conveyor.setOnTimeoutAction(builder -> {
			ReactivePersonBuilder1 personBuilder = (ReactivePersonBuilder1)builder;
			if(personBuilder.getFirstName() != null && personBuilder.getLastName() != null) {
				personBuilder.setForceReady(true);
			}
		});
		
		// VII - Wrap building parts in the Shopping Cart
		ShoppingCart<Integer, String, PersonBuilderLabel1> firstNameCart = new ShoppingCart<>(1, "John", PersonBuilderLabel1.SET_FIRST);
		ShoppingCart<Integer, String, PersonBuilderLabel1> lastNameCart = new ShoppingCart<>(1, "Silver", PersonBuilderLabel1.SET_LAST);
		
		// VIII - Add carts to conveyor queue 
		conveyor.add(firstNameCart);
		conveyor.add(lastNameCart);
		
		Thread.sleep(200);
		
		System.out.println( personRef.get() );
		
	}

}
