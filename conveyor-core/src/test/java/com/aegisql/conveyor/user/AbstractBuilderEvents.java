/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.user;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.serial.SerializableBiConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Enum UserBuilderEvents.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public enum AbstractBuilderEvents implements SmartLabel<AbstractSmartUserBuilder> {
	
	/** The create. */
	CREATE((a,b)->{}), /** The set first. */
 //Just create and do not do anything
	SET_FIRST(AbstractSmartUserBuilder::setFirst),
	
	/** The set last. */
	SET_LAST(AbstractSmartUserBuilder::setLast),
	
	/** The set year. */
	SET_YEAR(AbstractSmartUserBuilder::setYearOfBirth)
	;

	/** The setter. */
	final BiConsumer<AbstractSmartUserBuilder, Object> setter;

	/**
	 * Instantiates a new user builder events.
	 *
	 * @param <T> the generic type
	 * @param setter the setter
	 */
	<T> AbstractBuilderEvents(BiConsumer<AbstractSmartUserBuilder,T> setter) {
		this.setter = (BiConsumer<AbstractSmartUserBuilder, Object>) setter;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#getSetter()
	 */
	@Override
	public BiConsumer<AbstractSmartUserBuilder, Object> get() {
		return setter;
	}
}
