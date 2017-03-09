package com.aegisql.conveyor.loaders;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

// TODO: Auto-generated Javadoc
/**
 * The Class StaticPartLoader.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param <V> the value type
 * @param <OUT> the generic type
 * @param <F> the generic type
 */
public final class StaticPartLoader<K,L,V,OUT,F> {

	/** The placer. */
	private final Function<StaticPartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer;
	
	/** The label. */
	public final L label;
	
	/** The part value. */
	public final V staticPartValue;
	
	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 * @param label the label
	 * @param value the value
	 */
	private StaticPartLoader(Function<StaticPartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer,L label, V value) {
		this.placer = placer;
		this.label = label;
		this.staticPartValue = value;
	}

	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 */
	public StaticPartLoader(Function<StaticPartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer) {
		this(placer,null,null);
	}
	
	/**
	 * Id.
	 *
	 * @return the part loader
	 */
	public StaticPartLoader<K,L,V,OUT,F> foreach() {
		return new StaticPartLoader<K,L,V,OUT,F>(placer,label,staticPartValue);
	}

	/**
	 * Foreach
	 *
	 * @param filter the filtering predicate
	 * @return the part loader
	 */
	public StaticPartLoader<K,L,V,OUT,F> foreach(Predicate<K> filter) {
		return new StaticPartLoader<K,L,V,OUT,F>(placer,label,staticPartValue);
	}

	/**
	 * Label.
	 *
	 * @param l the l
	 * @return the part loader
	 */
	public StaticPartLoader<K,L,V,OUT,F> label(L l) {
		return new StaticPartLoader<K,L,V,OUT,F>(placer,l,staticPartValue);
	}

	/**
	 * Value.
	 *
	 * @param <X> the generic type
	 * @param v the v
	 * @return the part loader
	 */
	public<X> StaticPartLoader<K,L,X,OUT,F> value(X v) {
		return new StaticPartLoader<K,L,X,OUT,F>(placer,label,v);
	}

	/**
	 * Place.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<F> place() {
		return placer.apply(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "StaticPartLoader [label=" + label + ", staticValue=" + staticPartValue + "]";
	}
	
}
