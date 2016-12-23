/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.smart_conveyor_labels;

import java.util.Date;
import java.util.function.Supplier;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.TestingState;

// TODO: Auto-generated Javadoc
/**
 * The Class ReactivePersonBuilder1.
 */
public class PersonBuilder implements Supplier<Person>, Testing {
	
	/** The first name. */
	private String firstName;
	
	/** The last name. */
	private String lastName;
	
	/** The date of birth. */
	private Date dateOfBirth;
	
	/**
	 * Instantiates a new reactive person builder1.
	 */
	public PersonBuilder() {

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
	public static void setFirstName(PersonBuilder builder, String firstName) {
		builder.firstName = firstName;
	}

	/**
	 * Sets the last name.
	 *
	 * @param builder the builder
	 * @param lastName the last name
	 */
	public static void setLastName(PersonBuilder builder, String lastName) {
		builder.lastName = lastName;
	}

	/**
	 * Sets the date of birth.
	 *
	 * @param builder the builder
	 * @param dateOfBirth the date of birth
	 */
	public static void setDateOfBirth(PersonBuilder builder, Date dateOfBirth) {
		builder.dateOfBirth = dateOfBirth;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public Person get() {
		return new Person(firstName,lastName,dateOfBirth);
	}

	@Override
	public boolean test() {
		return firstName!=null && lastName != null && dateOfBirth != null;
	}
}