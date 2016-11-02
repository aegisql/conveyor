/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

public enum PersonBuilderLabel3 implements SmartLabel<ReactivePersonBuilder3> {
	
	SET_FIRST(ReactivePersonBuilder3::setFirstName),
	SET_LAST(ReactivePersonBuilder3::setLastName),
	SET_YEAR(ReactivePersonBuilder3::setDateOfBirth)
	;

	BiConsumer<ReactivePersonBuilder3, Object> setter;

	<T> PersonBuilderLabel3(BiConsumer<ReactivePersonBuilder3,T> setter) {
		this.setter = (BiConsumer<ReactivePersonBuilder3, Object>) setter;
	}
	
	@Override
	public BiConsumer<ReactivePersonBuilder3, Object> get() {
		return setter;
	}
}
