package com.aegisql.conveyor.user;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

public enum UserBuilderEvents implements SmartLabel<UserBuilderSmart> {
	
	
	SET_FIRST(UserBuilderSmart::setFirst),
	SET_LAST(UserBuilderSmart::setLast),
	SET_YEAR(UserBuilderSmart::setYearOfBirth)
	;

	BiConsumer<UserBuilderSmart, Object> setter;

	<T> UserBuilderEvents(BiConsumer<UserBuilderSmart,T> setter) {
		this.setter = (BiConsumer<UserBuilderSmart, Object>) setter;
	}
	
	@Override
	public BiConsumer<UserBuilderSmart, Object> getSetter() {
		return setter;
	}
}
