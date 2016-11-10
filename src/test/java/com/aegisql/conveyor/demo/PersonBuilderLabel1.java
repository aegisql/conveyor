/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Enum PersonBuilderLabel1.
 */
public enum PersonBuilderLabel1 implements SmartLabel<ReactivePersonBuilder1> {
	
	/** The set first. */
	SET_FIRST(ReactivePersonBuilder1::setFirstName),
	
	/** The set last. */
	SET_LAST(ReactivePersonBuilder1::setLastName),
	
	/** The set year. */
	SET_YEAR(ReactivePersonBuilder1::setDateOfBirth)
	;

	/** The setter. */
	BiConsumer<ReactivePersonBuilder1, Object> setter;

	/**
	 * Instantiates a new person builder label1.
	 *
	 * @param <T> the generic type
	 * @param setter the setter
	 */
	<T> PersonBuilderLabel1(BiConsumer<ReactivePersonBuilder1,T> setter) {
		this.setter = (BiConsumer<ReactivePersonBuilder1, Object>) setter;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#get()
	 */
	@Override
	public BiConsumer<ReactivePersonBuilder1, Object> get() {
		return setter;
	}
}
