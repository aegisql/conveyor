/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.function.Predicate;

/**
 * The Interface TestingState.
 *
 * @param <K> the key type
 */
@FunctionalInterface
public interface TestingState <K> extends Predicate<State<K>> {

}
