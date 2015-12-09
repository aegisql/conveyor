package com.aegisql.conveyor.utils.collection;

import java.util.Collection;

import com.aegisql.conveyor.AssemblingConveyor;

public class CollectionConveyor <K,V> extends AssemblingConveyor<K, CollectionBuilderLabel<V>, Collection<V>> {

	public CollectionConveyor() {
		super();
		this.setName("CollectionConveyor");
	}

}
