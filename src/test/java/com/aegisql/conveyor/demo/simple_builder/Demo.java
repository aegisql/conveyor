package com.aegisql.conveyor.demo.simple_builder;

import java.text.ParseException;
import java.text.SimpleDateFormat;

// TODO: Auto-generated Javadoc
/**
 * The Class Demo1.
 */
public class Demo {
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws ParseException the parse exception
	 */
	public static void main(String[] args) throws ParseException {
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		PersonBuilder builder   = new PersonBuilder();
		
		builder.setFirstName("John");
		builder.setLastName("Silver");
		builder.setDateOfBirth( format.parse("1695-11-10") );

		Person person = builder.get();
		
		System.out.println( person );
				
	}

}
