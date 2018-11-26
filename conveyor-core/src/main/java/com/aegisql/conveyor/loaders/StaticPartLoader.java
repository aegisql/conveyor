/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.loaders;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// TODO: Auto-generated Javadoc
/**
 * The Class StaticPartLoader.
 *
 * @param <L> the generic type
 * @param <V> the value type
 * @param <OUT> the generic type
 * @param <F> the generic type
 */
public final class StaticPartLoader<L,V,OUT,F> {

	/** The placer. */
	private final Function<StaticPartLoader<L,?,OUT,F>, CompletableFuture<F>> placer;
	
	/** The label. */
	public final L label;
	
	/** The part value. */
	public final V staticPartValue;
	
	/** The create. */
	public final boolean create;
	
	/**  The priority. */
	public final long priority;
	
	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 * @param label the label
	 * @param value the value
	 * @param create the create
	 * @param priority the priority
	 */
	private StaticPartLoader(Function<StaticPartLoader<L,?,OUT,F>, CompletableFuture<F>> placer,L label, V value, boolean create, long priority) {
		this.placer = placer;
		this.label = label;
		this.staticPartValue = value;
		this.create = create;
		this.priority = priority;
	}

	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 */
	public StaticPartLoader(Function<StaticPartLoader<L,?,OUT,F>, CompletableFuture<F>> placer) {
		this(placer,null,null,true,0);
	}
	
	/**
	 * Id.
	 *
	 * @return the part loader
	 */
	public StaticPartLoader<L,V,OUT,F> delete() {
		return new StaticPartLoader<L,V,OUT,F>(placer,label,staticPartValue,false,priority);
	}

	/**
	 * Label.
	 *
	 * @param l the l
	 * @return the part loader
	 */
	public StaticPartLoader<L,V,OUT,F> label(L l) {
		return new StaticPartLoader<L,V,OUT,F>(placer,l,staticPartValue,create,priority);
	}

	/**
	 * Priority.
	 *
	 * @param p the p
	 * @return the static part loader
	 */
	public StaticPartLoader<L,V,OUT,F> priority(long p) {
		return new StaticPartLoader<L,V,OUT,F>(placer,label,staticPartValue,create,p);
	}

	/**
	 * Value.
	 *
	 * @param <X> the generic type
	 * @param v the v
	 * @return the part loader
	 */
	public<X> StaticPartLoader<L,X,OUT,F> value(X v) {
		return new StaticPartLoader<L,X,OUT,F>(placer,label,v,true,priority);
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
		return "StaticPartLoader [" + (create ? "create ":"delete ") + "label=" + label + ", staticValue=" + staticPartValue + ", priority="+priority+"]";
	}
	
}
