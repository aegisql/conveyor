package com.aegisql.conveyor.parallel;

// TODO: Auto-generated Javadoc
/**
 * The Interface BalancingFunction.
 *
 * @param <K> the key type
 */
@FunctionalInterface
public interface BalancingFunction<K> {
	
	/**
	 * Balance cart.
	 *
	 * @param key the key
	 * @return the int
	 */
	public int balanceCart(K key);
}
