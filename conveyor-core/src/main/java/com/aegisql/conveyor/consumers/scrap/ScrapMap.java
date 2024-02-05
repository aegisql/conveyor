package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class ScrapMap.
 *
 * @param <K> the key type
 */
public class ScrapMap <K> implements ConcurrentMap<K,List<?>>, ScrapConsumer<K,Object> {

	/** The inner. */
	private final ConcurrentMap<K,List<?>> inner;
	
	/**
	 * Instantiates a new scrap map.
	 */
	public ScrapMap() {
		this.inner = new ConcurrentHashMap<>();
	}

	/**
	 * Instantiates a new scrap map.
	 *
	 * @param map the map
	 */
	public ScrapMap(ConcurrentMap<K,List<?>> map) {
		this.inner = map;
	}
	
	/**
	 * Instantiates a new scrap map.
	 *
	 * @param map the map
	 */
	public ScrapMap(Supplier<ConcurrentMap<K,List<?>>> map) {
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
	public List<?> get(Object key) {
		return inner.get(key);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public List<?> put(K key, List<?> value) {
		throw new RuntimeException("Method put is not available in this context");
	}

	/* (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	@Override
	public List<?> remove(Object key) {
		throw new RuntimeException("Method remove is not available in this context");
	}

	/* (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	@Override
	public void putAll(Map<? extends K, ? extends List<?>> m) {
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
	public Collection<List<?>> values() {
		return inner.values();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	@Override
	public Set<java.util.Map.Entry<K, List<?>>> entrySet() {
		return inner.entrySet();
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.ConcurrentMap#putIfAbsent(java.lang.Object, java.lang.Object)
	 */
	@Override
	public List<?> putIfAbsent(K key, List<?> value) {
		throw new RuntimeException("Method putIfAbsent is not available in this context");
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.ConcurrentMap#remove(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean remove(Object key, Object value) {
		throw new RuntimeException("Method remove is not available in this context");
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.ConcurrentMap#replace(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean replace(K key, List<?> oldValue, List<?> newValue) {
		throw new RuntimeException("Method replace is not available in this context");
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.ConcurrentMap#replace(java.lang.Object, java.lang.Object)
	 */
	@Override
	public List<?> replace(K key, List<?> value) {
		throw new RuntimeException("Method replace is not available in this context");
	}

	/**
	 * Unwrap.
	 *
	 * @return the concurrent map
	 */
	public ConcurrentMap<K,List<?>> unwrap() {
		return inner;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ScrapBin<K, Object> bin) {
		ArrayList<Object> newList = new ArrayList<>();
		List<Object> scraps = (List<Object>) inner.putIfAbsent(bin.key, newList);
		if(scraps == null) {
			scraps = newList;
		}
		scraps.add(bin.scrap);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @return the scrap map
	 */
	public static <K> ScrapMap<K> of(Conveyor<K,?,?> conv) {
		return new ScrapMap<>();
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param map the map
	 * @return the scrap map
	 */
	public static <K> ScrapMap<K> of(Conveyor<K,?,?> conv, ConcurrentMap<K,List<?>> map) {
		return new ScrapMap<>(map);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param mapS the map S
	 * @return the scrap map
	 */
	public static <K> ScrapMap<K> of(Conveyor<K,?,?> conv, Supplier<ConcurrentMap<K,List<?>>> mapS) {
		return new ScrapMap<>(mapS);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ScrapMap {" + inner + "}";
	}

}
