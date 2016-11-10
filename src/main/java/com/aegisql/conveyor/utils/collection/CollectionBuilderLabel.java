package com.aegisql.conveyor.utils.collection;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Class CollectionBuilderLabel.
 *
 * @param <T> the generic type
 */
public class  CollectionBuilderLabel<T> implements SmartLabel<CollectionBuilder<T>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 145340944929861103L;
	
	/** The setter. */
	private BiConsumer<CollectionBuilder<T>, T> setter;
	
	/**
	 * Instantiates a new collection builder label.
	 *
	 * @param setter the setter
	 */
	CollectionBuilderLabel(BiConsumer<CollectionBuilder<T>, T> setter) {
		this.setter = setter;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#get()
	 */
	@Override
	public BiConsumer<CollectionBuilder<T>, Object> get() {
		return (BiConsumer<CollectionBuilder<T>, Object>) setter;
	}

	/**
	 * Adds the item label.
	 *
	 * @param <T> the generic type
	 * @return the collection builder label
	 */
	public static <T> CollectionBuilderLabel<T> addItemLabel() {
		BiConsumer<CollectionBuilder<T>, T> setter = CollectionBuilder::add;
		return new CollectionBuilderLabel( setter );
	}

	/**
	 * Complete collection label.
	 *
	 * @param <T> the generic type
	 * @return the collection builder label
	 */
	public static <T> CollectionBuilderLabel<T> completeCollectionLabel() {
		BiConsumer<CollectionBuilder<T>, T> setter = CollectionBuilder::complete;
		return new CollectionBuilderLabel( setter );
	}

}
