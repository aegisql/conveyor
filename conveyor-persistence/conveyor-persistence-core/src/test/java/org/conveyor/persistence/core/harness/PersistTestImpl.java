package org.conveyor.persistence.core.harness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.conveyor.persistence.core.Persist;

import com.aegisql.conveyor.cart.Cart;

public class PersistTestImpl implements Persist<Integer, Integer> {

	ConcurrentHashMap<Integer,Cart<Integer,?,?>> carts = new ConcurrentHashMap<>();
	ConcurrentHashMap<Integer,List<Integer>> cartIds   = new ConcurrentHashMap<>();
	Set<Integer> ack = new HashSet<>();
	
	AtomicInteger idGen = new AtomicInteger(0);
	

	@Override
	public Integer getUniqueId() {
		return idGen.incrementAndGet();
	}

	@Override
	public void saveCart(Integer id, Cart<Integer, ?, ?> cart) {
		carts.put(id, cart);
		saveCartId(cart.getKey(), id);
	}

	@Override
	public void saveCartId(Integer key, Integer cartId) {
		getAllCartIds(key).add(cartId);
	}

	@Override
	public void saveAcknowledge(Integer key) {
		ack.add(key);
	}

	@Override
	public Cart<Integer, ?, ?> getCart(Integer id) {
		return carts.get(id);
	}

	@Override
	public Collection<Integer> getAllCartIds(Integer key) {
		ArrayList<Integer> newList = new ArrayList<>();
		
		List<Integer> ids = cartIds.putIfAbsent(key, newList);
		if(ids == null) {
			ids = newList;
		}
		return ids;
	}

	@Override
	public void deleteCarts(Collection<Integer> ids) {
		ids.forEach(id->carts.remove(id));
	}

	@Override
	public void deleteKeys(Collection<Integer> key) {
		key.forEach(k->cartIds.remove(k));
	}

	@Override
	public void deleteAckKeys(Collection<Integer> keys) {
		ack.removeAll(keys);
	}

	@Override
	public String toString() {
		return "Persistence [carts=" + carts + ", cartIds=" + cartIds + ", ack=" + ack + "]";
	}

	
	
}
