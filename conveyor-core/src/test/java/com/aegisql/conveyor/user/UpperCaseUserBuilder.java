package com.aegisql.conveyor.user;

// TODO: Auto-generated Javadoc
/**
 * The Class UpperCaseUserBuilder.
 */
public class UpperCaseUserBuilder extends AbstractSmartUserBuilder {

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public User get() {
		return new UpperUser(first, last, yearOfBirth);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.user.AbstractSmartUserBuilder#acceptYearOfBirth(java.lang.Integer)
	 */
	@Override
	public void acceptYearOfBirth(Integer yob) {
		this.yearOfBirth = yob;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.user.AbstractSmartUserBuilder#acceptFirst(java.lang.String)
	 */
	@Override
	public void acceptFirst(String first) {
		this.first = first.toUpperCase();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.user.AbstractSmartUserBuilder#acceptLast(java.lang.String)
	 */
	@Override
	public void acceptLast(String last) {
		this.last = last.toUpperCase();
	}

}
