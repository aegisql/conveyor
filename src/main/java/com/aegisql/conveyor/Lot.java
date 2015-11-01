package com.aegisql.conveyor;

public class Lot<K,V,L> {
	public final K key;
	public final V value;
	public final L label;

	public final long builderCreated;
	public final long builderExpiration;

	public final long cartCreated;
	public final long cartExpiration;

	public final int previouslyAccepted;
	
	public Lot(
			K k 
			,V v 
			,L label 
			,long builderCreated 
			,long builderExpiration
			,long cartCreated 
			,long cartExpiration
			,int previouslyAccepted
			) {
		this.key = k;
		this.value = v;
		this.label = label;
		this.builderCreated = builderCreated;
		this.builderExpiration = builderExpiration;
		this.cartCreated = cartCreated;
		this.cartExpiration = cartExpiration;
		this.previouslyAccepted = previouslyAccepted;
	}

}