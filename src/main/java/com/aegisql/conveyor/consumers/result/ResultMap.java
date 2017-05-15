package com.aegisql.conveyor.consumers.result;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

// TODO: Auto-generated Javadoc
/**
 * The Class ResultMap.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class ResultMap <K,V> implements Map<K,V>, ResultConsumer<K,V> {

	/** The inner. */
	private final Map<K,V> inner;
	
	/**
	 * Instantiates a new result map.
	 */
	public ResultMap() {
		this.inner = new ConcurrentHashMap<>();
	}

	/**
	 * Instantiates a new result map.
	 *
	 * @param map the map
	 */
	public ResultMap(Map<K,V> map) {
		this.inner = map;
	}
	
	/**
	 * Instantiates a new result map.
	 *
	 * @param map the map
	 */
	public ResultMap(Supplier<Map<K,V>> map) {
		this.inner = map.get();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	@Override
	public int size() {
		return inner.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return inner.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object key) {
		return inner.containsKey(key);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		return inner.containsValue(value);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	@Override
	public V get(Object key) {
		return inner.get(key);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public V put(K key, V value) {
		throw new RuntimeException("Method put is not available in this context");
	}

	/* (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	@Override
	public V remove(Object key) {
		throw new RuntimeException("Method remove is not available in this context");
	}

	/* (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new RuntimeException("Method putAll is not available in this context");
	}

	/* (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	@Override
	public void clear() {
		throw new RuntimeException("Method clear is not available in this context");
	}

	/* (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	@Override
	public Set<K> keySet() {
		return inner.keySet();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	@Override
	public Collection<V> values() {
		return inner.values();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return inner.entrySet();
	}
	
	/**
	 * Unwrap.
	 *
	 * @return the map
	 */
	public Map<K,V> unwrap() {
		return inner;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K, V> bin) {
		inner.put(bin.key, bin.product);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @return the result map
	 */
	public static <K,V> ResultMap<K,V> of(Conveyor<K,?,V> conv) {
		return new ResultMap<>();
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param map the map
	 * @return the result map
	 */
	public static <K,V> ResultMap<K,V> of(Conveyor<K,?,V> conv, Map<K,V> map) {
		return new ResultMap<>(map);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param mapS the map S
	 * @return the result map
	 */
	public static <K,V> ResultMap<K,V> of(Conveyor<K,?,V> conv, Supplier<Map<K,V>> mapS) {
		return new ResultMap<>(mapS);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ResultMap {" + inner + "}";
	}

}
