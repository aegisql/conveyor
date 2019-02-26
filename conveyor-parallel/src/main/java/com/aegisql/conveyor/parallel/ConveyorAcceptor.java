package com.aegisql.conveyor.parallel;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The type Conveyor acceptor.
 *
 * @param <K>   the type parameter
 * @param <L>   the type parameter
 * @param <OUT> the type parameter
 */
public class ConveyorAcceptor<K,L,OUT> implements Predicate<Map<String,Object>> {

	/**
	 * The Conveyor.
	 */
	final Conveyor<K,L,OUT> conveyor;

	/**
	 * The Testers.
	 */
	final Map<String,Predicate<Object>> testers = new HashMap<>();

	/**
	 * Instantiates a new Conveyor acceptor.
	 *
	 * @param conveyor the conveyor
	 * @param testers  the testers
	 */
	public ConveyorAcceptor(Conveyor<K,L,OUT> conveyor, Map<String,Predicate<Object>> testers) {
		this.conveyor = conveyor;
		this.testers.putAll(testers);
	}

	/**
	 * Instantiates a new Conveyor acceptor.
	 *
	 * @param conveyor the conveyor
	 */
	public ConveyorAcceptor(Conveyor<K,L,OUT> conveyor) {
		this.conveyor = conveyor;
	}

	/**
	 * Instantiates a new Conveyor acceptor.
	 *
	 * @param supplier the supplier
	 */
	public ConveyorAcceptor(Supplier<Conveyor<K,L,OUT>> supplier) {
		this.conveyor = supplier.get();
	}

	/**
	 * Instantiates a new Conveyor acceptor.
	 */
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

	/**
	 * Add testing predicate conveyor acceptor.
	 *
	 * @param key the key
	 * @param p   the p
	 * @return the conveyor acceptor
	 */
	public ConveyorAcceptor<K,L,OUT> addTestingPredicate(String key, Predicate<Object> p) {
		testers.put(key, (Predicate<Object>) p);
		return this;
	}

	/**
	 * Expects value conveyor acceptor.
	 *
	 * @param key   the key
	 * @param other the other
	 * @return the conveyor acceptor
	 */
	public ConveyorAcceptor<K,L,OUT> expectsValue(String key, Object other) {
		return addTestingPredicate(key, val->val.equals(other));
	}

	/**
	 * Gets conveyor.
	 *
	 * @return the conveyor
	 */
	public Conveyor<K, L, OUT> getConveyor() {
		return conveyor;
	}

	/**
	 * Gets property names.
	 *
	 * @return the property names
	 */
	public Set<String> getPropertyNames() {
		return testers.keySet();
	}
	
}
