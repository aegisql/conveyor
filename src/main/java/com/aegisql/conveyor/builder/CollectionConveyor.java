package com.aegisql.conveyor.builder;

import java.util.Collection;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.Cart;

public class CollectionConveyor <K,V> extends AssemblingConveyor<K, CollectionBuilderLabel<V>, Cart<K,V,CollectionBuilderLabel<V>>, Collection<V>> {

	public CollectionConveyor() {
		super();
		this.setName("CollectionConveyor");
	}

}
