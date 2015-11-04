package com.aegisql.conveyor;

public class Lot<K> {
	public final K key;

	public final long builderCreated;
	public final long builderExpiration;

	public final long cartCreated;
	public final long cartExpiration;

	public final int previouslyAccepted;
	
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