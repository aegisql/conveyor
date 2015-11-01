package com.aegisql.conveyor.user;

public class User {
	final String first;
	final String last;
	final int yearOfBirth;
	
	public User(String first, String last, int yob) {
		super();
		this.first = first;
		this.last = last;
		this.yearOfBirth = yob;
	}

	public String getFirst() {
		return first;
	}

	public String getLast() {
		return last;
	}
	
	public int getYearOfBirth() {
		return yearOfBirth;
	}

	@Override
	public String toString() {
		return "User [first=" + first + ", last=" + last + ", born in " + yearOfBirth + "]";
	}

}