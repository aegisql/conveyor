/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.Date;
import java.util.function.Supplier;

public class ReactivePersonBuilder3 implements Supplier<Person> {
	
	private String firstName;
	private String lastName;
	private Date dateOfBirth;
	
	public ReactivePersonBuilder3() {

	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public static void setFirstName(ReactivePersonBuilder3 builder, String firstName) {
		builder.firstName = firstName;
	}

	public static void setLastName(ReactivePersonBuilder3 builder, String lastName) {
		builder.lastName = lastName;
	}

	public static void setDateOfBirth(ReactivePersonBuilder3 builder, Date dateOfBirth) {
		builder.dateOfBirth = dateOfBirth;
	}

	@Override
	public Person get() {
		return new Person(firstName,lastName,dateOfBirth);
	}


	
}