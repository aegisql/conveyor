package com.aegisql.conveyor.multichannel;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

public enum UserBuilderEvents implements SmartLabel<UserBuilder> {

	/** The set first. */
	SET_FIRST(UserBuilder::setFirst),
	
	/** The set last. */
	SET_LAST(UserBuilder::setLast),
	
	/** The set year. */
	SET_YEAR(UserBuilder::setYearOfBirth),
	
	MERGE_A(UserBuilder::mergeChannelA),

	MERGE_B(UserBuilder::mergeChannelB),

	INFO(UserBuilder::setInfo)
;

	/** The setter. */
	BiConsumer<UserBuilder, Object> setter;

	/**
	 * Instantiates a new user builder events2.
	 *
	 * @param <T> the generic type
	 * @param setter the setter
	 */
	<T> UserBuilderEvents (BiConsumer<UserBuilder,T> setter) {
		this.setter = (BiConsumer<UserBuilder, Object>) setter;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#getSetter()
	 */
	@Override
	public BiConsumer<UserBuilder, Object> get() {
		return setter;
	}

}
