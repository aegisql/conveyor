/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.user;

import java.util.function.Supplier;

import com.aegisql.conveyor.Expireable;

// TODO: Auto-generated Javadoc
/**
 * The Class UserBuilderDelayed.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class UserBuilderExpireable implements Supplier<User>, Expireable {

	/** The first. */
	String first;
	
	/** The last. */
	String last;
	
	/** The year of birth. */
	Integer yearOfBirth;

	/** The builder created. */
	private final long builderCreated = System.currentTimeMillis();
	
	/** The builder expiration. */
	private long builderExpiration;

	
	/** The ready. */
	private boolean ready = false;

	/**
	 * Instantiates a new user builder delayed.
	 *
	 * @param delay the delay
	 */
	public UserBuilderExpireable(long delay) {
		builderExpiration = builderCreated + delay;
	}
	
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
		this.builderExpiration += 100;
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
		//this.builderExpiration += 100;
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
		this.builderExpiration += 100;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public User get() {
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
	 * @see com.aegisql.conveyor.Expireable#getExpirationTime()
	 */
	@Override
	public long getExpirationTime() {
		return builderExpiration;
	}

	/**
	 * Adds the expiration time.
	 *
	 * @param time the time
	 */
	public void addExpirationTime(long time) {
		builderExpiration = System.currentTimeMillis()+time;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "UserBuilderExpireable [first=" + first + ", last=" + last + ", yearOfBirth=" + yearOfBirth
				+ ", builderCreated=" + builderCreated + ", builderExpiration=" + builderExpiration + ", ready=" + ready
				+ "]";
	}
	
	

	
}