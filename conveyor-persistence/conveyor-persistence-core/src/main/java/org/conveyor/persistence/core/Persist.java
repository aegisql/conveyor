package org.conveyor.persistence.core;

import java.util.Collection;

import com.aegisql.conveyor.cart.Cart;

public interface Persist <K> {

	public long getUniqueId();
	public void saveCart(long id,Cart<K,?,?> cart);
	public void saveCartId(K key, long cartId);
	public void saveAcknowledge(K key);
	public Cart<K,?,?> getCart(long id);
	
	public Collection<Long> getAllCartIds(K key);
	public Collection<Cart<K,?,?>> getAllCarts();
	
	
	public void archiveData(Collection<Long> ids);
	public void archiveKeys(Collection<K> keys);
	public void archiveAckKeys(Collection<K> keys);
	
}
