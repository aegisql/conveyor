/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.simple_builder;

import java.util.Date;

public class Person {
	
	final String firstName;
	final String lastName;
	final Date dateOfBirth;
	
	public Person(String firstName, String lastName, Date dateOfBirth) {
		this.firstName   = firstName;
		this.lastName    = lastName;
		this.dateOfBirth = dateOfBirth;
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

	@Override
	public String toString() {
		return "Person [firstName=" + firstName + ", lastName=" + lastName + ", dateOfBirth=" + dateOfBirth + "]";
	}
}