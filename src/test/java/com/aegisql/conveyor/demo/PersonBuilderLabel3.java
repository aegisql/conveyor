/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Enum PersonBuilderLabel3.
 */
public enum PersonBuilderLabel3 implements SmartLabel<ReactivePersonBuilder3> {
	
	/** The set first. */
	SET_FIRST(ReactivePersonBuilder3::setFirstName),
	
	/** The set last. */
	SET_LAST(ReactivePersonBuilder3::setLastName),
	
	/** The set year. */
	SET_YEAR(ReactivePersonBuilder3::setDateOfBirth)
	;

	/** The setter. */
	BiConsumer<ReactivePersonBuilder3, Object> setter;

	/**
	 * Instantiates a new person builder label3.
	 *
	 * @param <T> the generic type
	 * @param setter the setter
	 */
	<T> PersonBuilderLabel3(BiConsumer<ReactivePersonBuilder3,T> setter) {
		this.setter = (BiConsumer<ReactivePersonBuilder3, Object>) setter;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#get()
	 */
	@Override
	public BiConsumer<ReactivePersonBuilder3, Object> get() {
		return setter;
	}
}
