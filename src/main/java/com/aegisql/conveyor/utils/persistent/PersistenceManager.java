package com.aegisql.conveyor.utils.persistent;

public interface PersistenceManager <K> {
	public void saveCart();
	public void removeKey();
	public void getCart(K key);
}
