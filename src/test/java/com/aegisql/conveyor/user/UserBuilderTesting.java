package com.aegisql.conveyor.user;

import java.util.function.Predicate;

import com.aegisql.conveyor.Builder;
import com.aegisql.conveyor.Lot;

public class UserBuilderTesting implements Builder<User>, Predicate<Lot<UserBuilderEvents2>> {

	String first;
	String last;
	Integer yearOfBirth;

	private boolean ready = false;

	public Integer getYearOfBirth() {
		return yearOfBirth;
	}

	boolean yobSet = false;
	public static void setYearOfBirth(UserBuilderTesting builder, Integer yob) {
		builder.yearOfBirth = yob;
		builder.yobSet = true;
	}

	public String getFirst() {
		return first;
	}

	boolean firstSet = false;
	public static void setFirst(UserBuilderTesting builder, String first) {
		builder.first = (String) first;
		builder.firstSet = true;
	}

	public String getLast() {
		return last;
	}

	boolean lastSet = false; 
	public static void setLast(UserBuilderTesting builder, String last) {
		builder.last = last;
		builder.lastSet = true;
	}

	@Override
	public User build() {
		return new User(first, last, yearOfBirth==null? 0:yearOfBirth.intValue());
	}

	public boolean ready() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	@Override
	public boolean test(Lot<UserBuilderEvents2> lot) {
		return firstSet && lastSet && yobSet;
	}

}