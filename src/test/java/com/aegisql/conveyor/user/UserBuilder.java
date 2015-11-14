/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.user;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.Builder;
import com.aegisql.conveyor.Lot;

// TODO: Auto-generated Javadoc
/**
 * The Class UserBuilder.
 */
public class UserBuilder implements Builder<User> {

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
	 * Sets the year of birth.
	 *
	 * @param yob the new year of birth
	 */
	public void setYearOfBirth(Integer yob) {
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
	 * Sets the first.
	 *
	 * @param first the new first
	 */
	public void setFirst(String first) {
		this.first = first;
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
	 * Sets the last.
	 *
	 * @param last the new last
	 */
	public void setLast(String last) {
		this.last = last;
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



}