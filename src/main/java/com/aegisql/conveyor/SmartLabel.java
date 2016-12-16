/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Interface SmartLabel.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <B> the generic type
 */
@FunctionalInterface
public interface SmartLabel<B> extends Serializable, Supplier<BiConsumer<B, Object>> {
	
	/**
	 * Gets the setter.
	 *
	 * @return the setter
	 */
	@Override
	BiConsumer<B, Object> get();
	
	default SmartLabel<B> identity() {
		return this;
	}
	
}
