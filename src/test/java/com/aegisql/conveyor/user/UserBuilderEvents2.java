package com.aegisql.conveyor.user;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

public enum UserBuilderEvents2 implements SmartLabel<UserBuilderTesting> {
	
	
	SET_FIRST(UserBuilderTesting::setFirst),
	SET_LAST(UserBuilderTesting::setLast),
	SET_YEAR(UserBuilderTesting::setYearOfBirth)
	;

	BiConsumer<UserBuilderTesting, Object> setter;

	<T> UserBuilderEvents2(BiConsumer<UserBuilderTesting,T> setter) {
		this.setter = (BiConsumer<UserBuilderTesting, Object>) setter;
	}
	
	@Override
	public BiConsumer<UserBuilderTesting, Object> getSetter() {
		return setter;
	}
}
