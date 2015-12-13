/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuildingSite.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.AbstractCommand;
import com.aegisql.conveyor.cart.command.CreateCommand;

// TODO: Auto-generated Javadoc
/**
 * The Class AssemblingConveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <L> the generic type
 * @param <IN> the generic type
 * @param <OUT> the generic type
 */
public class AssemblingConveyor<K, L, OUT> implements Conveyor<K, L, OUT> {

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(AssemblingConveyor.class);

	/** The in queue. */
	private final Queue<Cart<K,?,L>> inQueue = new ConcurrentLinkedDeque<>(); // this class does not permit the use of null elements.

	/** The m queue. */
	private final Queue<AbstractCommand<K, ?>> mQueue = new ConcurrentLinkedDeque<>(); // this class does not permit the use of null elements.

	/** The delay queue. */
	private final BlockingQueue<BuildingSite<K, L, Cart<K,?,L>, ? extends OUT>> delayQueue = new DelayQueue<>();

	/** The collector. */
	protected final Map<K, BuildingSite<K, L, Cart<K,?,L>, ? extends OUT>> collector = new HashMap<>();

	/** The builder timeout. */
	private long builderTimeout = 0;
	
	/** The start time reject. */
	private long startTimeReject = System.currentTimeMillis();

	private Consumer<Supplier<? extends OUT>> timeoutAction;

	/** The result consumer. */
	private Consumer<ProductBin<K,OUT>> resultConsumer = out -> { LOG.error("LOST RESULT {} {}",out.key,out.product); };

	/** The scrap consumer. */
	private Consumer<ScrapBin<?,?>> scrapConsumer = (scrapBin) -> { LOG.error("{}",scrapBin); };
	
	/** The cart consumer. */
	private LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer = (l,v,b) -> { 
		scrapConsumer.accept( new ScrapBin<L, Object>(l,v, "Cart Consumer is not set. label"));
		throw new IllegalStateException("Cart Consumer is not set");
	};
	
	/** The ready. */
	private BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness = (l,b) -> {
		scrapConsumer.accept( new ScrapBin<K, State<K,L>>(l.key,l, "Readiness Evaluator is not set"));
		throw new IllegalStateException("Readiness Evaluator is not set");
	};
	
	/** The builder supplier. */
	private Supplier<Supplier<? extends OUT>> builderSupplier = () -> {
		throw new IllegalStateException("Builder Supplier is not set");
	};

	/** The running. */
	private volatile boolean running = true;
	
	protected boolean synchronizeBuilder = false;

	/** The inner thread. */
	private final Thread innerThread;
	
	/**
	 * The Class Lock.
	 */
	private static final class Lock {
		private final ReentrantLock rLock = new ReentrantLock();
		private final Condition hasCarts  = rLock.newCondition();
		
		private long expirationCollectionInterval = Long.MAX_VALUE;
		
		public void setExpirationCollectionInterval(long expirationCollectionInterval) {
			this.expirationCollectionInterval = expirationCollectionInterval;
		}

		public void setExpirationCollectionUnit(TimeUnit expirationCollectionUnit) {
			this.expirationCollectionUnit = expirationCollectionUnit;
		}

		private TimeUnit expirationCollectionUnit = TimeUnit.MILLISECONDS;

		/**
		 * Tell.
		 */
		public void tell() {
			rLock.lock();
			try {
				hasCarts.signal();
			} finally {
				rLock.unlock();
			}
		}
		
		/**
		 * Wait data.
		 *
		 * @param q the q
		 * @throws InterruptedException the interrupted exception
		 */
		public void waitData(Queue<?> q) throws InterruptedException {
			rLock.lock();
			try {
				if( q.isEmpty() ) {
					hasCarts.await(expirationCollectionInterval, expirationCollectionUnit);
				}				
			} finally {
				rLock.unlock();
			}
		}
		
		
	}

	/** The lock. */
	private final Lock lock = new Lock();

	/**
	 * Wait data.
	 *
	 * @return true, if successful
	 */
	private boolean waitData() {
		try {
			lock.waitData( inQueue );
		} catch (InterruptedException e) {
			LOG.error("Interrupted ", e);
			stop();
		}
		return running;
	}
	
