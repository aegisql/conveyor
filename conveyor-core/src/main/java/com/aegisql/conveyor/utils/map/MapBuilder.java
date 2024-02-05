package com.aegisql.conveyor.utils.map;

import com.aegisql.conveyor.utils.CommonBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// TODO: Auto-generated Javadoc
/**
 * The Class MapBuilder.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class MapBuilder<K,V> extends CommonBuilder<Map<K,V>> {
	
	/** The map. */
	private final Map<K,V> map;
	
	/**
	 * Instantiates a new map builder.
	 *
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public MapBuilder(long ttl, TimeUnit timeUnit ) {
		super(ttl,timeUnit);
		this.map = new HashMap<>();
	}

	/**
	 * Instantiates a new map builder.
	 *
	 * @param expiration the expiration
	 */
	public MapBuilder(long expiration ) {
		super(expiration);
		this.map = new HashMap<>();
	}

	/**
	 * Instantiates a new map builder.
	 */
	public MapBuilder() {
		super();
		this.map = new HashMap<>();
	}

	/**
	 * Instantiates a new map builder.
	 *
	 * @param collection the collection
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public MapBuilder(Map<K,V> collection, long ttl, TimeUnit timeUnit ) {
		super(ttl,timeUnit);
		this.map = collection;
	}

	/**
	 * Instantiates a new map builder.
	 *
	 * @param collection the collection
	 * @param expiration the expiration
	 */
	public MapBuilder(Map<K,V> collection, long expiration ) {
		super(expiration);
		this.map = collection;
	}

	/**
	 * Instantiates a new map builder.
	 *
	 * @param collection the collection
	 */
	public MapBuilder(Map<K,V> collection) {
		super();
		this.map = collection;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public Map<K,V> get() {
		return map;
	}
	
	/**
	 * Adds the.
	 *
	 * @param key the key
	 * @param value the value
	 */
	public void add(K key, V value) {
		this.map.put(key,value);
	}

	/**
	 * Complete.
	 */
	public void complete() {
		this.setReady(true);
	}

}
