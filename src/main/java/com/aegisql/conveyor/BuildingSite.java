/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
public class BuildingSite <K, L, C extends Cart<K, ?, L>, OUT> implements Expireable {

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(BuildingSite.class);
	
	/** The Constant NON_LOCKING_LOCK. */
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
	private final Supplier<? extends OUT> builder;
	
	/** The value consumer. */
	private final LabeledValueConsumer<L, Object, Supplier<? extends OUT>> defaultValueConsumer;

	/** The value consumer. */
	private LabeledValueConsumer<L, Object, Supplier<? extends OUT>> valueConsumer = null;

	/** The readiness. */
	private final BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness;
	
	/** The default timeout action. */
	private final Consumer<Supplier<? extends OUT>> defaultTimeoutAction;

	/** The timeout action. */
	private final Consumer<Supplier<? extends OUT>> timeoutAction;

	/** The save carts. */
	private boolean saveCarts      = false;

	/** The all carts. */
	private final List<C> allCarts = new ArrayList<>();
	
	/** The futures. */
	private final List<CompletableFuture<? extends OUT>> futures = new ArrayList<>();
	
	/** The initial cart. */
	private final  C initialCart;

	/** The last cart. */
	private  C lastCart = null;

	/** The accept count. */
	private int acceptCount = 0;
	
	/** The builder created. */
	private long builderCreated;
	
	/** The builder expiration. */
	
	Expireable expireableSource = () -> 0;
	
	/** The status. */
	private Status status = Status.WAITING_DATA;
	
	/** The last error. */
	private Throwable lastError;
	
	/** The event history. */
	private final Map<L,AtomicInteger> eventHistory = new LinkedHashMap<>();
	
	/** The delay keeper. */
	//Delayed delayKeeper;
	
	/** The lock. */
	private final Lock lock;
	
	/** The postpone expiration enabled. */
	private final boolean postponeExpirationEnabled;
	
	/** The postpone expiration on timeout enabled. */
	private final boolean postponeExpirationOnTimeoutEnabled;

	/** The add expiration time msec. */
	private final long addExpirationTimeMsec;
	
