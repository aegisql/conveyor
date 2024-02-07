/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.simple_builder;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Demo {
	
	public static void main(String[] args) throws ParseException {
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		PersonBuilder builder   = new PersonBuilder();
		// << Builder is created, but it is empty. 
		//    Needs three pieces of data to build the person
		//    Adding building parts in the same thread
		builder.setFirstName("John");
		builder.setLastName("Silver");
		builder.setDateOfBirth( format.parse("1695-11-10") );
		// << Ok, here we know that we ready to build the Person
		Person person = builder.get();
		
		System.out.println( person );
				
	}
	
	@Test
	public void test() throws Exception {
		main(null);
	}

}
