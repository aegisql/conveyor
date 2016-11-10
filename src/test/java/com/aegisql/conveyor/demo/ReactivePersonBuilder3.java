/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.Date;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class ReactivePersonBuilder3.
 */
public class ReactivePersonBuilder3 implements Supplier<Person> {
	
	/** The first name. */
	private String firstName;
	
	/** The last name. */
	private String lastName;
	
	/** The date of birth. */
	private Date dateOfBirth;
	
	/**
	 * Instantiates a new reactive person builder3.
	 */
	public ReactivePersonBuilder3() {

	}

	/**
	 * Gets the first name.
	 *
	 * @return the first name
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * Gets the last name.
	 *
	 * @return the last name
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * Gets the date of birth.
	 *
	 * @return the date of birth
	 */
	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	/**
	 * Sets the first name.
	 *
	 * @param builder the builder
	 * @param firstName the first name
	 */
	public static void setFirstName(ReactivePersonBuilder3 builder, String firstName) {
		builder.firstName = firstName;
	}

	/**
	 * Sets the last name.
	 *
	 * @param builder the builder
	 * @param lastName the last name
	 */
	public static void setLastName(ReactivePersonBuilder3 builder, String lastName) {
		builder.lastName = lastName;
	}

	/**
	 * Sets the date of birth.
	 *
	 * @param builder the builder
	 * @param dateOfBirth the date of birth
	 */
	public static void setDateOfBirth(ReactivePersonBuilder3 builder, Date dateOfBirth) {
		builder.dateOfBirth = dateOfBirth;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public Person get() {
		return new Person(firstName,lastName,dateOfBirth);
	}


	
}