	/** The postpone alg. */
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
	 * @param saveCarts the save carts
	 * @param postponeExpirationEnabled the postpone expiration enabled
	 * @param addExpirationTimeMsec the add expiration time msec
	 * @param postponeExpirationOnTimeoutEnabled the postpone expiration on timeout enabled
	 */
	@SuppressWarnings("unchecked")
	public BuildingSite( 
			C cart, 
			Supplier<Supplier<? extends OUT>> builderSupplier, 
			LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer, 
			BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness, 
			Consumer<Supplier<? extends OUT>> timeoutAction,
			long ttl, TimeUnit unit, boolean synchronizeBuilder, 
			boolean saveCarts, boolean postponeExpirationEnabled, long addExpirationTimeMsec, boolean postponeExpirationOnTimeoutEnabled) {
		this.initialCart               = cart;
		this.lastCart                  = cart;
		this.defaultTimeoutAction      = timeoutAction;
		this.saveCarts                 = saveCarts;
		this.postponeExpirationEnabled = postponeExpirationEnabled;
		this.postponeExpirationOnTimeoutEnabled = postponeExpirationOnTimeoutEnabled;
		this.addExpirationTimeMsec     = addExpirationTimeMsec;
		this.defaultValueConsumer             = (LabeledValueConsumer<L, Object, Supplier<? extends OUT>>) cartConsumer;

		Supplier<? extends OUT> productSupplier = builderSupplier.get();
		if( productSupplier instanceof ProductSupplier && ((ProductSupplier)productSupplier).getSupplier() != null) {
			this.builder = ((ProductSupplier)productSupplier).getSupplier();
		} else {
			this.builder = productSupplier;
		}
		
		if(synchronizeBuilder) {
			this.lock = new ReentrantLock();
		} else {
			this.lock = BuildingSite.NON_LOCKING_LOCK;
		}
		if (productSupplier instanceof TimeoutAction) {
			this.timeoutAction = b -> {
				((TimeoutAction) productSupplier).onTimeout();
			};
		} else if (defaultTimeoutAction != null) {
			this.timeoutAction = defaultTimeoutAction;
		} else {
			this.timeoutAction = null;
		}

		if(readiness != null) {
			this.readiness = readiness;			
		} else {
			if(productSupplier instanceof TestingState) {
				this.readiness = (state,b) -> {
					lock.lock();
					try {
						return ((TestingState<K,L>)productSupplier).test(state);
					} finally {
						lock.unlock();
					}
				};
			}else if(productSupplier instanceof Testing) {
				this.readiness = (state,b) -> {
					lock.lock();
					try {
						return ((Testing)productSupplier).test();
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
		if(productSupplier instanceof Expireable) {
			Expireable expireable = (Expireable)productSupplier;
			builderCreated    = System.currentTimeMillis();
			expireableSource  = expireable;
		} else {
			if(postponeExpirationEnabled) {
				postponeAlg = (bs,c) -> {
					if( c != null && c.getExpirationTime() > 0 ) {
						bs.expireableSource = () -> c.getExpirationTime();
					} else if( expireableSource.getExpirationTime() > 0) {
						bs.expireableSource = ()->System.currentTimeMillis() + bs.addExpirationTimeMsec; //just add some time, if expireable, lowest priority
					}
				};			
			}
		}
		if( cart.getExpirationTime() > 0 && expireableSource.getExpirationTime() == 0) {
			builderCreated    = cart.getCreationTime();
			expireableSource = cart::getExpirationTime;
		} 
		if(expireableSource.getExpirationTime() == 0) {
			builderCreated = System.currentTimeMillis();
			if(ttl > 0) {
				expireableSource = () -> builderCreated + TimeUnit.MILLISECONDS.convert(ttl, unit);
			}
		}
	}

	/**
	 * Accept the data.
	 *
	 * @param cart the cart
	 */
	public void accept(C cart) {
		this.lastCart = cart;
		if( saveCarts) {
			allCarts.add(cart);
		}
		L label      = cart.getLabel();
		Object value = cart.getValue();

		lock.lock();
		try {
			getValueConsumer(label, value, builder).accept(label, value, builder);
		} finally {
			lock.unlock();
		}
		acceptCount++;
		if (eventHistory.containsKey(label)) {
			eventHistory.get(label).incrementAndGet();
		} else {
			eventHistory.put(label, new AtomicInteger(1));
		}
		// this itself does not affect expiration time
		// it should be enabled
		// enabling may affect performance
		if(acceptCount > 1) {
			postponeAlg.accept(this, cart);
		}
	}

	/**
	 * Timeout.
	 *
	 * @param cart the cart
	 */
	public void timeout(C cart) {
		this.lastCart = cart;
		lock.lock();
		try {
			if(postponeExpirationOnTimeoutEnabled) {
				timeoutAction.accept(builder);
				postponeAlg.accept(this, cart);
			} else {
				long expTimestamp = this.expireableSource.getExpirationTime();
				this.expireableSource = () -> expTimestamp;				
				timeoutAction.accept(builder);
			}
		} finally {
			lock.unlock();
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

	/** The product supplier. */
	private Supplier<? extends OUT> productSupplier = null;
	
	/**
	 * Gets the product supplier.
	 *
	 * @return the product supplier
	 */
	public Supplier<? extends OUT> getProductSupplier() {
		if(productSupplier == null ) {
			final BuildingSite <K, L, C, OUT> bs = this;
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
						bs.postponeAlg.accept(bs, null);
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
				expireableSource.getExpirationTime(),
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

	/**
	 * Gets the delay msec.
	 *
	 * @return the delay msec
	 */
	public long getDelayMsec() {
		return expireableSource.getExpirationTime()  -System.currentTimeMillis();	
	}

	/**
	 * Gets the builder expiration.
	 *
	 * @return the builder expiration
	 */
	@Override
	public long getExpirationTime() {
		return expireableSource.getExpirationTime();
	}

	/**
	 * Gets the cart.
	 *
	 * @return the cart
	 */
	public C getCreatingCart() {
		return initialCart;
	}

	/**
	 * Gets the accepted carts.
	 *
	 * @return the accepted carts
	 */
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
				+ ", builderCreated=" + builderCreated + ", builderExpiration=" + expireableSource.getExpirationTime() + ", "
				+ (status != null ? "status=" + status + ", " : "")
				+"delay="+getDelayMsec() + ", "
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

	/**
	 * Gets the last cart.
	 *
	 * @return the last cart
	 */
	public C getLastCart() {
		return lastCart;
	}

	/**
	 * Sets the last cart.
	 *
	 * @param lastCart the new last cart
	 */
	public void setLastCart(C lastCart) {
		this.lastCart = lastCart;
	}
	
	/**
	 * Complete futures with value.
	 *
	 * @param value the value
	 */
	void completeFuturesWithValue(Object value) {
		for(CompletableFuture f : futures ) {
			f.complete(value);
		}
	}

	/**
	 * Cancel futures.
	 */
	void cancelFutures() {
		for(CompletableFuture<? extends OUT> f : futures ) {
			f.cancel(true);
		}
	}

	/**
	 * Complete futures exceptionaly.
	 *
	 * @param value the value
	 */
	void completeFuturesExceptionaly(Throwable value) {
		for(CompletableFuture<? extends OUT> f : futures ) {
			f.completeExceptionally(value);
		}
	}

	
	/**
	 * Adds the future.
	 *
	 * @param resultFuture the result future
	 */
	public void addFuture(CompletableFuture<? extends OUT> resultFuture) {
		futures.add(resultFuture);
	}

	/**
	 * package access methods.
	 *
	 * @return the builder
	 */
	public Supplier<? extends OUT> getBuilder() {
		return builder;
	}
	
	/**
	 * Gets the lock.
	 *
	 * @return the lock
	 */
	public Lock getLock() {
		return lock;
	}

	/**
	 * Update expiration time.
	 *
	 * @param expirationTime the expiration time
	 */
	void updateExpirationTime(long expirationTime) {
		this.expireableSource = () -> expirationTime;
	}

	/**
	 * Gets the value consumer.
	 *
	 * @param label the label
	 * @param value the value
	 * @param builder the builder
	 * @return the value consumer
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private LabeledValueConsumer<L, Object, Supplier<? extends OUT>> getValueConsumer(L label, Object value, Supplier<? extends OUT> builder) {
		if(label == null) {
			return defaultValueConsumer;
		} else if(valueConsumer == null) {
			if( label instanceof SmartLabel ) {
				valueConsumer = (l,v,b) -> {
					((SmartLabel) l).get().accept(b, v);
				};
			} else {
				valueConsumer = defaultValueConsumer;
			}
		}
		return valueConsumer;
	}

	/**
	 * Gets the timeout action.
	 *
	 * @return the timeout action
	 */
	public Consumer<Supplier<? extends OUT>> getTimeoutAction() {
		return timeoutAction;
	}
		
}
