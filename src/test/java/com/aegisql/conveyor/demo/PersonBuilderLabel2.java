/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

public enum PersonBuilderLabel2 implements SmartLabel<ReactivePersonBuilder2> {
	
	SET_FIRST(ReactivePersonBuilder2::setFirstName),
	SET_LAST(ReactivePersonBuilder2::setLastName),
	SET_YEAR(ReactivePersonBuilder2::setDateOfBirth)
	;

	BiConsumer<ReactivePersonBuilder2, Object> setter;

	<T> PersonBuilderLabel2(BiConsumer<ReactivePersonBuilder2,T> setter) {
		this.setter = (BiConsumer<ReactivePersonBuilder2, Object>) setter;
	}
	
	@Override
	public BiConsumer<ReactivePersonBuilder2, Object> get() {
		return setter;
	}
}
