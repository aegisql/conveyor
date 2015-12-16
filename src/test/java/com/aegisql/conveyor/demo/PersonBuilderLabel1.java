/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

public enum PersonBuilderLabel1 implements SmartLabel<ReactivePersonBuilder1> {
	
	SET_FIRST(ReactivePersonBuilder1::setFirstName),
	SET_LAST(ReactivePersonBuilder1::setLastName),
	SET_YEAR(ReactivePersonBuilder1::setDateOfBirth)
	;

	BiConsumer<ReactivePersonBuilder1, Object> setter;

	<T> PersonBuilderLabel1(BiConsumer<ReactivePersonBuilder1,T> setter) {
		this.setter = (BiConsumer<ReactivePersonBuilder1, Object>) setter;
	}
	
	@Override
	public BiConsumer<ReactivePersonBuilder1, Object> getSetter() {
		return setter;
	}
}
