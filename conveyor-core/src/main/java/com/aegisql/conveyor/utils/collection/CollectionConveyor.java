package com.aegisql.conveyor.utils.collection;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.loaders.PartLoader;

import java.util.Collection;

// TODO: Auto-generated Javadoc
/**
 * The Class CollectionConveyor.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class CollectionConveyor <K,V> extends AssemblingConveyor<K, SmartLabel<CollectionBuilder<V>>, Collection<V>> {

	/**
	 * Instantiates a new collection conveyor.
	 */
	public CollectionConveyor() {
		super();
		this.setName("CollectionConveyor");
		this.setReadinessEvaluator(Conveyor.getTesterFor(this).accepted(COMPLETE));
	}

	public final SmartLabel<CollectionBuilder<V>> ITEM = SmartLabel.of((b,v)-> CollectionBuilder.add(b, (V)v));

	public final SmartLabel<CollectionBuilder<V>> COMPLETE = SmartLabel.of(()->{});

	@Override
	public PartLoader<K, SmartLabel<CollectionBuilder<V>>> part() {
		return super.part().label(ITEM);
	}

	
}
