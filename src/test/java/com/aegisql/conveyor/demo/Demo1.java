package com.aegisql.conveyor.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;

// TODO: Auto-generated Javadoc
/**
 * The Class Demo1.
 */
public class Demo1 {
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws ParseException the parse exception
	 */
	public static void main(String[] args) throws ParseException {
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		
		PersonBuilder1 builder = new PersonBuilder1();
		
		builder.setFirstName("John");
		builder.setLastName("Silver");
		builder.setDateOfBirth( format.parse("1695-11-10") );

		Person person = builder.get();
		
		System.out.println( person );
		
		
		
	}

}
