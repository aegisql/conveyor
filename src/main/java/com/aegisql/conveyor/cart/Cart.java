package com.aegisql.conveyor.cart;

import java.io.Serializable;

import com.aegisql.conveyor.Expireable;

public interface Cart <K,V,L> extends Expireable, Serializable {
	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public K getKey();
	
	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public V getValue();
	
	/**
	 * Gets the label.
	 *
	 * @return the label
	 */
	public L getLabel();
	
	/**
	 * Gets the creation time.
	 *
	 * @return the creation time
	 */
	public long getCreationTime();
	
	/**
	 * Gets the expiration time.
	 *
	 * @return the expiration time
	 */
	@Override
	public long getExpirationTime();
	
	/**
	 * Expired.
	 *
	 * @return true, if successful
	 */
	public boolean expired();
	
}
