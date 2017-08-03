package com.aegisql.conveyor.persistence.core;

import java.util.Collection;
import java.util.Set;

import com.aegisql.conveyor.cart.Cart;

public interface Persistence <K> {

	public long nextUniquePartId();
	public <L> void savePart(long id,Cart<K,?,L> cart);
	public void savePartId(K key, long partId);
	public void saveCompletedBuildKey(K key);
	public <L> Cart<K,?,L> getPart(long id);
	
	public Collection<Long> getAllPartIds(K key);
	public <L> Collection<Cart<K,?,L>> getAllParts();
	
	public Set<K> getCompletedKeys();
	
	//batch operations
	public void archiveParts(Collection<Long> ids);
	public void archiveKeys(Collection<K> keys);
	public void archiveCompleteKeys(Collection<K> keys);
	
	public void archiveAll();
	
	default <L> void absorb(Persistence<K> old) {
		Set<K> completed                 = old.getCompletedKeys();
		Collection<Cart<K,?,L>> oldParts = old.getAllParts();
		oldParts.forEach(cart->{
			K key = cart.getKey();
			if( ! completed.contains(key) ) {
				long nextId = nextUniquePartId();
				cart.addProperty("CART_ID", nextId);
				Integer recoveryAttempt = cart.getProperty("RECOVERY_ATTEMPT", Integer.class);
				if(recoveryAttempt == null) {
					cart.addProperty("RECOVERY_ATTEMPT", 1);
				} else {
					cart.addProperty("RECOVERY_ATTEMPT", recoveryAttempt+1);
				}
				savePart(nextId, cart);
				savePartId(key,nextId);
			}
		});
	}
	
}
