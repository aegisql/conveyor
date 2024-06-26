/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.reflection;

import java.util.Date;

public class Person {
	
	public final String firstName;
	public final String lastName;
	public final Date dateOfBirth;
	
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