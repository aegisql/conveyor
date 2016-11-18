package com.aegisql.conveyor.user;

// TODO: Auto-generated Javadoc
/**
 * The Class LowerUser.
 */
public class LowerUser extends User {

	/**
	 * Instantiates a new lower user.
	 *
	 * @param first the first
	 * @param last the last
	 * @param yob the yob
	 */
	public LowerUser(String first, String last, int yob) {
		super(first, last, yob);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.user.User#toString()
	 */
	@Override
	public String toString() {
		return "LowerUser [first=" + first + ", last=" + last + ", yearOfBirth=" + yearOfBirth + "]";
	}

}
