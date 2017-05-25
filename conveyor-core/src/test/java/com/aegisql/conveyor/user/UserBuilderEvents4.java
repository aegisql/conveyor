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
public enum UserBuilderEvents4 implements SmartLabel<UserBuilderTestingState> {
	
	/** The set first. */
	SET_FIRST(SmartLabel.of(UserBuilderTestingState::setFirst)),
	
	/** The set last. */
	SET_LAST(SmartLabel.of(UserBuilderTestingState::setLast)),
	
	/** The set year. */
	SET_YEAR(SmartLabel.of(UserBuilderTestingState::setYearOfBirth))
	;

	/** The setter. */
	private final SmartLabel<UserBuilderTestingState> inner;

	/**
	 * Instantiates a new user builder events2.
	 *
	 * @param <T> the generic type
	 * @param setter the setter
	 */
	<T> UserBuilderEvents4(SmartLabel<UserBuilderTestingState> inner) {
		this.inner = inner;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#getSetter()
	 */
	@Override
	public BiConsumer<UserBuilderTestingState, Object> get() {
		return inner.get();
	}
}
