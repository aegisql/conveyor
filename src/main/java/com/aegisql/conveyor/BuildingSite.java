/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class BuildingSite <K, L, C extends Cart<K, ?, L>, OUT> implements Expireable, Delayed {

	private final static Logger LOG = LoggerFactory.getLogger(BuildingSite.class);
	
	public final static Lock NON_LOCKING_LOCK = new Lock() {
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
	private final Supplier<? extends OUT> builder;
	
	/** The value consumer. */
	private final LabeledValueConsumer<L, Object, Supplier<? extends OUT>> valueConsumer;
	
	/** The readiness. */
	private final BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness;
	
	private Consumer<Supplier<? extends OUT>> timeoutAction;
	
	private boolean saveCarts      = false;

	private final List<C> allCarts = new ArrayList<>();
	
	/** The initial cart. */
	private final  C initialCart;

	private  C lastCart = null;

	/** The accept count. */
	private int acceptCount = 0;
	
	/** The builder created. */
	private long builderCreated;
	
	/** The builder expiration. */
	long builderExpiration = 0;
	
	/** The status. */
	private Status status = Status.WAITING_DATA;
	
	/** The last error. */
	private Throwable lastError;
	
	/** The event history. */
	private final Map<L,AtomicInteger> eventHistory = new LinkedHashMap<>();
	
	/** The delay keeper. */
	Delayed delayKeeper;
	
	private final Lock lock;
	
	private final boolean postponeExpirationEnabled;

	private final long addExpirationTimeMsec;
	
	private BiConsumer<BuildingSite <K, L, C, OUT>,C> postponeAlg = (bs,cart)->{/*do nothing*/};
	
	/**
	 * Instantiates a new building site.
	 *
	 * @param cart the cart
	 * @param builderSupplier the builder supplier
	 * @param cartConsumer the cart consumer
	 * @param readiness the ready
	 * @param timeoutAction the timeoutAction
	 * @param ttl the ttl
	 * @param unit the unit
	 * @param synchronizeBuilder the synchronizeBuilder
	 */
	@SuppressWarnings("unchecked")
	public BuildingSite( 
			C cart, 
			Supplier<Supplier<? extends OUT>> builderSupplier, 
			LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer, 
			BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness, 
			Consumer<Supplier<? extends OUT>> timeoutAction,
			long ttl, TimeUnit unit, boolean synchronizeBuilder, 
			boolean saveCarts, boolean postponeExpirationEnabled, long addExpirationTimeMsec) {
		this.initialCart               = cart;
		this.lastCart                  = cart;
		this.builder                   = builderSupplier.get() ;
		this.timeoutAction             = timeoutAction;
		this.saveCarts                 = saveCarts;
		this.postponeExpirationEnabled = postponeExpirationEnabled;
		this.addExpirationTimeMsec     = addExpirationTimeMsec;
		this.valueConsumer             = (LabeledValueConsumer<L, Object, Supplier<? extends OUT>>) cartConsumer;
		if(synchronizeBuilder) {
			this.lock = new ReentrantLock();
		} else {
			this.lock = BuildingSite.NON_LOCKING_LOCK;
		}
		if(readiness != null) {
			this.readiness = readiness;			
		} else {
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
				this.readiness = (l,b) -> {
					throw new IllegalStateException("Readiness Evaluator is not set");
				};
			}			
		}
		this.eventHistory.put(cart.getLabel(), new AtomicInteger(0));
		if(builder instanceof Expireable) {
			Expireable expireable = (Expireable)builder;
			builderCreated    = System.currentTimeMillis();
			builderExpiration = expireable.getExpirationTime();
			delayKeeper       = Expireable.toDelayed((Expireable)builder);
			postponeAlg       = (bs,c) -> {
				Expireable ex = (Expireable)bs.builder;
				bs.builderExpiration = ex.getExpirationTime(); //let builder decide. highest priority
			};			
		} else {
			postponeAlg = (bs,c) -> {
				if( c.getExpirationTime() > 0 ) {
					bs.builderExpiration = Math.max(this.builderExpiration, c.getExpirationTime()); // keep longest TTL
				} else if( builderExpiration > 0) {
					bs.builderExpiration += bs.addExpirationTimeMsec; //just add some time, if expireable, lowest priority
				}
			};			
		}
		if( cart.getExpirationTime() > 0 && builderExpiration == 0) {
			builderCreated    = cart.getCreationTime();
			builderExpiration = cart.getExpirationTime();
			delayKeeper       = Expireable.toDelayed(cart);
		} 
		if(builderExpiration == 0) {
			builderCreated = System.currentTimeMillis();
			if(ttl == 0) {
				builderExpiration = 0;
			} else {
				builderExpiration = builderCreated + TimeUnit.MILLISECONDS.convert(ttl, unit);
			}
			delayKeeper = Expireable.toDelayed(this);
		}
	}

	/**
	 * Accept the data.
	 *
	 * @param cart the cart
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void accept(C cart) {
		this.lastCart = cart;
		if( saveCarts) {
			allCarts.add(cart);
		}
		L label = cart.getLabel();
		Object value = cart.getValue();

		if ((label == null) || !(label instanceof SmartLabel)) {
			lock.lock();
			try {
				valueConsumer.accept(label, value, builder);
			} finally {
				lock.unlock();
			}
		} else {
			lock.lock();
			try {
				((SmartLabel) label).get().accept(builder, value);
			} finally {
				lock.unlock();
			}
		}
		acceptCount++;
		if(label != null) {
			if(eventHistory.containsKey(label)) {
				eventHistory.get(label).incrementAndGet();
			} else {
				eventHistory.put(label, new AtomicInteger(1));
			}
		}
		// this itself does not affect expiration time
		// it should be enabled
		// enabling may affect performance
		if(postponeExpirationEnabled) {
			postponeAlg.accept(this, cart);
		}
	}

	public void timeout(C cart) {
		this.lastCart = cart;
		if( timeoutAction != null ) {
			lock.lock();
			try {
				timeoutAction.accept(builder);
			} finally {
				lock.unlock();
			}
		} else if (builder instanceof TimeoutAction ){ 
			lock.lock();
			try {
				((TimeoutAction)builder).onTimeout();
			} finally {
				lock.unlock();
			}
		} 
	}

	/**
	 * Builds the Product when it is ready.
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

	/**
	 * Try to build the product without checking for it's readiness condition.
	 * This method should not be used to produce actual results, but it can be useful
	 * when processing the ScrapBin - the only place where this interface is accessible.
	 *
	 * @return the out
	 */
	public OUT unsafeBuild() {
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
				Collections.unmodifiableMap( history ),
				Collections.unmodifiableList(allCarts)
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
	public C getCreatingCart() {
		return initialCart;
	}

	public List<C> getAcceptedCarts() {
		return Collections.unmodifiableList( allCarts );
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

	public C getLastCart() {
		return lastCart;
	}

	public void setLastCart(C lastCart) {
		this.lastCart = lastCart;
	}
	
	/**
	 * package access methods
	 * */
	public Supplier<? extends OUT> getBuilder() {
		return builder;
	}
	
	public Lock getLock() {
		return lock;
	}

	void updateExpirationTime(long expirationTime) {
		this.builderExpiration = expirationTime;
	}
	
}
