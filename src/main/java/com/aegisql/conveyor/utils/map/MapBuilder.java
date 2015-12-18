package com.aegisql.conveyor.utils.map;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.utils.CommonBuilder;

public class MapBuilder<K,V> extends CommonBuilder<Map<K,V>> {
	
	private final Map<K,V> map;
	
	public MapBuilder(long ttl, TimeUnit timeUnit ) {
		super(ttl,timeUnit);
		this.map = new HashMap<>();
	}

	public MapBuilder(long expiration ) {
		super(expiration);
		this.map = new HashMap<>();
	}

	public MapBuilder() {
		super();
		this.map = new HashMap<>();
	}

	public MapBuilder(Map<K,V> collection, long ttl, TimeUnit timeUnit ) {
		super(ttl,timeUnit);
		this.map = collection;
	}

	public MapBuilder(Map<K,V> collection, long expiration ) {
		super(expiration);
		this.map = collection;
	}

	public MapBuilder(Map<K,V> collection) {
		super();
		this.map = collection;
	}

	@Override
	public Map<K,V> get() {
		return map;
	}
	
	public void add(K key, V value) {
		this.map.put(key,value);
	}

	public void complete() {
		this.setReady(true);
	}

}
