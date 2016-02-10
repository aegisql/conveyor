/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
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
import com.aegisql.conveyor.delay.DelayProvider;

// TODO: Auto-generated Javadoc
/**
 * The Class AssemblingConveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <L> the generic type
 * @param <OUT> the generic type
 */
public class AssemblingConveyor<K, L, OUT> implements Conveyor<K, L, OUT> {

	/** The Constant LOG. */
	protected final static Logger LOG = LoggerFactory.getLogger(AssemblingConveyor.class);

	/** The in queue. */
	protected final Queue<Cart<K,?,L>> inQueue = new ConcurrentLinkedDeque<>(); // this class does not permit the use of null elements.

	/** The m queue. */
	protected final Queue<AbstractCommand<K, ?>> mQueue = new ConcurrentLinkedDeque<>(); // this class does not permit the use of null elements.

	/** The delay provider. */
	private final DelayProvider<K> delayProvider = new DelayProvider<>();
	
	/** The collector. */
	protected final Map<K, BuildingSite<K, L, Cart<K,?,L>, ? extends OUT>> collector = new HashMap<>();

	/** The builder timeout. */
	protected long builderTimeout = 0;
	
	/** The start time reject. */
	protected long startTimeReject = System.currentTimeMillis();

	protected Consumer<Supplier<? extends OUT>> timeoutAction;

	/** The result consumer. */
	protected Consumer<ProductBin<K,OUT>> resultConsumer = out -> { LOG.error("LOST RESULT {} {}",out.key,out.product); };

	/** The scrap consumer. */
	protected Consumer<ScrapBin<?,?>> scrapConsumer = (scrapBin) -> { LOG.error("{}",scrapBin); };
	
	/** The cart consumer. */
	protected LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer = (l,v,b) -> { 
		scrapConsumer.accept( new ScrapBin<L, Object>(l,v, "Cart Consumer is not set. label"));
		throw new IllegalStateException("Cart Consumer is not set");
	};
	
	/** The ready. */
	protected BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness = (l,b) -> {
		scrapConsumer.accept( new ScrapBin<K, State<K,L>>(l.key,l, "Readiness Evaluator is not set"));
		throw new IllegalStateException("Readiness Evaluator is not set");
	};
	
	/** The builder supplier. */
	protected Supplier<Supplier<? extends OUT>> builderSupplier = () -> {
		throw new IllegalStateException("Builder Supplier is not set");
	};

	/** The running. */
	protected volatile boolean running = true;
	
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

