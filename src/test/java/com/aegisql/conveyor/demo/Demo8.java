package com.aegisql.conveyor.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.ShoppingCart;

public class Demo8 {
	
	public static void main(String[] args) throws ParseException, InterruptedException {
		final SimpleDateFormat format     = new SimpleDateFormat("yyyy-MM-dd");
		AtomicReference<Person> personRef = new AtomicReference<>();
		
		// I - Create conveyor
		AssemblingConveyor<Integer, String, Person> conveyor = new AssemblingConveyor<>();
		
		// II - Tell it how to create the Builder
		conveyor.setBuilderSupplier(PersonBuilder1::new);
		
		// III - Explain how to process Building Parts
		conveyor.setDefaultCartConsumer((label, value, builder) -> {
			try {
				PersonBuilder1 personBuilder = (PersonBuilder1) builder;
				String[] parts = value.toString().split("\\|");
				personBuilder.setFirstName(parts[0]);
				personBuilder.setLastName(parts[1]);
				personBuilder.setDateOfBirth((Date) format.parse(parts[2]));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		
		// IV - How to evaluate readiness
		conveyor.setReadinessEvaluator((state,builder)->{
			return true;
		});
		
		// V - Tell it where to send the Product
		conveyor.setResultConsumer( bin-> personRef.set(bin.product) );
		
		// VI - Wrap building parts in the Shopping Cart
		ShoppingCart<Integer, String, String> csvCart = new ShoppingCart<>(1, "John|Silver|1695-11-10", "CSV");
		
		// VII - Add carts to conveyor queue 
		conveyor.add(csvCart);
		
		Thread.sleep(100);
		
		System.out.println( personRef.get() );
		
		
		
	}

}
