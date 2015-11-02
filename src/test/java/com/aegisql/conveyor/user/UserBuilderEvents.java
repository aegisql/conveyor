package com.aegisql.conveyor.user;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

public enum UserBuilderEvents implements SmartLabel<UserBuilderSmart,Object> {
	
	
	SET_FIRST(UserBuilderSmart::setFirst),
	SET_LAST(UserBuilderSmart::setLast),
	SET_YEAR(UserBuilderSmart::setYearOfBirth)
	;

	BiConsumer<UserBuilderSmart, ?> setter;

	<T> UserBuilderEvents(BiConsumer<UserBuilderSmart,T> setter) {
		this.setter = setter;
	}
	
	@Override
	public BiConsumer<UserBuilderSmart, Object> getSetter() {
		return (BiConsumer<UserBuilderSmart, Object>) setter;
	}
}
