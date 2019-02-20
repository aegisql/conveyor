package com.aegisql.conveyor.parallel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.aegisql.conveyor.Conveyor;

public class CartPropertyTester<K,L,OUT> implements Predicate<Map<String,Object>> {

	private final Conveyor<K,L,OUT> conveyor;
	
	private final Map<String,Predicate<Object>> testers = new HashMap<>();
	
	public CartPropertyTester(Conveyor<K,L,OUT> conveyor) {
		this.conveyor = conveyor;
	}

	@Override
	public boolean test(Map<String, Object> properties) {
		
		boolean res = testers.size() > 0;
		
		for(String key: testers.keySet()) {
			if(properties.containsKey(key)) {
				res &= testers.get(key).test(properties.get(key));
			} else {
				res &= false;
				break;
			}
		}
		return res;
	}

	public CartPropertyTester<K,L,OUT>  addKeyPredicate(String key, Predicate<Object> p) {
		testers.put(key, (Predicate<Object>) p);
		return this;
	}

	public Conveyor<K, L, OUT> getConveyor() {
		return conveyor;
	}
	
}
