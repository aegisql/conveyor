/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Enum PersonBuilderLabel2.
 */
public enum PersonBuilderLabel2 implements SmartLabel<ReactivePersonBuilder2> {
	
	/** The set first. */
	SET_FIRST(ReactivePersonBuilder2::setFirstName),
	
	/** The set last. */
	SET_LAST(ReactivePersonBuilder2::setLastName),
	
	/** The set year. */
	SET_YEAR(ReactivePersonBuilder2::setDateOfBirth)
	;

	/** The setter. */
	BiConsumer<ReactivePersonBuilder2, Object> setter;

	/**
	 * Instantiates a new person builder label2.
	 *
	 * @param <T> the generic type
	 * @param setter the setter
	 */
	<T> PersonBuilderLabel2(BiConsumer<ReactivePersonBuilder2,T> setter) {
		this.setter = (BiConsumer<ReactivePersonBuilder2, Object>) setter;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#get()
	 */
	@Override
	public BiConsumer<ReactivePersonBuilder2, Object> get() {
		return setter;
	}
}
