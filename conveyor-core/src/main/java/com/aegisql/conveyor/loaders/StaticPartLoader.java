/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.loaders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import com.aegisql.conveyor.Conveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class StaticPartLoader.
 *
 * @param <L> the generic type
 */
public final class StaticPartLoader<L> {

	/** The placer. */
	private final Function<StaticPartLoader<L>, CompletableFuture<Boolean>> placer;
	
	/** The label. */
	public final L label;
	
	/** The part value. */
	public final Object staticPartValue;
	
	/** The create. */
	public final boolean create;
	
	/**  The priority. */
	public final long priority;
	
	/** The properties. */
	private final Map<String,Object> properties = new HashMap<>();

	
	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 * @param label the label
	 * @param value the value
	 * @param create the create
	 * @param priority the priority
	 */
	private StaticPartLoader(Function<StaticPartLoader<L>
	, CompletableFuture<Boolean>> placer
	,L label
	, Object value
	, boolean create
	, long priority
	, Map<String,Object> properties) {
		this.placer = placer;
		this.label = label;
		this.staticPartValue = value;
		this.create = create;
		this.priority = priority;
		this.properties.putAll(properties);
	}

	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 */
	public StaticPartLoader(Function<StaticPartLoader<L>, CompletableFuture<Boolean>> placer) {
		this(placer,null,null,true,0,new HashMap<>());
	}
	
	/**
	 * Id.
	 *
	 * @return the part loader
	 */
	public StaticPartLoader<L> delete() {
		return new StaticPartLoader<>(placer, label, staticPartValue, false, priority, properties);
	}

	/**
	 * Label.
	 *
	 * @param l the l
	 * @return the part loader
	 */
	public StaticPartLoader<L> label(L l) {
		return new StaticPartLoader<>(placer, l, staticPartValue, create, priority, properties);
	}

	/**
	 * Priority.
	 *
	 * @param p the p
	 * @return the static part loader
	 */
	public StaticPartLoader<L> priority(long p) {
		return new StaticPartLoader<>(placer, label, staticPartValue, create, p, properties);
	}

	/**
	 * Value.
	 *
	 * @param v the v
	 * @return the part loader
	 */
	public StaticPartLoader<L> value(Object v) {
		return new StaticPartLoader<>(placer, label, v, true, priority, properties);
	}

	/**
	 * Gets the property.
	 *
	 * @param <X> the generic type
	 * @param key the key
	 * @param cls the cls
	 * @return the property
	 */
	public <X> X getProperty(String key, Class<X> cls) {
		return (X) properties.get(key);
	}

	/**
	 * Gets the all properties.
	 *
	 * @return the all properties
	 */
	public Map<String,Object> getAllProperties() {
		return Collections.unmodifiableMap(properties);
	}
	
	public StaticPartLoader<L> addProperty(String k, Object v) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.put(k, v);
		return new StaticPartLoader<>(placer, label, staticPartValue, create, priority, newMap);
	}
	
	public StaticPartLoader<L> addProperties(Map<String,Object> moreProperties) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.putAll(moreProperties);
		return new StaticPartLoader<>(placer, label, staticPartValue, create, priority, newMap);
	}
	/**
	 * Place.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> place() {
		return placer.apply(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "StaticPartLoader [" + (create ? "create ":"delete ") + "label=" + label + ", staticValue=" + staticPartValue + ", priority="+priority+"]";
	}
	
	/**
	 * By conveyor name.
	 *
	 * @param <L> the generic type
	 * @param name the name
	 * @return the static part loader
	 */
	public static <L> StaticPartLoader<L> byConveyorName(String name) {
		return Conveyor.byName(name).staticPart();
	}
	
	/**
	 * Lazy supplier.
	 *
	 * @param <L> the generic type
	 * @param name the name
	 * @return the supplier
	 */
	public static <L> Supplier<StaticPartLoader<L>> lazySupplier(String name) {
		return new Supplier<>() {
            StaticPartLoader<L> spl;

            @Override
            public StaticPartLoader<L> get() {
                if (spl == null) {
                    Conveyor<?, L, ?> c = Conveyor.byName(name);
                    if (c != null) {
                        spl = c.staticPart();
                    }
                }
                return spl;
            }
        };
	}

}
