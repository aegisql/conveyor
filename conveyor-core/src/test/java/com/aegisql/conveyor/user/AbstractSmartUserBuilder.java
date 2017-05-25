/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.user;

import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class UserBuilderSmart.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public abstract class AbstractSmartUserBuilder implements Supplier<User> {

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

	/**
	 * Accept year of birth.
	 *
	 * @param yob the yob
	 */
	public abstract void acceptYearOfBirth(Integer yob);
	/**
	 * Sets the year of birth.
	 *
	 * @param builder the builder
	 * @param yob the yob
	 */

	public static void setYearOfBirth(AbstractSmartUserBuilder builder, Integer yob) {
		builder.acceptYearOfBirth(yob);
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
	 * Accept first.
	 *
	 * @param first the first
	 */
	public abstract void acceptFirst(String first);
	/**
	 * Sets the first.
	 *
	 * @param builder the builder
	 * @param first the first
	 */
	public static void setFirst(AbstractSmartUserBuilder builder, String first) {
		builder.acceptFirst(first);
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
	 * Accept last.
	 *
	 * @param last the last
	 */
	public abstract void acceptLast(String last);
	/**
	 * Sets the last.
	 *
	 * @param builder the builder
	 * @param last the last
	 */
	public static void setLast(AbstractSmartUserBuilder builder, String last) {
		builder.acceptLast(last);
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

}