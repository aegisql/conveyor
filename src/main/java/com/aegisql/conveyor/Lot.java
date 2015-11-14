/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class Lot.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 */
public class Lot<K> {
	
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
	
	/**
	 * Instantiates a new lot.
	 *
	 * @param k the k
	 * @param builderCreated the builder created
	 * @param builderExpiration the builder expiration
	 * @param cartCreated the cart created
	 * @param cartExpiration the cart expiration
	 * @param previouslyAccepted the previously accepted
	 */
	public Lot(
			K k 
			,long builderCreated 
			,long builderExpiration
			,long cartCreated 
			,long cartExpiration
			,int previouslyAccepted
			) {
		this.key = k;
		this.builderCreated = builderCreated;
		this.builderExpiration = builderExpiration;
		this.cartCreated = cartCreated;
		this.cartExpiration = cartExpiration;
		this.previouslyAccepted = previouslyAccepted;
	}

}