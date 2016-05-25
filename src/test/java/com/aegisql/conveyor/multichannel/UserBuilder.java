package com.aegisql.conveyor.multichannel;

import java.util.function.Supplier;

import com.aegisql.conveyor.user.User;

public class UserBuilder implements Supplier<User> {

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
	 * @param builder the builder
	 * @param yob the yob
	 */
	public static void setYearOfBirth(UserBuilder builder, Integer yob) {
		builder.yearOfBirth = yob;
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
	 * @param builder the builder
	 * @param first the first
	 */
	public static void setFirst(UserBuilder builder, String first) {
		builder.first = (String) first;
	}

	public static void mergeChannelA(UserBuilder builder, User user) {
		builder.first = user.getFirst();
		builder.last  = user.getLast();
	}

	public static void mergeChannelB(UserBuilder builder, User user) {
		builder.yearOfBirth = user.getYearOfBirth();
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
	 * @param builder the builder
	 * @param last the last
	 */
	public static void setLast(UserBuilder builder, String last) {
		builder.last = last;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Builder#build()
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

}

