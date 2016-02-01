/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.user;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Enum UserBuilderEvents2.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public enum UserBuilderEvents3 implements SmartLabel<UserBuilderTestingState> {
	
	
	/** The set first. */
	SET_FIRST(UserBuilderTestingState::setFirst),
	
	/** The set last. */
	SET_LAST(UserBuilderTestingState::setLast),
	
	/** The set year. */
	SET_YEAR(UserBuilderTestingState::setYearOfBirth)
	;

	/** The setter. */
	BiConsumer<UserBuilderTestingState, Object> setter;

	/**
	 * Instantiates a new user builder events2.
	 *
	 * @param <T> the generic type
	 * @param setter the setter
	 */
	<T> UserBuilderEvents3(BiConsumer<UserBuilderTestingState,T> setter) {
		this.setter = (BiConsumer<UserBuilderTestingState, Object>) setter;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#getSetter()
	 */
	@Override
	public BiConsumer<UserBuilderTestingState, Object> get() {
		return setter;
	}
}
