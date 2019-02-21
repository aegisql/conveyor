package com.aegisql.conveyor.parallel;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import com.aegisql.conveyor.Conveyor;

public class PropertyTester<K,L,OUT> implements Predicate<Map<String,Object>> {

	private final Conveyor<K,L,OUT> conveyor;
	
	private final Map<String,Predicate<Object>> testers = new HashMap<>();
	
	public PropertyTester(Conveyor<K,L,OUT> conveyor) {
		this.conveyor = conveyor;
	}

	@Override
	public boolean test(Map<String, Object> properties) {
		
		boolean res = testers.size() > 0;
		
		for(Entry<String,Predicate<Object>> entry: testers.entrySet()) {
			if(properties.containsKey(entry.getKey())) {
				res &= entry.getValue().test(properties.get(entry.getKey()));
			} else {
				res = false;
				break;
			}
		}
		return res;
	}

	public PropertyTester<K,L,OUT>  addTestingPredicate(String key, Predicate<Object> p) {
		testers.put(key, (Predicate<Object>) p);
		return this;
	}

	public PropertyTester<K,L,OUT>  expectsValue(String key, Object other) {
		return addTestingPredicate(key, val->val.equals(other));
	}

	public Conveyor<K, L, OUT> getConveyor() {
		return conveyor;
	}
	
}
