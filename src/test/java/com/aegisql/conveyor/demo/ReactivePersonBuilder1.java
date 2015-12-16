/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.Date;
import java.util.function.Supplier;

public class ReactivePersonBuilder1 implements Supplier<Person> {
	
	private String firstName;
	private String lastName;
	private Date dateOfBirth;
	
	public ReactivePersonBuilder1() {

	}

	public static void setFirstName(ReactivePersonBuilder1 builder, String firstName) {
		builder.firstName = firstName;
	}

	public static void setLastName(ReactivePersonBuilder1 builder, String lastName) {
		builder.lastName = lastName;
	}

	public static void setDateOfBirth(ReactivePersonBuilder1 builder, Date dateOfBirth) {
		builder.dateOfBirth = dateOfBirth;
	}

	@Override
	public Person get() {
		return new Person(firstName,lastName,dateOfBirth);
	}
	
}