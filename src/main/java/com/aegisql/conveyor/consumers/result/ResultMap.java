package com.aegisql.conveyor.consumers.result;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

public class ResultMap <K,V> implements Map<K,V>, Consumer<ProductBin<K,V>> {

	private final Map<K,V> inner;
	
	public ResultMap() {
		this.inner = new ConcurrentHashMap<>();
	}

	public ResultMap(Map<K,V> map) {
		this.inner = map;
	}
	
	public ResultMap(Supplier<Map<K,V>> map) {
		this.inner = map.get();
	}

	@Override
	public int size() {
		return inner.size();
	}

	@Override
	public boolean isEmpty() {
		return inner.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return inner.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return inner.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return inner.get(key);
	}

	@Override
	public V put(K key, V value) {
		throw new RuntimeException("Method put is not available in this context");
	}

	@Override
	public V remove(Object key) {
		throw new RuntimeException("Method remove is not available in this context");
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new RuntimeException("Method putAll is not available in this context");
	}

	@Override
	public void clear() {
		throw new RuntimeException("Method clear is not available in this context");
	}

	@Override
	public Set<K> keySet() {
		return inner.keySet();
	}

	@Override
	public Collection<V> values() {
		return inner.values();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return inner.entrySet();
	}
	
	public Map<K,V> unwrap() {
		return inner;
	}

	@Override
	public void accept(ProductBin<K, V> bin) {
		inner.put(bin.key, bin.product);
	}

	public static <K,V> ResultMap<K,V> of(Conveyor<K,?,V> conv) {
		return new ResultMap<>();
	}

	public static <K,V> ResultMap<K,V> of(Conveyor<K,?,V> conv, Map<K,V> map) {
		return new ResultMap<>(map);
	}

	public static <K,V> ResultMap<K,V> of(Conveyor<K,?,V> conv, Supplier<Map<K,V>> mapS) {
		return new ResultMap<>(mapS);
	}

}
