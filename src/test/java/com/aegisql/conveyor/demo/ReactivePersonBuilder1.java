/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.Date;
import java.util.function.Supplier;

import com.aegisql.conveyor.State;
import com.aegisql.conveyor.TestingState;

// TODO: Auto-generated Javadoc
/**
 * The Class ReactivePersonBuilder1.
 */
public class ReactivePersonBuilder1 implements Supplier<Person>, TestingState<Integer, PersonBuilderLabel1> {
	
	/** The first name. */
	private String firstName;
	
	/** The last name. */
	private String lastName;
	
	/** The date of birth. */
	private Date dateOfBirth;
	
	/** The force ready. */
	private boolean forceReady = false;
	
	/**
	 * Instantiates a new reactive person builder1.
	 */
	public ReactivePersonBuilder1() {

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
	public static void setFirstName(ReactivePersonBuilder1 builder, String firstName) {
		builder.firstName = firstName;
	}

	/**
	 * Sets the last name.
	 *
	 * @param builder the builder
	 * @param lastName the last name
	 */
	public static void setLastName(ReactivePersonBuilder1 builder, String lastName) {
		builder.lastName = lastName;
	}

	/**
	 * Sets the date of birth.
	 *
	 * @param builder the builder
	 * @param dateOfBirth the date of birth
	 */
	public static void setDateOfBirth(ReactivePersonBuilder1 builder, Date dateOfBirth) {
		builder.dateOfBirth = dateOfBirth;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public Person get() {
		return new Person(firstName,lastName,dateOfBirth);
	}

	/* (non-Javadoc)
	 * @see java.util.function.Predicate#test(java.lang.Object)
	 */
	@Override
	public boolean test(State<Integer, PersonBuilderLabel1> state) {
		return state.previouslyAccepted == 3 || forceReady;
	}

	/**
	 * Sets the force ready.
	 *
	 * @param forceReady the new force ready
	 */
	public void setForceReady(boolean forceReady) {
		this.forceReady = forceReady;
	}
	
}