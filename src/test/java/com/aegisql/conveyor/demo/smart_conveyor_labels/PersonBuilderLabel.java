/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.smart_conveyor_labels;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Enum PersonBuilderLabel1.
 */
public enum PersonBuilderLabel implements SmartLabel<PersonBuilder> {
	
	/** The set first. */
	SET_FIRST(PersonBuilder::setFirstName),
	
	/** The set last. */
	SET_LAST(PersonBuilder::setLastName),
	
	/** The set year. */
	SET_YEAR(PersonBuilder::setDateOfBirth)
	;

	/** The setter. */
	BiConsumer<PersonBuilder, Object> setter;

	/**
	 * Instantiates a new person builder label1.
	 *
	 * @param <T> the generic type
	 * @param setter the setter
	 */
	<T> PersonBuilderLabel(BiConsumer<PersonBuilder,T> setter) {
		this.setter = (BiConsumer<PersonBuilder, Object>) setter;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#get()
	 */
	@Override
	public BiConsumer<PersonBuilder, Object> get() {
		return setter;
	}
}
