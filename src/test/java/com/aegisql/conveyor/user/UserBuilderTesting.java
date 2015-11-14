/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.user;

import java.util.function.Predicate;

import com.aegisql.conveyor.Builder;
import com.aegisql.conveyor.Lot;

// TODO: Auto-generated Javadoc
/**
 * The Class UserBuilderTesting.
 */
public class UserBuilderTesting implements Builder<User>, Predicate<Lot<UserBuilderEvents2>> {

	/** The first. */
	String first;
	
	/** The last. */
	String last;
	
	/** The year of birth. */
	Integer yearOfBirth;

	/** The ready. */
	private boolean ready = false;

	/**
	 * Gets the year of birth.
	 *
	 * @return the year of birth
	 */
	public Integer getYearOfBirth() {
		return yearOfBirth;
	}

	/** The yob set. */
	boolean yobSet = false;
	
	/**
	 * Sets the year of birth.
	 *
	 * @param builder the builder
	 * @param yob the yob
	 */
	public static void setYearOfBirth(UserBuilderTesting builder, Integer yob) {
		builder.yearOfBirth = yob;
		builder.yobSet = true;
	}

	/**
	 * Gets the first.
	 *
	 * @return the first
	 */
	public String getFirst() {
		return first;
	}

	/** The first set. */
	boolean firstSet = false;
	
	/**
	 * Sets the first.
	 *
	 * @param builder the builder
	 * @param first the first
	 */
	public static void setFirst(UserBuilderTesting builder, String first) {
		builder.first = (String) first;
		builder.firstSet = true;
	}

	/**
	 * Gets the last.
	 *
	 * @return the last
	 */
	public String getLast() {
		return last;
	}

	/** The last set. */
	boolean lastSet = false; 
	
	/**
	 * Sets the last.
	 *
	 * @param builder the builder
	 * @param last the last
	 */
	public static void setLast(UserBuilderTesting builder, String last) {
		builder.last = last;
		builder.lastSet = true;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Builder#build()
	 */
	@Override
	public User build() {
		return new User(first, last, yearOfBirth==null? 0:yearOfBirth.intValue());
	}

	/**
	 * Ready.
	 *
	 * @return true, if successful
	 */
	public boolean ready() {
		return ready;
	}

	/**
	 * Sets the ready.
	 *
	 * @param ready the new ready
	 */
	public void setReady(boolean ready) {
		this.ready = ready;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Predicate#test(java.lang.Object)
	 */
	@Override
	public boolean test(Lot<UserBuilderEvents2> lot) {
		return firstSet && lastSet && yobSet;
	}

}