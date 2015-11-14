/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.function.BiConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Interface SmartLabel.
 *
 * @param <L> the generic type
 */
public interface SmartLabel<L> {
	
	/**
	 * Gets the setter.
	 *
	 * @return the setter
	 */
	BiConsumer<L, Object> getSetter();
}
