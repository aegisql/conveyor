package com.aegisql.conveyor;

import com.aegisql.conveyor.BuildingSite.Status;

public class ProductBin<K,OUT> {

	public final K key;
	public final OUT product;
	public final long remainingDelayMsec;
	public final Status status;
	
	
	public ProductBin(K key, OUT product, long remainingDelayMsec, Status status) {
		super();
		this.key = key;
		this.product = product;
		this.remainingDelayMsec = remainingDelayMsec;
		this.status = status;
	}


	@Override
	public String toString() {
		return "ProductBin [key=" + key + ", product=" + product + ", remainingDelayMsec=" + remainingDelayMsec
				+ ", status=" + status + "]";
	}

}
