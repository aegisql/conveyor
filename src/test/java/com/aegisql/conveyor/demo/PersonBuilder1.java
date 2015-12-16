/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.Date;
import java.util.function.Supplier;

public class PersonBuilder1 implements Supplier<Person> {
	
	private String firstName;
	private String lastName;
	private Date dateOfBirth;
	
	public PersonBuilder1() {

	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	@Override
	public Person get() {
		return new Person(firstName,lastName,dateOfBirth);
	}
	
}