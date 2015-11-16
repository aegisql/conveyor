/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class BuildingSite.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <L> the generic type
 * @param <C> the generic type
 * @param <OUT> the generic type
 */
public class BuildingSite <K, L, C extends Cart<K, ?, L>, OUT> implements Delayed {

	/**
	 * The Enum Status.
	 */
	public static enum Status{
		
		/** The waiting data. */
		WAITING_DATA,
		
		/** The timed out. */
		TIMED_OUT,
		
		/** The ready. */
		READY,
		
		/** The canceled. */
		CANCELED,
		
		/** The invalid. */
		INVALID;
	}
	
	/** The builder. */
	private final Supplier<OUT> builder;
	
	/** The value consumer. */
	private final LabeledValueConsumer<L, Object, Supplier<OUT>> valueConsumer;
	
	/** The readiness. */
	private final BiPredicate<Lot<K>, Supplier<OUT>> readiness;
	
	/** The initial cart. */
	private final  C initialCart;
	
	/** The accept count. */
	private int acceptCount = 0;
	
	/** The builder created. */
	private final long builderCreated;
	
	/** The builder expiration. */
	long builderExpiration;
	
	/** The status. */
	private Status status = Status.WAITING_DATA;
	
	/** The last error. */
	private Throwable lastError;
	
	/** The event history. */
	private final Map<L,AtomicInteger> eventHistory = new LinkedHashMap<>();
	
	/** The delay keeper. */
	Delayed delayKeeper;

	/**
	 * Instantiates a new building site.
	 *
	 * @param cart the cart
	 * @param builderSupplier the builder supplier
	 * @param cartConsumer the cart consumer
	 * @param ready the ready
	 * @param ttl the ttl
	 * @param unit the unit
	 */
	public BuildingSite( 
			C cart, 
			Supplier<Supplier<OUT>> builderSupplier, 
			LabeledValueConsumer<L, ?, Supplier<OUT>> cartConsumer, 
			BiPredicate<Lot<K>, Supplier<OUT>> ready, 
			long ttl, TimeUnit unit) {
		this.initialCart = cart;
		this.builder = builderSupplier.get() ;
		this.valueConsumer = (LabeledValueConsumer<L, Object, Supplier<OUT>>) cartConsumer;
		if(builder instanceof Predicate) {
			this.readiness = (lot,builder) -> {
				return ((Predicate<Lot<K>>)builder).test(lot);
			};
		} else {
			this.readiness = ready;
		}
		this.eventHistory.put(cart.getLabel(), new AtomicInteger(1));
		if(builder instanceof Delayed) {
			builderCreated = System.currentTimeMillis();
			builderExpiration = 0;
			delayKeeper = new Delayed() {
				@Override
				public int compareTo(Delayed o) {
					return ((Delayed)builder).compareTo( (Delayed) ((BuildingSite)o).builder );
				}
				@Override
				public long getDelay(TimeUnit unit) {
					return ((Delayed) builder).getDelay(unit);
				}
			};
		} else if ( cart.getExpirationTime() > 0 ) {
			builderCreated = cart.getCreationTime();
			builderExpiration = cart.getExpirationTime();
			delayKeeper = new Delayed() {
				@Override
				public int compareTo(Delayed o) {
					return cart.compareTo( ((BuildingSite)o).initialCart );
				}
				@Override
				public long getDelay(TimeUnit unit) {
					return cart.getDelay(unit);
				}
				
			};
		} else {
			builderCreated = System.currentTimeMillis();
			if(ttl == 0) {
				builderExpiration = 0;
			} else {
				builderExpiration = builderCreated + TimeUnit.MILLISECONDS.convert(ttl, unit);
			}
			delayKeeper = new Delayed() {
				@Override
				public int compareTo(Delayed o) {
					if( builderCreated < ((BuildingSite<?,?,?,?>)o).builderCreated) {
						return -1;
					}
					if( builderCreated > ((BuildingSite<?,?,?,?>)o).builderCreated) {
						return +1;
					}
					return 0;
				}

				@Override
				public long getDelay(TimeUnit unit) {
			        long delta;
					if( builderExpiration == 0 ) {
						delta = Long.MAX_VALUE;
					} else {
						delta = builderExpiration - System.currentTimeMillis();
					}
			        return unit.convert(delta, TimeUnit.MILLISECONDS);
				}
			};
		}
		
	}

	/**
	 * Accept.
	 *
	 * @param cart the cart
	 */
	public void accept(C cart) {
		L label = null;
		Object value = null;
		if(cart != null) {
			 label = cart.getLabel();
			 value = cart.getValue();
		}
		
		if( Status.TIMED_OUT.equals(value) || (label == null) || ! (label instanceof SmartLabel) ) {
			valueConsumer.accept(label, value, builder);
		} else {
			((SmartLabel)label).getSetter().accept(builder,value);
		}
		
		if(label != null) {
			acceptCount++;
			if(eventHistory.containsKey(label)) {
				eventHistory.get(label).incrementAndGet();
			} else {
				eventHistory.put(label, new AtomicInteger(1));
			}
		}
	}

	/**
	 * Builds the.
	 *
	 * @return the out
	 */
	public OUT build() {
		if( ! ready() ) {
			throw new IllegalStateException("Builder is not ready!");
		}
		return builder.get();
	}

	/**
	 * Ready.
	 *
	 * @return true, if successful
	 */
	public boolean ready() {
		Lot<K> lot = null;
		 lot = new Lot<>(
					initialCart.getKey(),
					builderCreated,
					builderExpiration,
					initialCart.getCreationTime(),
					initialCart.getExpirationTime(),
					acceptCount
					);
		boolean res = readiness.test(lot, builder);
		if( res ) {
			status = Status.READY;
		}
		return res;
	}

	/**
	 * Gets the accept count.
	 *
	 * @return the accept count
	 */
	public int getAcceptCount() {
		return acceptCount;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed o) {
		return delayKeeper.compareTo(o);
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		return delayKeeper.getDelay(unit);	}

	/**
	 * Expired.
	 *
	 * @return true, if successful
	 */
	public boolean expired() {
		return getDelay(TimeUnit.MILLISECONDS) < 0;
	}

	/**
	 * Gets the builder expiration.
	 *
	 * @return the builder expiration
	 */
	public long getBuilderExpiration() {
		return builderExpiration;
	}

	/**
	 * Gets the cart.
	 *
	 * @return the cart
	 */
	public C getCart() {
		return initialCart;
	}

	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public K getKey() {
		return initialCart.getKey();
	}
	
	/**
	 * Gets the last error.
	 *
	 * @return the last error
	 */
	public Throwable getLastError() {
		return lastError;
	}

	/**
	 * Sets the last error.
	 *
	 * @param lastError the new last error
	 */
	public void setLastError(Throwable lastError) {
		this.lastError = lastError;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BuildingSite [" + (builder != null ? "builder=" + builder + ", " : "")
				+ (initialCart != null ? "initialCart=" + initialCart + ", " : "") + "acceptCount=" + acceptCount
				+ ", builderCreated=" + builderCreated + ", builderExpiration=" + builderExpiration + ", "
				+ (status != null ? "status=" + status + ", " : "")
				+ (lastError != null ? "lastError=" + lastError + ", " : "")
				+ (eventHistory != null ? "eventHistory=" + eventHistory : "") + "]";
	}

	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Sets the status.
	 *
	 * @param status the new status
	 */
	public void setStatus(Status status) {
		this.status = status;
	}
	
}