	/**
	 * Gets the building site.
	 *
	 * @param cart the cart
	 * @return the building site
	 */
	private BuildingSite<K, L, Cart<K,?,L>, ? extends OUT> getBuildingSite(Cart<K,?,L> cart) {
		BuildingSite<K, L, Cart<K,?,L>, ? extends OUT> buildingSite = null;
		boolean returnNull = false;
		K key = cart.getKey();
		if(key == null) {
			returnNull = true;
		} else if( Status.TIMED_OUT.equals( cart.getValue() )) {
			returnNull = true;
		} else if ( (buildingSite = collector.get(key)) == null) {
			Supplier<Supplier<? extends OUT>> bs;
			
			if(cart.getValue() != null && cart instanceof CreatingCart ) {
				CreatingCart<K,Supplier<? extends OUT>,L> cc = (CreatingCart<K,Supplier<? extends OUT>,L>)cart;
				bs= cc.getValue();
				buildingSite = new BuildingSite<K, L, Cart<K,?,L>, OUT>(cart, bs, cartConsumer, readiness, timeoutAction, builderTimeout, TimeUnit.MILLISECONDS,synchronizeBuilder);
				returnNull = true;
			} if(cart.getValue() != null && cart instanceof CreateCommand ) {
				CreateCommand<K, Supplier<? extends OUT>> cc = (CreateCommand<K, Supplier<? extends OUT>>)cart;
				bs= cc.getValue();
				buildingSite = new BuildingSite<K, L, Cart<K,?,L>, OUT>(cart, bs, cartConsumer, readiness, timeoutAction, builderTimeout, TimeUnit.MILLISECONDS,synchronizeBuilder);
				returnNull = true;
			} else if(builderSupplier != null) {
				buildingSite = new BuildingSite<K, L, Cart<K,?,L>, OUT>(cart, builderSupplier, cartConsumer, readiness, timeoutAction, builderTimeout, TimeUnit.MILLISECONDS,synchronizeBuilder);
			} else {
				scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Ignore cart. Neither builder nor builder supplier available") );
				returnNull = true;
			}
			if(buildingSite != null) {
				collector.put(key, buildingSite);
				delayQueue.add(buildingSite);
			}
		}
		if( returnNull ) {
			return null;
		} else {
			return buildingSite;
		}
	}
		
	/**
	 * Instantiates a new assembling conveyor.
	 */
	public AssemblingConveyor() {
		this.innerThread = new Thread(() -> {
			try {
				while (running) {
					if (! waitData() ) 
						break;
					processManagementCommands();
					Cart<K,?,L> cart = inQueue.poll();
					if(cart == null) {
						removeExpired();
					} else {
						processSite(cart);
						removeExpired();
					}
				}
				LOG.debug("Leaving {}", Thread.currentThread().getName());
				drainQueues();
			} catch (Throwable e) { // Let it crash, but don't pretend its running
				stop();
				throw e;
			}
		});
		innerThread.setDaemon(false);
		innerThread.setName("AssemblingConveyor "+innerThread.getId());
		innerThread.start();
	}

	/**
	 * Process management commands.
	 */
	private void processManagementCommands() {
		Cart<K,?,CommandLabel> cmd = null;
		while((cmd = mQueue.poll()) != null) {
			LOG.debug("processing command "+cmd);
			CommandLabel l = cmd.getLabel();
			l.getSetter().accept(this, cmd);
		}
	}

