package com.aegisql.conveyor.user;

public class UpperUser extends User {

	public UpperUser(String first, String last, int yob) {
		super(first, last, yob);
	}

	@Override
	public String toString() {
		return "UpperUser [first=" + first + ", last=" + last + ", yearOfBirth=" + yearOfBirth + "]";
	}

}
