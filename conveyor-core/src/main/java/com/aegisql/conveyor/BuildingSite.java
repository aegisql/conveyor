/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
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

// TODO: Auto-generated Javadoc

/**
 * The Class BuildingSite.
 *
 * @param <K>   the key type
 * @param <L>   the generic type
 * @param <C>   the generic type
 * @param <OUT> the generic type
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class BuildingSite <K, L, C extends Cart<K, ?, L>, OUT> implements Expireable, Interruptable {

	/**
	 * The Class Memento.
	 *
	 * @param <K>   the key type
	 * @param <L>   the generic type
	 * @param <OUT> the generic type
	 */
	public static class Memento<K,L,OUT> implements Serializable {
		
		/** The Constant serialVersionUID. */
		@Serial
        private static final long serialVersionUID = 1L;

		/**
		 * The timestamp.
		 */
		final long timestamp;

		/**
		 * The state.
		 */
		final State<K,L> state;

		/**
		 * The builder.
		 */
		final Supplier<? extends OUT> builder;

		/**
		 * The properties.
		 */
		final Map<String,Object> properties = new HashMap<>();

		/**
		 * Instantiates a new memento.
		 *
		 * @param state      the state
		 * @param builder    the builder
		 * @param properties the properties
		 */
		Memento(
				State<K,L> state,
				Supplier<? extends OUT> builder,
				Map<String,Object> properties
				) {
			this.state = state;
			this.timestamp = System.currentTimeMillis();
			this.builder = builder;
			this.properties.putAll(properties);
		}

		/**
		 * Gets the timestamp.
		 *
		 * @return the timestamp
		 */
		public long getTimestamp() {
			return timestamp;
		}

		/**
		 * Gets the id.
		 *
		 * @return the id
		 */
		public K getId() {
			return state.key;
		}

		/**
		 * Gets the expiration time.
		 *
		 * @return the expiration time
		 */
		public long getExpirationTime() {
			return state.builderExpiration;
		}

		/**
		 * Gets build creation time.
		 *
		 * @return the creation time
		 */
		public long getCreationTime() {
			return state.builderCreated;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder2 = new StringBuilder();
			builder2.append("Memento [timestamp=").append(timestamp)
			.append(", build created=").append(state.builderCreated)
			.append(", build expires=").append(state.builderExpiration)
			.append(", builder=").append(builder)
			.append(", properties=").append(properties).append("]");
			return builder2.toString();
		}
	}

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(BuildingSite.class);

	/**
	 * The Constant NON_LOCKING_LOCK.
	 */
	public final static Lock NON_LOCKING_LOCK = new Lock() {
		public void lock() {}
		public void lockInterruptibly() {}
		public boolean tryLock() {
			return true;
		}
		public boolean tryLock(long time, TimeUnit unit) {
			return true;
		}
		public void unlock() {}
		public Condition newCondition() {
			return null;
		}
	};
	
	/** The builder. */
	private Supplier<? extends OUT> builder;
	
	/** The value consumer. */
	private final LabeledValueConsumer<L, Cart<K,?,L>, Supplier<? extends OUT>> defaultValueConsumer;

	/** The value consumer. */
	private LabeledValueConsumer<L, Cart<K,?,L>, Supplier<? extends OUT>> valueConsumer = null;

	/** The readiness. */
	private final BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness;

	/** The timeout action. */
	private final Consumer<Supplier<? extends OUT>> timeoutAction;

	/** The save carts. */
	private final boolean saveCarts;

	/** The all carts. */
	private final List<C> allCarts = new ArrayList<>();
	
	/** The futures. */
	private final List<CompletableFuture<? extends OUT>> futures = new ArrayList<>();
	
	/** The initial cart. */
	private final  C initialCart;

	/** The last cart. */
	private  C lastCart;

	/** The accept count. */
	private int acceptCount = 0;
	
	/** The builder created. */
	private long builderCreated;

	/**
	 * The builder expiration.
	 */
	Expireable expireableSource = () -> 0;
	
	/** The status. */
	private Status status = Status.WAITING_DATA;
	
	/** The last error. */
	private Throwable lastError;
	
	/** The event history. */
	private final Map<L,AtomicInteger> eventHistory = new LinkedHashMap<>();
	
	/** The properties. */
	private Map<String,Object> properties = new HashMap<>();
	
	/** The lock. */
	private final Lock lock;
	
	/** The postpone expiration on timeout enabled. */
	private final boolean postponeExpirationOnTimeoutEnabled;

	/** The add expiration time msec. */
	private final long addExpirationTimeMsec;
	
	/** The postpone alg. */
	private BiConsumer<BuildingSite <K, L, C, OUT>,C> postponeAlg = (bs,cart)->{/*do nothing*/};
	
	/** The result consumer. */
	private ResultConsumer<K,OUT> resultConsumer;

	/** The complete result consumer. */
	private ResultConsumer<K,OUT> completeResultConsumer = b->{};	
	
	/** The cancel result consumer. */
	private Consumer<Boolean> cancelResultConsumer = b->{};

	/** The exceptional result consumer. */
	private Consumer<Throwable> exceptionalResultConsumer = t->{};

	/** The acknowledge. */
	private final Acknowledge acknowledge;

	private final Conveyor<K,Object,OUT> conveyor;

	/** The siteRunning variable is set by the inner thread, read by the interrup method, called by some other thread*/
	private volatile Boolean siteRunning = false;

	private final Object syncLock = new Object();

	/**
	 * Instantiates a new building site.
	 *
	 * @param cart                               the cart
	 * @param builderSupplier                    the builder supplier
	 * @param cartConsumer                       the cart consumer
	 * @param readiness                          the ready
	 * @param timeoutAction                      the timeoutAction
	 * @param ttl                                the ttl
	 * @param unit                               the unit
	 * @param synchronizeBuilder                 the synchronizeBuilder
	 * @param saveCarts                          the save carts
	 * @param postponeExpirationEnabled          the postpone expiration enabled
	 * @param addExpirationTimeMsec              the add expiration time msec
	 * @param postponeExpirationOnTimeoutEnabled the postpone expiration on timeout enabled
	 * @param staticValues                       map of static values
	 * @param resultConsumer                     product consumer
	 * @param ackAction                          the ack action
	 * @param conveyor                           the conveyor
	 */
	@SuppressWarnings("unchecked")
	public BuildingSite( 
			C cart, 
			Supplier<Supplier<? extends OUT>> builderSupplier,
			LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer, 
			BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness, 
			Consumer<Supplier<? extends OUT>> timeoutAction,
			long ttl,
			TimeUnit unit,
			boolean synchronizeBuilder, 
			boolean saveCarts, 
			boolean postponeExpirationEnabled, 
			long addExpirationTimeMsec, 
			boolean postponeExpirationOnTimeoutEnabled,
			Map<L,C> staticValues,
			ResultConsumer<K,OUT> resultConsumer,
			Consumer<AcknowledgeStatus<K>> ackAction,
			Conveyor<K,L,OUT> conveyor
			) {
		this.initialCart               = cart;
		this.lastCart                  = cart;
		/* The default timeout action. */
		this.saveCarts                 = saveCarts;
		this.postponeExpirationOnTimeoutEnabled = postponeExpirationOnTimeoutEnabled;
		this.addExpirationTimeMsec     = addExpirationTimeMsec;
		this.defaultValueConsumer      = (LabeledValueConsumer<L, Cart<K,?,L>, Supplier<? extends OUT>>) cartConsumer;
		this.acknowledge               = new Acknowledge() {
			
			private volatile boolean acknowledged = false;
			@Override
			public boolean isAcknowledged() {
				return acknowledged;
			}
			
			@Override
			public synchronized void ack() {
				if(! acknowledged) {
					ackAction.accept(new AcknowledgeStatus<>(getKey(),status,properties));
					acknowledged = true;
				}
			}
		};
		this.conveyor = (Conveyor<K, Object, OUT>) conveyor;
		this.resultConsumer = Objects.requireNonNullElseGet(resultConsumer, () -> bin -> LOG.error("LOST RESULT {} {}", bin.key, bin.product));
		
		var productSupplier = builderSupplier.get();
		
		if(cart.getValue() != null && cart.getValue() instanceof Memento memento) {
			this.restore(memento);
		} else {
			if( productSupplier instanceof ProductSupplier && ((ProductSupplier)productSupplier).getSupplier() != null) {
				this.builder = ((ProductSupplier)productSupplier).getSupplier();
			} else {
				this.builder = productSupplier;
			}
		}
		
		if(synchronizeBuilder) {
			this.lock = new ReentrantLock();
		} else {
			this.lock = BuildingSite.NON_LOCKING_LOCK;
		}
		if (productSupplier instanceof TimeoutAction) {
			this.timeoutAction = b -> ((TimeoutAction) productSupplier).onTimeout();
		} else this.timeoutAction = timeoutAction;

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
		if(productSupplier instanceof Expireable expireable) {
			builderCreated    = System.currentTimeMillis();
			expireableSource  = expireable;
		} else {
			if(postponeExpirationEnabled) {
				postponeAlg = (bs,c) -> {
					if( c != null && c.isExpireable() ) {
						bs.expireableSource = c;
					} else if( expireableSource.isExpireable() ) {
						bs.expireableSource = ()->System.currentTimeMillis() + bs.addExpirationTimeMsec; //just add some time, if expireable, lowest priority
					}
				};			
			}
		}
		if( cart.isExpireable() && ! expireableSource.isExpireable() ) {
			builderCreated    = cart.getCreationTime();
			expireableSource = cart;
		} 
		if( ! expireableSource.isExpireable() ) {
			builderCreated = System.currentTimeMillis();
			if(ttl > 0) {
				expireableSource = () -> builderCreated + TimeUnit.MILLISECONDS.convert(ttl, unit);
			}
		}
		if(staticValues != null && staticValues.size() > 0) {
			staticValues.values().forEach(this::accept);
		}
	}

	/**
	 * Accept the data.
	 *
	 * @param cart the cart
	 */
	public void accept(C cart) {
		lock.lock();
		try {
			this.lastCart     = cart;
			if( saveCarts) {
				allCarts.add(cart);
			}
			L label      = cart.getLabel();

			getValueConsumer(label).accept(label, cart, builder);
			acceptCount++;
			eventHistory.computeIfAbsent(label,(l)->new AtomicInteger()).incrementAndGet();
			// this itself does not affect expiration time
			// it should be enabled
			//  may affect performance
			if(acceptCount > 1) {
				postponeAlg.accept(this, cart);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Timeout.
	 *
	 * @param cart the cart
	 */
	public void timeout(C cart) {
		lock.lock();
		try {
			this.lastCart = cart;
			if(postponeExpirationOnTimeoutEnabled) {
				timeoutAction.accept(builder);
				postponeAlg.accept(this, cart);
			} else {
				var expTimestamp = this.expireableSource.getExpirationTime();
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
		return unsafeBuild();
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
			final var bs = this;
			productSupplier = (Supplier<OUT>) () -> {
				if (!getStatus().equals(Status.WAITING_DATA)) {
					throw new IllegalStateException("Supplier is in a wrong state: " + getStatus());
				}
				OUT res;
				lock.lock();
				try {
					res = builder.get();
					bs.postponeAlg.accept(bs, null);
				} finally {
					lock.unlock();
				}
				return res;
			};
		}
		return productSupplier;
	}

	/**
	 * Gets the state.
	 *
	 * @return the state
	 */
	public State<K,L> getState() {
		final var history = new LinkedHashMap<L,Integer>();
		eventHistory.forEach((k,v)->history.put(k, v.get()));
		return new State<>(
				initialCart.getKey(),
				builderCreated,
				expireableSource.getExpirationTime(),
				initialCart.getCreationTime(),
				initialCart.getExpirationTime(),
				acceptCount,
				Collections.unmodifiableMap( history ),
				Collections.unmodifiableList(allCarts)
				);
	}

	/**
	 * Ready.
	 *
	 * @return true, if successful
	 */
	public boolean ready() {
		var res = false;
		var state = getState();
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
		var expirationTime = expireableSource.getExpirationTime() ;
		if(expirationTime == 0 ) {
			return Long.MAX_VALUE;
		} else {
			return expireableSource.getExpirationTime() - System.currentTimeMillis();
		}
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
				+ (getDelayMsec() == Long.MAX_VALUE ? "unexpireable" : "delay=" + getDelayMsec()) + ", "
				+ (lastError != null ? "lastError=" + lastError + ", " : "")
				+ "eventHistory=" + eventHistory + "]";
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
	 * @param value  the value
	 * @param status the status
	 */
	void completeWithValue(OUT value, Status status) {
		var ack = getAcknowledge();
		resultConsumer.andThen(completeResultConsumer).accept(new ProductBin<>(conveyor, getKey(), value, getExpirationTime(), status, getProperties(), ack) );
	}

	/**
	 * Sets the result consumer.
	 *
	 * @param resultConsumer the result consumer
	 */
	void setResultConsumer(ResultConsumer<K,OUT> resultConsumer) {
		this.resultConsumer = resultConsumer;
	}

	/**
	 * Cancel futures.
	 */
	void cancelFutures() {
		cancelResultConsumer.accept(true);
	}

	/**
	 * Complete futures exceptionaly.
	 *
	 * @param error the error
	 */
	void completeFuturesExceptionaly(Throwable error) {
		exceptionalResultConsumer.accept(error);
	}


	/**
	 * Adds the future.
	 *
	 * @param resultFuture the result future
	 */
	public void addFuture(final CompletableFuture<OUT> resultFuture) {
		this.completeResultConsumer = this.completeResultConsumer.andThen(bin-> resultFuture.complete(bin.product));
		this.cancelResultConsumer = this.cancelResultConsumer.andThen(resultFuture::cancel);
		this.exceptionalResultConsumer = this.exceptionalResultConsumer.andThen(resultFuture::completeExceptionally);
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
	 * @return the value consumer
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private LabeledValueConsumer<L, Cart<K,?,L>, Supplier<? extends OUT>> getValueConsumer(L label) {
		if(label == null) {
			return defaultValueConsumer;
		} else if(valueConsumer == null) {
			if( label instanceof SmartLabel ) {
				valueConsumer = (l,v,b) -> {
					SmartLabel sl = (SmartLabel)l;
					sl.get().accept(b, sl.getPayload(v));
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

	/**
	 * Gets the properties.
	 *
	 * @return the properties
	 */
	public Map<String,Object> getProperties() {
		return properties;
	}

	/**
	 * Adds the properties.
	 *
	 * @param properties the properties
	 */
	public void addProperties(Map<String,Object> properties) {
		this.properties.putAll(properties);
	}

	/**
	 * Gets the acknowledge.
	 *
	 * @return the acknowledge
	 */
	public Acknowledge getAcknowledge() {
		return acknowledge;
	}

	/**
	 * Gets the memento.
	 *
	 * @return the memento
	 */
	public Memento getMemento() {
		return new Memento(getState(), builder, properties);
	}

	/**
	 * Restore.
	 *
	 * @param memento the memento
	 */
	public void restore(Memento memento) {
		this.builder = memento.builder;
		this.properties = memento.properties;
		this.acceptCount = memento.state.previouslyAccepted;
		this.allCarts.clear();
		this.allCarts.addAll((Collection<? extends C>) memento.state.carts);
		this.eventHistory.clear(); 
		memento.state.eventHistory.forEach((l,i)-> this.eventHistory.put((L) l, new AtomicInteger((int) i)));
	}

	/**
	 * Site running.
	 * Called only by the iner conveyor thread
	 *
	 * @param siteRunning the site running
	 */
	void siteRunning(boolean siteRunning) {
		synchronized (this.syncLock) {
			this.siteRunning = siteRunning;
		}
	}

	private void _interrupt(Thread conveyorThread) {
		if (builder instanceof Interruptable) {
			((Interruptable) builder).interrupt(conveyorThread);
		} else {
			conveyorThread.interrupt();
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Interruptable#interrupt(java.lang.Thread)
	 */
	@Override
	public void interrupt(Thread conveyorThread) {
		synchronized (this.syncLock) {
			if (siteRunning) {
				_interrupt(conveyorThread);
			}
		}
	}

	/**
	 * Interrupt.
	 *
	 * @param conveyorThread the conveyor thread
	 * @param key            the key
	 */
	public void interrupt(Thread conveyorThread, K key) {
		synchronized (this.syncLock) {
			if (siteRunning && getKey().equals(key)) {
				_interrupt(conveyorThread);
			}
		}
	}

}
