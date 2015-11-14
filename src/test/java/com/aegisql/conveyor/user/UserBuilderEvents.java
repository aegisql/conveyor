/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.user;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Enum UserBuilderEvents.
 */
public enum UserBuilderEvents implements SmartLabel<UserBuilderSmart> {
	
	/** The create. */
	CREATE((a,b)->{}), /** The set first. */
 //Just create and do not doo anything
	SET_FIRST(UserBuilderSmart::setFirst),
	
	/** The set last. */
	SET_LAST(UserBuilderSmart::setLast),
	
	/** The set year. */
	SET_YEAR(UserBuilderSmart::setYearOfBirth)
	;

	/** The setter. */
	BiConsumer<UserBuilderSmart, Object> setter;

	/**
	 * Instantiates a new user builder events.
	 *
	 * @param <T> the generic type
	 * @param setter the setter
	 */
	<T> UserBuilderEvents(BiConsumer<UserBuilderSmart,T> setter) {
		this.setter = (BiConsumer<UserBuilderSmart, Object>) setter;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#getSetter()
	 */
	@Override
	public BiConsumer<UserBuilderSmart, Object> getSetter() {
		return setter;
	}
}
