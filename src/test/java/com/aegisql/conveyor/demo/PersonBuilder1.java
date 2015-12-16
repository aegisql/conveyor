/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.Date;

public class PersonBuilder1 {
	
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

	public Person get() {
		return new Person(firstName,lastName,dateOfBirth);
	}
	
}