/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.smart_conveyor_labels;

import com.aegisql.conveyor.SmartLabel;

import java.util.function.BiConsumer;

public enum PersonBuilderLabel implements SmartLabel<PersonBuilder> {
	
	SET_FIRST(PersonBuilder::setFirstName),
	SET_LAST(PersonBuilder::setLastName),
	SET_YEAR(PersonBuilder::setDateOfBirth);

	final BiConsumer<PersonBuilder, Object> setter;

	<T> PersonBuilderLabel(BiConsumer<PersonBuilder,T> setter) {
		this.setter = (BiConsumer<PersonBuilder, Object>) setter;
	}
	
	@Override
	public BiConsumer<PersonBuilder, Object> get() {
		return setter;
	}
}