	private boolean saveCarts;

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
				buildingSite = new BuildingSite<K, L, Cart<K,?,L>, OUT>(cart, bs, cartConsumer, readiness, timeoutAction, builderTimeout, TimeUnit.MILLISECONDS,synchronizeBuilder,saveCarts);
				returnNull = true;
			} if(cart.getValue() != null && cart instanceof CreateCommand ) {
				CreateCommand<K, Supplier<? extends OUT>> cc = (CreateCommand<K, Supplier<? extends OUT>>)cart;
				bs= cc.getValue();
				buildingSite = new BuildingSite<K, L, Cart<K,?,L>, OUT>(cart, bs, cartConsumer, readiness, timeoutAction, builderTimeout, TimeUnit.MILLISECONDS,synchronizeBuilder, saveCarts);
				returnNull = true;
			} else if(builderSupplier != null) {
				buildingSite = new BuildingSite<K, L, Cart<K,?,L>, OUT>(cart, builderSupplier, cartConsumer, readiness, timeoutAction, builderTimeout, TimeUnit.MILLISECONDS, synchronizeBuilder,saveCarts);
			} else {
				scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Ignore cart. Neither builder nor builder supplier available") );
				returnNull = true;
			}
			if(buildingSite != null) {
				collector.put(key, buildingSite);
				if(buildingSite.getExpirationTime() > 0) {
					delayProvider.getBox(buildingSite.getExpirationTime()).add(key);
				}
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
					if(cart != null) {
						processSite(cart);
					}
					removeExpired();
				}
				LOG.info("Leaving {}", Thread.currentThread().getName());
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
			if(LOG.isDebugEnabled()) {
				LOG.debug("processing command "+cmd);
			}
			CommandLabel l = cmd.getLabel();
			l.get().accept(this, cmd);
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
		delayProvider.clear();
		collector.forEach((k,v)->{
			scrapConsumer.accept( new ScrapBin<K,Object>(k,v,"Draining collector") );
		});
		collector.clear();
	}

	protected RuntimeException evaluateCart(Cart<K,?,?> cart) {
		
		if( cart == null ) {
			lock.tell();
			return new NullPointerException("Cart is void");
		}

		if (!running) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Conveyor Not Running") );
			lock.tell();
			return new IllegalStateException("Assembling Conveyor is not running");
		}

		if (cart.expired()) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart has already expired") );
			lock.tell();
			return new IllegalStateException("Cart has already expired " + cart);
		}

		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart is too old") );
			lock.tell();
			return new IllegalStateException("Cart is too old");
		}
	
		return null;
	}
	
	/**
	 * Adds the first.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	protected boolean addFirst(Cart<K,?,L> cart) {
		
		RuntimeException e = evaluateCart(cart);
		
		if( e != null ) {
			throw e;
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
		RuntimeException e = evaluateCart(cart);
		
		if( e != null ) {
			throw e;
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
		RuntimeException e = evaluateCart(cart);
		
		if( e != null ) {
			throw e;
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
		RuntimeException e = evaluateCart(cart);
		
		if( e != null ) {
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
		return delayProvider.delayedSize();
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
	public long getExpirationCollectionIdleInterval() {
		return lock.expirationCollectionInterval;
	}

	public TimeUnit getExpirationCollectionIdleTimeUnit() {
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
			if(LOG.isTraceEnabled()) {
				LOG.trace("Read " + cart);
			}
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
		}
	}
	
	/**
	 * Removes the expired.
	 */
	private void removeExpired() {
		int cnt = 0;
		for(K key: delayProvider.getAllExpiredKeys()) {	
			BuildingSite<K, L, Cart<K,?,L>, ? extends OUT> buildingSite = collector.get(key);
			if(buildingSite == null) {
				continue;
			}
			buildingSite.setStatus(Status.TIMED_OUT);
			if (collector.containsKey(key)) {
				collector.remove(key);
				cnt++;
				if (timeoutAction != null || buildingSite.builder instanceof TimeoutAction ) {
					try {
						ShoppingCart<K,Object,L> to = new ShoppingCart<K,Object,L>(buildingSite.getCreatingCart().getKey(), Status.TIMED_OUT,null);
						buildingSite.timeout((Cart<K,?,L>)to);
						if (buildingSite.ready()) {
							if(LOG.isTraceEnabled()) {
								LOG.trace("Expired and finished " + key);
							}
							OUT res = buildingSite.build();
							resultConsumer.accept(new ProductBin<K,OUT>(key, res, buildingSite.getDelay(TimeUnit.MILLISECONDS), Status.TIMED_OUT));
						} else {
							if(LOG.isTraceEnabled()) {
								LOG.trace("Expired and not finished " + key);
							}
							scrapConsumer.accept( new ScrapBin<K,BuildingSite<K, L, Cart<K,?,L>, ? extends OUT>>(key,buildingSite,"Site expired") );
						}
					} catch (Exception e) {
						buildingSite.setStatus(Status.INVALID);
						buildingSite.setLastError(e);
						scrapConsumer.accept( new ScrapBin<K,BuildingSite<K, L, Cart<K,?,L>, ? extends OUT>>(key,buildingSite,"Timeout processor failed ",e) );
					}
				} else {
					if(LOG.isTraceEnabled()) {
						LOG.trace("Expired and removed " + key);
					}
					scrapConsumer.accept( new ScrapBin<K,BuildingSite<K, L, Cart<K,?,L>, ? extends OUT>>(key,buildingSite,"Site expired. No timeout action") );
				}
			}
		}
		if(cnt > 0) {
			if(LOG.isTraceEnabled()) {
				LOG.trace("Timeout collected: " + cnt);
			}
		}
	}

	/**
	 * Sets the expiration collection interval.
	 *
	 * @param expirationCollectionInterval the expiration collection interval
	 * @param unit the unit
	 */
	public void setExpirationCollectionIdleInterval(long expirationCollectionInterval, TimeUnit unit) {
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
	 * @param timeoutAction the new on timeout action
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
			bs.setStatus(Status.TIMED_OUT);
			conveyor.delayProvider.getBox(System.currentTimeMillis()).add(bs);
			ShoppingCart to = new ShoppingCart(bs.getCreatingCart().getKey(), Status.TIMED_OUT, null);
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

	public boolean isKeepCartsOnSite() {
		return saveCarts;
	}

	public void setKeepCartsOnSite(boolean saveCarts) {
		this.saveCarts = saveCarts;
	}

	public void setSynchronizeBuilder(boolean synchronizeBuilder) {
		this.synchronizeBuilder = synchronizeBuilder;
	}

}
