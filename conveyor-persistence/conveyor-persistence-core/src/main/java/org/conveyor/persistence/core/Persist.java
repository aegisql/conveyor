package org.conveyor.persistence.core;

import java.util.Collection;

import com.aegisql.conveyor.cart.Cart;

public interface Persist <K,I> {

	public I getUniqueId();
	public void saveCart(I id,Cart<K,?,?> cart);
	public void saveCartId(K key, I cartId);
	public void saveAcknowledge(K key);
	public Cart<K,?,?> getCart(I id);
	
	public Collection<I> getAllCartIds(K key);
	public Collection<Cart<K,?,?>> getAllCarts();
	
	
	public void archiveData(Collection<I> ids);
	public void archiveKeys(Collection<K> keys);
	public void archiveAckKeys(Collection<K> keys);
	
}
