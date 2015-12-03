package com.aegisql.conveyor;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.BuildingSite.Status;
import com.aegisql.conveyor.cart.ShoppingCart;

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

//	public <L> ShoppingCart<K,OUT,L> toShoppingCart( L label ) {
//		return new ShoppingCart<K, OUT, L>(key, product, label,remainingDelayMsec,TimeUnit.MILLISECONDS);
//	}
	
}
