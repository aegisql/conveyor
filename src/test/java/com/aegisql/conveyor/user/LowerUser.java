package com.aegisql.conveyor.user;

public class LowerUser extends User {

	public LowerUser(String first, String last, int yob) {
		super(first, last, yob);
	}

	@Override
	public String toString() {
		return "LowerUser [first=" + first + ", last=" + last + ", yearOfBirth=" + yearOfBirth + "]";
	}

}
