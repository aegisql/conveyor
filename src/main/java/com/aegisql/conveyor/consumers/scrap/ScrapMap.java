package com.aegisql.conveyor.consumers.scrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

public class ScrapMap <K> implements ConcurrentMap<K,List<?>>, Consumer<ScrapBin<K,?>> {

	private final ConcurrentMap<K,List<?>> inner;
	
	public ScrapMap() {
		this.inner = new ConcurrentHashMap<>();
	}

	public ScrapMap(ConcurrentMap<K,List<?>> map) {
		this.inner = map;
	}
	
	public ScrapMap(Supplier<ConcurrentMap<K,List<?>>> map) {
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
	public List<?> get(Object key) {
		return inner.get(key);
	}

	@Override
	public List<?> put(K key, List<?> value) {
		throw new RuntimeException("Method put is not available in this context");
	}

	@Override
	public List<?> remove(Object key) {
		throw new RuntimeException("Method remove is not available in this context");
	}

	@Override
	public void putAll(Map<? extends K, ? extends List<?>> m) {
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
	public Collection<List<?>> values() {
		return inner.values();
	}

	@Override
	public Set<java.util.Map.Entry<K, List<?>>> entrySet() {
		return inner.entrySet();
	}

	@Override
	public List<?> putIfAbsent(K key, List<?> value) {
		throw new RuntimeException("Method putIfAbsent is not available in this context");
	}

	@Override
	public boolean remove(Object key, Object value) {
		throw new RuntimeException("Method remove is not available in this context");
	}

	@Override
	public boolean replace(K key, List<?> oldValue, List<?> newValue) {
		throw new RuntimeException("Method replace is not available in this context");
	}

	@Override
	public List<?> replace(K key, List<?> value) {
		throw new RuntimeException("Method replace is not available in this context");
	}

	public ConcurrentMap<K,List<?>> unwrap() {
		return inner;
	}

	@Override
	public void accept(ScrapBin<K, ?> bin) {
		List<Object> scraps = (List<Object>) inner.putIfAbsent(bin.key, new ArrayList<>());
		scraps.add(bin.scrap);
	}

	public static <K> ScrapMap<K> of(Conveyor<K,?,?> conv) {
		return new ScrapMap<>();
	}

	public static <K> ScrapMap<K> of(Conveyor<K,?,?> conv, ConcurrentMap<K,List<?>> map) {
		return new ScrapMap<K>(map);
	}

	public static <K> ScrapMap<K> of(Conveyor<K,?,?> conv, Supplier<ConcurrentMap<K,List<?>>> mapS) {
		return new ScrapMap<>(mapS);
	}

}
