/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.Map;

// TODO: Auto-generated Javadoc
/**
 * The Class ProductBin.
 *
 * @param <K> the key type
 * @param <OUT> the generic type
 */
public final class ProductBin<K,OUT> extends AbstractBin<K,Object,OUT>{

	/** The product. */
	public final OUT product;
	
	/** The remaining delay msec. */
	public final long expirationTime;
	
	/** The status. */
	public final Status status;
	
	/** The properties. */
	public final Map<String,Object> properties;
	
	/** The acknowledge. */
	public final Acknowledge acknowledge;
	
	/**
	 * Instantiates a new product bin.
	 *
	 * @param key the key
	 * @param product the product
	 * @param expirationTime the remaining delay msec
	 * @param status the status
	 * @param properties the properties
	 * @param acknowledge the acknowledge
	 */
	public ProductBin(Conveyor<K, Object, OUT> conveyor, K key, OUT product, long expirationTime, Status status, Map<String, Object> properties, Acknowledge acknowledge) {
		super(conveyor,key);
		this.product        = product;
		this.expirationTime = expirationTime;
		this.status         = status;
		this.properties     = properties;
		this.acknowledge    = acknowledge;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ProductBin [conveyor= "+(conveyor==null?"N/A":conveyor.getName())+" key=" + key + ", product='" + product + (expirationTime == 0 ? "', unexpireable":"', expirationTime="+ expirationTime)
				+ ", status=" + status + ", properties=" + properties + "]";
	}

}
