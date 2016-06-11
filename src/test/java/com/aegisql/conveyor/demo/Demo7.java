package com.aegisql.conveyor.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.ShoppingCart;

public class Demo7 {
	
	public static void main(String[] args) throws ParseException, InterruptedException {
		final SimpleDateFormat format     = new SimpleDateFormat("yyyy-MM-dd");
		AtomicReference<Person> personRef = new AtomicReference<>();
		
		// I - Create conveyor
		AssemblingConveyor<Integer, PersonBuilderLabel2, Person> conveyor = new AssemblingConveyor<>();
		
		// II - Tell it how to create the Builder
		conveyor.setBuilderSupplier(ReactivePersonBuilder2::new);
		
		// III - Tell it where to send the Product
		conveyor.setResultConsumer( bin-> personRef.set(bin.product) );

		// IV - Set collection interval
		conveyor.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		
		// V - Wrap building parts in the Shopping Cart
		ShoppingCart<Integer, String, PersonBuilderLabel2> firstNameCart = new ShoppingCart<>(1, "John", PersonBuilderLabel2.SET_FIRST);
		ShoppingCart<Integer, String, PersonBuilderLabel2> lastNameCart = new ShoppingCart<>(1, "Silver", PersonBuilderLabel2.SET_LAST);
		
		// VI - Add carts to conveyor queue 
		conveyor.add(firstNameCart);
		conveyor.add(lastNameCart);
		
		Thread.sleep(200);
		
		System.out.println( personRef.get() );
		
		
		
	}

}
