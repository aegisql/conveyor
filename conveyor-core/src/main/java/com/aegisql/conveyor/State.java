/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

// TODO: Auto-generated Javadoc
/**
 * The Class State.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <L> the generic type
 */
public final class State<K,L> implements Serializable {
	
	/** The Constant serialVersionUID. */
	@Serial
    private static final long serialVersionUID = 1L;

	/** The key. */
	public final K key;

	/** The builder created. */
	public final long builderCreated;
	
	/** The builder expiration. */
	public final long builderExpiration;

	/** The cart created. */
	public final long cartCreated;
	
	/** The cart expiration. */
	public final long cartExpiration;

	/** The previously accepted. */
	public final int previouslyAccepted;
	
	/** The event history. */
	public final Map<L,Integer> eventHistory;
	
	/** The carts. */
	public final List<Cart<K,?,L>> carts;
	
	/**
	 * Instantiates a new building state.
	 *
	 * @param k the k
	 * @param builderCreated the builder created
	 * @param builderExpiration the builder expiration
	 * @param cartCreated the cart created
	 * @param cartExpiration the cart expiration
	 * @param previouslyAccepted the previously accepted
	 * @param eventHistory the eventHistory Map
	 * @param carts the carts
	 */
	public State(
			K k 
			,long builderCreated 
			,long builderExpiration
			,long cartCreated 
			,long cartExpiration
			,int previouslyAccepted
			,Map<L,Integer> eventHistory
			,List<Cart<K,?,L>> carts
			) {
		this.key                = k;
		this.builderCreated     = builderCreated;
		this.builderExpiration  = builderExpiration;
		this.cartCreated        = cartCreated;
		this.cartExpiration     = cartExpiration;
		this.previouslyAccepted = previouslyAccepted;
		this.eventHistory       = eventHistory;
		this.carts              = carts;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "State [" + (key != null ? "key=" + key + ", " : "") + "builderCreated=" + builderCreated
				+ ", builderExpiration=" + builderExpiration + ", cartCreated=" + cartCreated + ", cartExpiration="
				+ cartExpiration + ", previouslyAccepted=" + previouslyAccepted + ", "
				+ (eventHistory != null ? "eventHistory=" + eventHistory : "") + "]";
	}
	
}