	/**
	 * Drain queues.
	 */
	protected void drainQueues() {
		Cart<K,?,L> cart = null;
		while((cart = inQueue.poll()) != null) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Draining inQueue") );
		}
		delayQueue.clear();
		collector.forEach((k,v)->{
			scrapConsumer.accept( new ScrapBin<K,Object>(k,v,"Draining site") );
		});
		collector.clear();
	}

	/**
	 * Adds the first.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	protected boolean addFirst(Cart<K,?,L> cart) {
		if (!running) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Conveyor Not Running") );
			lock.tell();
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		boolean r = inQueue.add(cart);
		lock.tell();
		return r;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(com.aegisql.conveyor.Cart)
	 */
	@Override
	public boolean addCommand(AbstractCommand<K, ?> cart) {
		Objects.requireNonNull(cart);
		if (!running) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Conveyor Not Running") );
			lock.tell();
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Command Expired") );
			lock.tell();
			throw new IllegalStateException("Data expired " + cart);
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Command is too old") );
			lock.tell();
			throw new IllegalStateException("Data too old");
		}
		boolean r = mQueue.add(cart);
		lock.tell();
		return r;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(com.aegisql.conveyor.Cart)
	 */
	@Override
	public boolean add(Cart<K,?,L> cart) {
		Objects.requireNonNull(cart);
		if (!running) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Conveyor Not Running") );
			lock.tell();
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart Expired") );
			lock.tell();
			throw new IllegalStateException("Data expired " + cart);
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart is too old") );
			lock.tell();
			throw new IllegalStateException("Data too old");
		}
		boolean r = inQueue.add(cart);
		lock.tell();
		return r;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#offer(com.aegisql.conveyor.Cart)
	 */
	@Override
	public boolean offer(Cart<K,?,L> cart) {
		if( Objects.isNull(cart) ) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart is NULL") );
			lock.tell();
			return false;
		}
		if ( ! running ) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Conveyor Not Running") );
			lock.tell();
			return false;
		}
		if ( cart.expired() ) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart Expired") );
			lock.tell();
			return false;
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart is too old") );
			lock.tell();
			return false;
		}
		boolean r = inQueue.offer(cart);
		lock.tell();
		return r;
	}

	/**
	 * Gets the collector size.
	 *
	 * @return the collector size
	 */
	public int getCollectorSize() {
		return collector.size();
	}

	/**
	 * Gets the input queue size.
	 *
	 * @return the input queue size
	 */
	public int getInputQueueSize() {
		return inQueue.size();
	}

	/**
	 * Gets the delayed queue size.
	 *
	 * @return the delayed queue size
	 */
	public int getDelayedQueueSize() {
		return delayQueue.size();
	}

	/**
	 * Sets the scrap consumer.
	 *
	 * @param scrapConsumer the scrap consumer
	 */
	public void setScrapConsumer(Consumer<ScrapBin<?, ?>> scrapConsumer) {
		this.scrapConsumer = scrapConsumer;
	}

	/**
	 * Stop.
	 */
	public void stop() {
		running = false;
		lock.tell();
	}

	/**
	 * Gets the expiration collection interval.
	 *
	 * @return the expiration collection interval
	 */
	public long getExpirationCollectionInterval() {
		return lock.expirationCollectionInterval;
	}

	public TimeUnit getExpirationCollectionTimeUnit() {
		return lock.expirationCollectionUnit;
	}

	/**
	 * Process site.
	 *
	 * @param cart the cart
	 */
	private void processSite(Cart<K,?,L> cart) {
		K key = cart.getKey();
		if( key == null ) {
			return;
		}
		BuildingSite<K, L, Cart<K,?,L>, ? extends OUT> buildingSite = null; 
		try {
			LOG.debug("Read " + cart);
			buildingSite = getBuildingSite(cart);
			if(buildingSite == null) {
				return;
			}
			if(Status.TIMED_OUT.equals(cart.getValue())) {
				buildingSite.timeout((Cart<K,?,L>) cart);
			} else {
				buildingSite.accept((Cart<K,?,L>) cart);
			}
			if (buildingSite.ready()) {
				collector.remove(key);
				delayQueue.remove(buildingSite);
				OUT res = buildingSite.build();
				resultConsumer.accept(new ProductBin<K,OUT>(key, res, buildingSite.getDelay(TimeUnit.MILLISECONDS), Status.READY));
			}
		} catch (Exception e) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart Processor Failed",e) );
			if (buildingSite != null) {
				buildingSite.setStatus(Status.INVALID);
				buildingSite.setLastError(e);
				scrapConsumer.accept( new ScrapBin<K,BuildingSite<K,?,?,?>>(cart.getKey(),buildingSite,"Site Processor failed",e) );
			}
			collector.remove(key);
			delayQueue.remove(buildingSite);
		}
	}
	
	/**
	 * Removes the expired.
	 */
	private void removeExpired() {
		int cnt = 0;
		BuildingSite<K, L, Cart<K,?,L>, ? extends OUT> buildingSite = null;
		while ( (buildingSite = delayQueue.poll()) != null) {
			buildingSite.setStatus(Status.TIMED_OUT);
			K key = buildingSite.getKey();
			if (collector.containsKey(key)) {
				collector.remove(key);
				cnt++;
				if (timeoutAction != null) {
					try {
						ShoppingCart<K,Object,L> to = new ShoppingCart<K,Object,L>(buildingSite.getCart().getKey(), Status.TIMED_OUT,null);
						buildingSite.timeout((Cart<K,?,L>)to);
						if (buildingSite.ready()) {
							LOG.debug("Expired and finished " + key);
							OUT res = buildingSite.build();
							resultConsumer.accept(new ProductBin<K,OUT>(key, res, buildingSite.getDelay(TimeUnit.MILLISECONDS), Status.TIMED_OUT));
						} else {
							LOG.debug("Expired and not finished " + key);
							scrapConsumer.accept( new ScrapBin<K,BuildingSite<K, L, Cart<K,?,L>, ? extends OUT>>(key,buildingSite,"Site expired") );
						}
					} catch (Exception e) {
						buildingSite.setStatus(Status.INVALID);
						buildingSite.setLastError(e);
						scrapConsumer.accept( new ScrapBin<K,BuildingSite<K, L, Cart<K,?,L>, ? extends OUT>>(key,buildingSite,"Timeout processor failed ",e) );
					}
				} else {
					LOG.debug("Expired and removed " + key);
				}
			}
		}
		if(cnt > 0) {
			LOG.debug("Timeout collected: " + cnt);
		}
	}

	/**
	 * Sets the expiration collection interval.
	 *
	 * @param expirationCollectionInterval the expiration collection interval
	 * @param unit the unit
	 */
	public void setExpirationCollectionInterval(long expirationCollectionInterval, TimeUnit unit) {
		lock.setExpirationCollectionInterval(expirationCollectionInterval);
		lock.setExpirationCollectionUnit(unit);
		lock.tell();
	}

	/**
	 * Gets the builder timeout.
	 *
	 * @return the builder timeout
	 */
	public long getDefaultBuilderTimeout() {
		return builderTimeout;
	}

	/**
	 * Sets the builder timeout.
	 *
	 * @param builderTimeout the builder timeout
	 * @param unit the unit
	 */
	public void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit) {
		this.builderTimeout = unit.toMillis(builderTimeout);
	}

	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param timeout the timeout
	 * @param unit the unit
	 */
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
		this.startTimeReject = unit.toMillis(timeout);
	}

	
	/**
	 * Checks if is on timeout action.
	 *
	 * @return true, if is on timeout action
	 */
	public boolean isOnTimeoutAction() {
		return timeoutAction != null;
	}

	/**
	 * Sets the on timeout action.
	 *
	 * @param onTimeoutAction the new on timeout action
	 */
	public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction) {
		this.timeoutAction = timeoutAction;
	}

	/**
	 * Sets the result consumer.
	 *
	 * @param resultConsumer the new result consumer
	 */
	public void setResultConsumer(Consumer<ProductBin<K,OUT>> resultConsumer) {
		this.resultConsumer = resultConsumer;
	}

	/**
	 * Sets the cart consumer.
	 *
	 * @param cartConsumer the cart consumer
	 */
	public void setDefaultCartConsumer(LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer) {
		this.cartConsumer = cartConsumer;
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the ready
	 */
	public void setReadinessEvaluator(BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness) {
		this.readiness = readiness;
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the ready
	 */
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
		this.readiness = (status,builder) -> readiness.test( builder ) ;
	}

	/**
	 * Sets the builder supplier.
	 *
	 * @param builderSupplier the new builder supplier
	 */
	public void setBuilderSupplier(Supplier<Supplier<? extends OUT>> builderSupplier) {
		this.builderSupplier = builderSupplier;
	}

	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	public void setName(String name) {
		innerThread.setName(name);
	}

	/*
	 * STATIC METHODS TO SUPPORT MANAGEMENT COMMANDS
	 * 
	 * */

	/**
	 * Creates the now.
	 *
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	static void createNow( AssemblingConveyor conveyor, Object cart ) {
		BuildingSite bs = (BuildingSite) conveyor.getBuildingSite((Cart) cart);
	}

	/**
	 * Cancel now.
	 *
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	static void cancelNow( AssemblingConveyor conveyor, Object cart ) {
		Object key = ((Cart)cart).getKey();
		BuildingSite bs = (BuildingSite) conveyor.collector.get(key);
		if(bs != null) {
			bs.setStatus(Status.CANCELED);
		}
	}

	/**
	 * Timeout now.
	 *
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	static void timeoutNow( AssemblingConveyor conveyor, Object cart ) {
		Object key = ((Cart)cart).getKey();
		BuildingSite bs = (BuildingSite) conveyor.collector.get(key);
		if( bs != null ) {
			conveyor.delayQueue.remove(bs);
			final Delayed oldDelayKeeper = bs.delayKeeper;
			bs.setStatus(Status.TIMED_OUT);
			bs.delayKeeper = new Delayed() {
				@Override
				public int compareTo(Delayed o) {
					return -1;
				}
				@Override
				public long getDelay(TimeUnit unit) {
					return -1;
				}
			};
			conveyor.delayQueue.add(bs);
			ShoppingCart to = new ShoppingCart(bs.getCart().getKey(), Status.TIMED_OUT, null);
			conveyor.addFirst( to );
		}
	}

	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning() {
		return running;
	}

	public boolean isSynchronizeBuilder() {
		return synchronizeBuilder;
	}

	public void setSynchronizeBuilder(boolean synchronizeBuilder) {
		this.synchronizeBuilder = synchronizeBuilder;
	}

}
