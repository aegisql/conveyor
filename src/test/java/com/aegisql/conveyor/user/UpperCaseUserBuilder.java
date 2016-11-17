package com.aegisql.conveyor.user;

public class UpperCaseUserBuilder extends AbstractSmartUserBuilder {

	@Override
	public User get() {
		return new User(first, last, yearOfBirth);
	}

	@Override
	public void acceptYearOfBirth(Integer yob) {
		this.yearOfBirth = yob;
	}

	@Override
	public void acceptFirst(String first) {
		this.first = first.toUpperCase();
	}

	@Override
	public void acceptLast(String last) {
		this.last = last.toUpperCase();
	}

}
