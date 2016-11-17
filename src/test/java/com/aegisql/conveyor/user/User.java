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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((last == null) ? 0 : last.hashCode());
		result = prime * result + yearOfBirth;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (last == null) {
			if (other.last != null)
				return false;
		} else if (!last.equals(other.last))
			return false;
		if (yearOfBirth != other.yearOfBirth)
			return false;
		return true;
	}

}