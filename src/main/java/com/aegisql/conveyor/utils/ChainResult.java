package com.aegisql.conveyor.utils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

public class ChainResult<K,OUT1,L2> implements Consumer<ProductBin<K,OUT1>> {

	private final Conveyor<K,L2,?> next;
	private final L2 label;
	
	private long ttlMsec;
	private boolean useRemaining = false;
	
	public ChainResult(Conveyor<K, L2, ?> next,L2 label) {
		this.next         = next;
		this.label        = label;
		this.useRemaining = true;
	}

	public ChainResult(Conveyor<K, L2, ?> next,L2 label,long ttl, TimeUnit unit) {
		this.next    = next;
		this.label   = label;
		this.ttlMsec = unit.toMillis(ttl);
	}

	public ChainResult(Conveyor<K, L2, ?> next,L2 label, Duration duration) {
		this.next    = next;
		this.label   = label;
		this.ttlMsec = duration.toMillis();
	}

	@Override
	public void accept(ProductBin<K,OUT1> bin) {
		if(useRemaining) {
			next.add(bin.key,bin.product,label,bin.remainingDelayMsec,TimeUnit.MILLISECONDS);			
		} else {
			next.add(bin.key,bin.product,label,ttlMsec,TimeUnit.MILLISECONDS);
		}
	}

}
