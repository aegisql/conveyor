package com.aegisql.conveyor.user;

import com.aegisql.conveyor.Builder;

public class UserBuilder implements Builder<User> {

	String first;
	String last;
	Integer yearOfBirth;

	private boolean ready = false;

	public Integer getYearOfBirth() {
		return yearOfBirth;
	}

	public void setYearOfBirth(Integer yob) {
		this.yearOfBirth = yob;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}

	@Override
	public User build() {
		return new User(first, last, yearOfBirth);
	}

	@Override
	public boolean ready() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

}