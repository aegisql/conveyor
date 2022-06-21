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
public enum UserBuilderEvents implements SmartLabel<UserBuilderSmart> {
	
	/** The create. */
	CREATE((a,b)->{}), /** The set first. */
 //Just create and do not do anything
	SET_FIRST(UserBuilderSmart::setFirst),
	
	/** The set last. */
	SET_LAST(UserBuilderSmart::setLast),
	
	/** The set year. */
	SET_YEAR(UserBuilderSmart::setYearOfBirth),
	
	/** The failure. */
	FAILURE((UserBuilderSmart builder,RuntimeException error) -> { throw error; } ),
	
	PRINT((UserBuilderSmart builder,String msg) -> { System.out.println( msg+": "+builder.get()); })
	;

	
	
	/** The setter. */
	final BiConsumer<UserBuilderSmart, Object> setter;

	/**
	 * Instantiates a new user builder events.
	 *
	 * @param <T> the generic type
	 * @param setter the setter
	 */
	<T> UserBuilderEvents(BiConsumer<UserBuilderSmart,T> setter) {
		this.setter = (BiConsumer<UserBuilderSmart, Object>) setter;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#getSetter()
	 */
	@Override
	public BiConsumer<UserBuilderSmart, Object> get() {
		return setter;
	}
}
