package com.aegisql.conveyor.persistence.core;

// TODO: Auto-generated Javadoc
/**
 * The Interface ObjectConverter.
 *
 * @param <O> the generic type
 * @param <P> the generic type
 */
public interface ObjectConverter <O,P> {
	
	/**
	 * To persistence.
	 *
	 * @param obj the obj
	 * @return the p
	 */
	P toPersistence(O obj);
	
	/**
	 * From persistence.
	 *
	 * @param p the p
	 * @return the o
	 */
	O fromPersistence(P p);
}
