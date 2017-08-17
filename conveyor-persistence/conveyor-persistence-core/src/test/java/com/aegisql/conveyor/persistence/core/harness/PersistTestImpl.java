package com.aegisql.conveyor.persistence.core.harness;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;

public class PersistTestImpl implements Persistence<Integer> {

	ConcurrentHashMap<Long,byte[]> carts = new ConcurrentHashMap<>(); //emulation of serialization-deserialization stage
	ConcurrentHashMap<Integer,List<Long>> cartIds   = new ConcurrentHashMap<>();
	Set<Integer> completed = new HashSet<>();
	
	AtomicInteger idGen = new AtomicInteger(0);
	
	public PersistTestImpl() {
		
	}
	
	public PersistTestImpl(PersistTestImpl old, PersistTestImpl... more) {

		old.archiveCompleteKeys(old.getCompletedKeys());
		absorb(old);
		old.archiveAll();
		
		old.archiveKeys(this.cartIds.keySet());
		this.cartIds.forEach((id,list)->old.archiveParts(list));
		
		completed.addAll(old.completed);
		if(more != null) {
			for(PersistTestImpl p:more) {
				p.archiveCompleteKeys(p.getCompletedKeys());
				absorb(p);
				p.archiveAll();
			}
		}
	}
	

	@Override
	public long nextUniquePartId() {
		return idGen.incrementAndGet();
	}

	@Override
	public <L> void savePart(long id, Cart<Integer, ?, L> cart) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(cart);
			carts.put(id, bos.toByteArray());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void savePartId(Integer key, long cartId) {
		getAllPartIds(key).add(cartId);
	}

	@Override
	public void saveCompletedBuildKey(Integer key) {
		completed.add(key);
	}

	@Override
	public <L> Cart<Integer, ?, L> getPart(long id) {
		byte[] bytes = carts.get(id);
		if(bytes == null) {
			return null;
		}
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(bis);
			return (Cart<Integer, ?, L>) ois.readObject();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<Long> getAllPartIds(Integer key) {
		ArrayList<Long> newList = new ArrayList<>();
		
		List<Long> ids = cartIds.putIfAbsent(key, newList);
		if(ids == null) {
			ids = newList;
		}
		return ids;
	}

	@Override
	public void archiveParts(Collection<Long> ids) {
		ids.forEach(id->carts.remove(id));
	}

	@Override
	public void archiveKeys(Collection<Integer> key) {
		key.forEach(k->cartIds.remove(k));
	}

	@Override
	public void archiveCompleteKeys(Collection<Integer> keys) {
		completed.removeAll(keys);
		keys.forEach(key->{
			Collection<Long> ids = cartIds.remove(key);
			if(ids != null) {
				ids.forEach(id->carts.remove(id));
			}
		});
	}

	@Override
	public String toString() {
		return "Persistence [carts=" + carts + ", cartIds=" + cartIds + ", ack=" + completed + "]";
	}

	@Override
	public Collection<Cart<Integer, ?, ?>> getAllParts() {
		List<Cart<Integer, ?, ?>> cartsList = new ArrayList<>();
		carts.forEach((id,bytes)->{
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			ObjectInputStream ois;
			try {
				ois = new ObjectInputStream(bis);
				cartsList.add( (Cart<Integer, ?, ?>) ois.readObject() );
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		return cartsList;
	}

	public boolean isEmpty() {
		return carts.isEmpty() && cartIds.isEmpty() && completed.isEmpty();
	}

	@Override
	public Set<Integer> getCompletedKeys() {
		return new HashSet<Integer>(completed);
	}

	@Override
	public void archiveAll() {
		cartIds.clear();
		carts.clear();
		completed.clear();
	}

	@Override
	public <L> Collection<Cart<Integer, ?, L>> getAllStaticParts() {
		return new ArrayList<>();
	}

	@Override
	public Persistence<Integer> copy() {
		return this;
	}

	@Override
	public void close() throws IOException {
		
	}
	
}
