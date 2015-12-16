package com.aegisql.conveyor.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Demo1 {
	
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
