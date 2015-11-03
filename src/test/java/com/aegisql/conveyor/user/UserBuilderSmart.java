package com.aegisql.conveyor.user;

import com.aegisql.conveyor.Builder;
import com.aegisql.conveyor.Lot;

public class UserBuilderSmart implements Builder<User> {

	String first;
	String last;
	Integer yearOfBirth;

	private boolean ready = false;

	public Integer getYearOfBirth() {
		return yearOfBirth;
	}

	public static void setYearOfBirth(UserBuilderSmart builder, Integer yob) {
		builder.yearOfBirth = yob;
	}

	public String getFirst() {
		return first;
	}

	public static void setFirst(UserBuilderSmart builder, String first) {
		builder.first = (String) first;
	}

	public String getLast() {
		return last;
	}

	public static void setLast(UserBuilderSmart builder, String last) {
		builder.last = last;
	}

	@Override
	public User build() {
		return new User(first, last, yearOfBirth==null? 0:yearOfBirth.intValue());
	}

	@Override
	public boolean ready(Lot lot) {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

}