package com.aegisql.conveyor.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.utils.caching.CachingConveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class Demo9.
 */
public class Demo9 {
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws ParseException the parse exception
	 * @throws InterruptedException the interrupted exception
	 */
	public static void main(String[] args) throws ParseException, InterruptedException {
		final SimpleDateFormat format     = new SimpleDateFormat("yyyy-MM-dd");
		
		// I - Create conveyor
		CachingConveyor<Integer, PersonBuilderLabel3, Person> conveyor = new CachingConveyor<>();
		
		// II - Tell it how to create the Builder
		conveyor.setBuilderSupplier(ReactivePersonBuilder3::new);
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.HOURS);
		
		// III - Wrap building parts in the Shopping Cart
		ShoppingCart<Integer, String, PersonBuilderLabel3> firstNameCart = new ShoppingCart<>(1, "John", PersonBuilderLabel3.SET_FIRST);
		ShoppingCart<Integer, String, PersonBuilderLabel3> lastNameCart = new ShoppingCart<>(1, "Silver", PersonBuilderLabel3.SET_LAST);
		ShoppingCart<Integer, Date, PersonBuilderLabel3>   dateOfBirthNameCart = new ShoppingCart<>(1, format.parse("1695-11-10"), PersonBuilderLabel3.SET_YEAR);
		
		// IV - Add carts to conveyor queue 
		conveyor.add(firstNameCart);
		conveyor.add(lastNameCart);
		conveyor.add(dateOfBirthNameCart);
		
		Thread.sleep(100);

		Supplier<? extends Person> personSupplier = conveyor.getProductSupplier(1);
		
		System.out.println( personSupplier.get() );
		
		ShoppingCart<Integer, Date, PersonBuilderLabel3>   dateOfBirthUpdate = new ShoppingCart<>(1, format.parse("1696-11-10"), PersonBuilderLabel3.SET_YEAR);
		conveyor.add(dateOfBirthUpdate);

		Thread.sleep(100);

		System.out.println( personSupplier.get() );

		
	}

}
