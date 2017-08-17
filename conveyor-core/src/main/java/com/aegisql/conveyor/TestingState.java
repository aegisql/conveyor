/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import com.aegisql.conveyor.serial.SerializablePredicate;

/**
 * The Interface TestingState.
 *
 * @param <K> the key type
 * @param <L> the generic type
 */
@FunctionalInterface
public interface TestingState <K,L> extends SerializablePredicate<State<K,L>> {

}
