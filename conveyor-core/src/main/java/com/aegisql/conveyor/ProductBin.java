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
public final class ProductBin<K,OUT> {

	/** The key. */
	public final K key;
	
	/** The product. */
	public final OUT product;
	
	/** The remaining delay msec. */
	public final long remainingDelayMsec;
	
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
	 * @param remainingDelayMsec the remaining delay msec
	 * @param status the status
	 * @param properties the properties
	 * @param acknowledge the acknowledge
	 */
	public ProductBin(K key, OUT product, long remainingDelayMsec, Status status, Map<String,Object> properties, Acknowledge acknowledge) {
		super();
		this.key                = key;
		this.product            = product;
		this.remainingDelayMsec = remainingDelayMsec;
		this.status             = status;
		this.properties         = properties;
		this.acknowledge        = acknowledge;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ProductBin [key=" + key + ", product='" + product + (remainingDelayMsec == Long.MAX_VALUE ? "', unexpireable":"', remainingDelayMsec="+remainingDelayMsec)
				+ ", status=" + status + ", properties=" + properties + "]";
	}

}
