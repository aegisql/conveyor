/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.user;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Enum UserBuilderEvents2.
 */
public enum UserBuilderEvents2 implements SmartLabel<UserBuilderTesting> {
	
	
	/** The set first. */
	SET_FIRST(UserBuilderTesting::setFirst),
	
	/** The set last. */
	SET_LAST(UserBuilderTesting::setLast),
	
	/** The set year. */
	SET_YEAR(UserBuilderTesting::setYearOfBirth)
	;

	/** The setter. */
	BiConsumer<UserBuilderTesting, Object> setter;

	/**
	 * Instantiates a new user builder events2.
	 *
	 * @param <T> the generic type
	 * @param setter the setter
	 */
	<T> UserBuilderEvents2(BiConsumer<UserBuilderTesting,T> setter) {
		this.setter = (BiConsumer<UserBuilderTesting, Object>) setter;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#getSetter()
	 */
	@Override
	public BiConsumer<UserBuilderTesting, Object> getSetter() {
		return setter;
	}
}
