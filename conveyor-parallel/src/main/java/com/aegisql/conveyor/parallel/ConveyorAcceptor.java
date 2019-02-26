package com.aegisql.conveyor.parallel;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ConveyorAcceptor<K,L,OUT> implements Predicate<Map<String,Object>> {

	private final Conveyor<K,L,OUT> conveyor;
	
	private final Map<String,Predicate<Object>> testers = new HashMap<>();
	
	public ConveyorAcceptor(Conveyor<K,L,OUT> conveyor) {
		this.conveyor = conveyor;
	}

	public ConveyorAcceptor(Supplier<Conveyor<K,L,OUT>> supplier) {
		this.conveyor = supplier.get();
	}

	public ConveyorAcceptor() {
		this(AssemblingConveyor::new);
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

	public ConveyorAcceptor<K,L,OUT> addTestingPredicate(String key, Predicate<Object> p) {
		testers.put(key, (Predicate<Object>) p);
		return this;
	}

	public ConveyorAcceptor<K,L,OUT> expectsValue(String key, Object other) {
		return addTestingPredicate(key, val->val.equals(other));
	}

	public Conveyor<K, L, OUT> getConveyor() {
		return conveyor;
	}

	public Set<String> getPropertyNames() {
		return testers.keySet();
	}
	
}
