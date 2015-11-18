/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.io.Serializable;
import java.util.function.BiConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Interface SmartLabel.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <B> the generic type
 */
public interface SmartLabel<B> extends Serializable {
	
	/**
	 * Gets the setter.
	 *
	 * @return the setter
	 */
	BiConsumer<B, Object> getSetter();
}
