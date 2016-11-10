package com.aegisql.conveyor.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.ShoppingCart;

// TODO: Auto-generated Javadoc
/**
 * The Class Demo3.
 */
public class Demo3 {
	
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
		
		// I - Create conveyor
		AssemblingConveyor<Integer, String, Person> conveyor = new AssemblingConveyor<>();
		
		// II - Tell it how to create the Builder
		conveyor.setBuilderSupplier(PersonBuilder1::new);
		
		// III - Explain how to process Building Parts
		conveyor.setDefaultCartConsumer((label, value, builder) -> {
			PersonBuilder1 personBuilder = (PersonBuilder1) builder;
			switch (label) {
			case "FirstName":
				personBuilder.setFirstName((String) value);
				break;
			case "LastName":
				personBuilder.setLastName((String) value);
				break;
			case "DateOfBirth":
				personBuilder.setDateOfBirth((Date) value);
				break;
			default:
				throw new RuntimeException("Unknown label " + label);
			}
		});
		
		// IV - How to evaluate readiness
		conveyor.setReadinessEvaluator((state,builder)->{
			return state.previouslyAccepted == 3;
		});
		
		// V - Tell it where to send the Product
		conveyor.setResultConsumer( bin-> personRef.set(bin.product) );
		
		// VI - Wrap building parts in the Shopping Cart
		ShoppingCart<Integer, String, String> firstNameCart = new ShoppingCart<>(1, "John", "FirstName");
		ShoppingCart<Integer, String, String> lastNameCart = new ShoppingCart<>(1, "Silver", "LastName");
		ShoppingCart<Integer, Date, String>   dateOfBirthNameCart = new ShoppingCart<>(1, format.parse("1695-11-10"), "DateOfBirth");
		
		// VII - Add carts to conveyor queue 
		conveyor.add(firstNameCart);
		conveyor.add(lastNameCart);
		conveyor.add(dateOfBirthNameCart);
		
		Thread.sleep(100);
		
		System.out.println( personRef.get() );
		
		
		
	}

}
