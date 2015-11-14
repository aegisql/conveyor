/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.user;

// TODO: Auto-generated Javadoc
/**
 * The Class User.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class User {
	
	/** The first. */
	final String first;
	
	/** The last. */
	final String last;
	
	/** The year of birth. */
	final int yearOfBirth;
	
	/**
	 * Instantiates a new user.
	 *
	 * @param first the first
	 * @param last the last
	 * @param yob the yob
	 */
	public User(String first, String last, int yob) {
		this.first = first;
		this.last = last;
		this.yearOfBirth = yob;
	}

	/**
	 * Gets the first.
	 *
	 * @return the first
	 */
	public String getFirst() {
		return first;
	}

	/**
	 * Gets the last.
	 *
	 * @return the last
	 */
	public String getLast() {
		return last;
	}
	
	/**
	 * Gets the year of birth.
	 *
	 * @return the year of birth
	 */
	public int getYearOfBirth() {
		return yearOfBirth;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "User [first=" + first + ", last=" + last + ", born in " + yearOfBirth + "]";
	}

}