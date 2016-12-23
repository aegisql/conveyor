/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.simple_conveyor;

import java.util.Date;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class PersonBuilder1.
 */
public class PersonBuilder implements Supplier<Person> {
	
	/** The first name. */
	private String firstName;
	
	/** The last name. */
	private String lastName;
	
	/** The date of birth. */
	private Date dateOfBirth;
	
	/**
	 * Instantiates a new person builder1.
	 */
	public PersonBuilder() {

	}

	/**
	 * Sets the first name.
	 *
	 * @param firstName the new first name
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * Sets the last name.
	 *
	 * @param lastName the new last name
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * Sets the date of birth.
	 *
	 * @param dateOfBirth the new date of birth
	 */
	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public Person get() {
		return new Person(firstName,lastName,dateOfBirth);
	}
	
}