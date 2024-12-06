/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.Conveyor;

/**
 * The interface Task pool conveyor m bean.
 */
public interface TaskPoolConveyorMBean {

	/**
	 * Gets name.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Gets enclosed conveyor name.
	 *
	 * @return the enclosed conveyor name
	 */
	String getEnclosedConveyorName();

	/**
	 * Conveyor conveyor.
	 *
	 * @param <K>   the type parameter
	 * @param <L>   the type parameter
	 * @param <OUT> the type parameter
	 * @return the conveyor
	 */
	<K,L,OUT> Conveyor<K, L, OUT> conveyor();

}
