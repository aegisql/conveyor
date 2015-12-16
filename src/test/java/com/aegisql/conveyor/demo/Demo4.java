package com.aegisql.conveyor.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.ShoppingCart;

public class Demo4 {
	
	public static void main(String[] args) throws ParseException, InterruptedException {
		final SimpleDateFormat format     = new SimpleDateFormat("yyyy-MM-dd");
		AtomicReference<Person> personRef = new AtomicReference<>();
		
		// I - Create conveyor
		AssemblingConveyor<Integer, PersonBuilderLabel1, Person> conveyor = new AssemblingConveyor<>();
		
		// II - Tell it how to create the Builder
		conveyor.setBuilderSupplier(ReactivePersonBuilder1::new);
		
		// III - Tell it where to send the Product
		conveyor.setResultConsumer( bin-> personRef.set(bin.product) );
		
		// IV - Wrap building parts in the Shopping Cart
		ShoppingCart<Integer, String, PersonBuilderLabel1> firstNameCart = new ShoppingCart<>(1, "John", PersonBuilderLabel1.SET_FIRST);
		ShoppingCart<Integer, String, PersonBuilderLabel1> lastNameCart = new ShoppingCart<>(1, "Silver", PersonBuilderLabel1.SET_LAST);
		ShoppingCart<Integer, Date, PersonBuilderLabel1>   dateOfBirthNameCart = new ShoppingCart<>(1, format.parse("1695-11-10"), PersonBuilderLabel1.SET_YEAR);
		
		// V - Add carts to conveyor queue 
		conveyor.add(firstNameCart);
		conveyor.add(lastNameCart);
		conveyor.add(dateOfBirthNameCart);
		
		Thread.sleep(100);
		
		System.out.println( personRef.get() );
		
		
		
	}

}
