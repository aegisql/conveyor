package com.aegisql.conveyor.utils.collection;

import java.util.Collection;

import com.aegisql.conveyor.AssemblingConveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class CollectionConveyor.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class CollectionConveyor <K,V> extends AssemblingConveyor<K, CollectionBuilderLabel<V>, Collection<V>> {

	/**
	 * Instantiates a new collection conveyor.
	 */
	public CollectionConveyor() {
		super();
		this.setName("CollectionConveyor");
	}

}
