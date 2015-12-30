/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuildingSite.Status;
import com.aegisql.conveyor.cart.Cart;

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
public class BuildingSite <K, L, C extends Cart<K, ?, L>, OUT> implements Expireable {

	private final static Logger LOG = LoggerFactory.getLogger(BuildingSite.class);
	/**
	 * The Enum Status.
	 */
	public static enum Status{
		WAITING_DATA,
		TIMED_OUT,
		READY,
		CANCELED,
		INVALID;
	}
	
	/** The builder. */
	final Supplier<? extends OUT> builder;
	
	/** The value consumer. */
	private final LabeledValueConsumer<L, Object, Supplier<? extends OUT>> valueConsumer;
	
	/** The readiness. */
	private final BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness;
	
	private Consumer<Supplier<? extends OUT>> timeoutAction;
	
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
	
	private final Lock lock;

	/**
	 * Instantiates a new building site.
	 *
	 * @param cart the cart
	 * @param builderSupplier the builder supplier
	 * @param cartConsumer the cart consumer
	 * @param readiness the ready
	 * @param ttl the ttl
	 * @param unit the unit
	 */
	public BuildingSite( 
			C cart, 
			Supplier<Supplier<? extends OUT>> builderSupplier, 
			LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer, 
			BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness, 
			Consumer<Supplier<? extends OUT>> timeoutAction,
			long ttl, TimeUnit unit, boolean synchronizeBuilder) {
		this.initialCart = cart;
		this.builder = builderSupplier.get() ;
		this.timeoutAction = timeoutAction;
		this.valueConsumer = (LabeledValueConsumer<L, Object, Supplier<? extends OUT>>) cartConsumer;
		if(synchronizeBuilder) {
			lock = new ReentrantLock();
		} else {
			lock = new Lock() {
				public void lock() {}
				public void lockInterruptibly() throws InterruptedException {}
				public boolean tryLock() {
					return true;
				}
				public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
					return true;
				}
				public void unlock() {}
				public Condition newCondition() {
					return null;
				}
			};
		}	
		if(builder instanceof TestingState) {
			this.readiness = (state,builder) -> {
				lock.lock();
				try {
					return ((TestingState<K,L>)builder).test(state);
				} finally {
					lock.unlock();
				}
			};
		}else if(builder instanceof Testing) {
			this.readiness = (state,builder) -> {
				lock.lock();
				try {
					return ((Testing)builder).test();
				} finally {
					lock.unlock();
				}
			};
		} else {
			this.readiness = readiness;
		}
		this.eventHistory.put(cart.getLabel(), new AtomicInteger(0));
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
		L label = cart.getLabel();
		Object value = cart.getValue();
		
			if( (label == null) || ! (label instanceof SmartLabel) ) {
				lock.lock();
				try {
					valueConsumer.accept(label, value, builder);
				} finally {
					lock.unlock();
				}
			} else {
				lock.lock();
				try {
					((SmartLabel)label).getSetter().accept(builder,value);
				} finally {
					lock.unlock();
				}
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

	public void timeout(C cart) {
			if (builder instanceof TimeoutAction ){
				lock.lock();
				try {
					((TimeoutAction)builder).onTimeout();
				} finally {
					lock.unlock();
				}
			} else if( timeoutAction != null ) {
				lock.lock();
				try {
					timeoutAction.accept(builder);
				} finally {
					lock.unlock();
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
		lock.lock();
		try {
			return builder.get();
		} finally {
			lock.unlock();
		}
	}
	
	private Supplier<? extends OUT> productSupplier = null;
	
	public Supplier<? extends OUT> getProductSupplier() {
		if(productSupplier == null ) {
			productSupplier = new Supplier<OUT>() {
				@Override
				public OUT get() {
					if( ! getStatus().equals(Status.WAITING_DATA)) {
						throw new IllegalStateException("Supplier is in a wrong state: " + getStatus());
					}
					OUT res = null;
					lock.lock();
					try {
						res = builder.get();
					} finally {
						lock.unlock();
					}
					return res;
				}
			};
		}
		return productSupplier;
	}

	/**
	 * Ready.
	 *
	 * @return true, if successful
	 */
	public boolean ready() {
		boolean res = false;
		final Map<L,Integer> history = new LinkedHashMap<>();
		eventHistory.forEach((k,v)->history.put(k, v.get()));
		State<K,L> state = new State<>(
				initialCart.getKey(),
				builderCreated,
				builderExpiration,
				initialCart.getCreationTime(),
				initialCart.getExpirationTime(),
				acceptCount,
				Collections.unmodifiableMap( history )
				);
		lock.lock();
		try {
			res = readiness.test(state, builder);
		} finally {
			lock.unlock();
		}
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
	@Override
	public long getExpirationTime() {
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
