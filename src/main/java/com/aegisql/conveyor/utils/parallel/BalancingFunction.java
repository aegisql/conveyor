package com.aegisql.conveyor.utils.parallel;

@FunctionalInterface
public interface BalancingFunction<K> {
	public int balanceCart(K key);
}
