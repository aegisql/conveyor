package com.aegisql.conveyor.utils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

// TODO: Auto-generated Javadoc
/**
 * The Class ChainResult.
 *
 * @param <K> the key type
 * @param <OUT1> the generic type
 * @param <L2> the generic type
 */
public class ChainResult<K,OUT1,L2> implements Consumer<ProductBin<K,OUT1>> {

	/** The next. */
	private final Conveyor<K,L2,?> next;
	
	/** The label. */
	private final L2 label;
	
	/** The ttl msec. */
	private final long ttlMsec;
	
	/** The use remaining. */
	private final boolean useRemaining;
	
	/**
	 * Instantiates a new chain result.
	 *
	 * @param next the next
	 * @param label the label
	 */
	public ChainResult(Conveyor<K, L2, ?> next,L2 label) {
		this.next         = next;
		this.label        = label;
		this.ttlMsec      = 0;
		this.useRemaining = true;
	}

	/**
	 * Instantiates a new chain result.
	 *
	 * @param next the next
	 * @param label the label
	 * @param ttl the ttl
	 * @param unit the unit
	 */
	public ChainResult(Conveyor<K, L2, ?> next,L2 label,long ttl, TimeUnit unit) {
		this.next         = next;
		this.label        = label;
		this.ttlMsec      = unit.toMillis(ttl);
		this.useRemaining = false;
	}

	/**
	 * Instantiates a new chain result.
	 *
	 * @param next the next
	 * @param label the label
	 * @param duration the duration
	 */
	public ChainResult(Conveyor<K, L2, ?> next,L2 label, Duration duration) {
		this.next         = next;
		this.label        = label;
		this.ttlMsec      = duration.toMillis();
		this.useRemaining = false;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K,OUT1> bin) {
		if(useRemaining) {
			next.add(bin.key,bin.product,label,bin.remainingDelayMsec,TimeUnit.MILLISECONDS);			
		} else {
			next.add(bin.key,bin.product,label,ttlMsec,TimeUnit.MILLISECONDS);
		}
	}

}
