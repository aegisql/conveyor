package com.aegisql.conveyor.user;

// TODO: Auto-generated Javadoc
/**
 * The Class UpperUser.
 */
public class UpperUser extends User {

	/**
	 * Instantiates a new upper user.
	 *
	 * @param first the first
	 * @param last the last
	 * @param yob the yob
	 */
	public UpperUser(String first, String last, int yob) {
		super(first, last, yob);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.user.User#toString()
	 */
	@Override
	public String toString() {
		return "UpperUser [first=" + first + ", last=" + last + ", yearOfBirth=" + yearOfBirth + "]";
	}

}
