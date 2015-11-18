/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.function.Predicate;

@FunctionalInterface
public interface TestingState <K> extends Predicate<State<K>> {

}
