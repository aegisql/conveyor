/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuildingSite.Status;

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
public class AssemblingConveyor<K, L, IN extends Cart<K, ?, L>, OUT> implements Conveyor<K, L, IN, OUT> {

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(AssemblingConveyor.class);

	/** The timer. */
	private final Timer timer = new Timer("BuilderTimeoutTicker");

	/** The expired collector. */
	private TimerTask expiredCollector = null;

	/** The expiration collection interval. */
	private long expirationCollectionInterval = 0;

	/** The in queue. */
	private final Queue<IN> inQueue = new ConcurrentLinkedDeque<>(); // this class does not permit the use of null elements.

	/** The m queue. */
	private final Queue<Cart<K,?,Command>> mQueue = new ConcurrentLinkedDeque<>(); // this class does not permit the use of null elements.

	/** The delay queue. */
	private final BlockingQueue<BuildingSite<K, L, IN, OUT>> delayQueue = new DelayQueue<>();

	/** The collector. */
	private final Map<K, BuildingSite<K, L, IN, OUT>> collector = new HashMap<>();

	/** The builder timeout. */
	private long builderTimeout = 0;
	
	/** The start time reject. */
	private long startTimeReject = System.currentTimeMillis();

	/** The on timeout action. */
	private boolean onTimeoutAction = false;

	/** The result consumer. */
	private Consumer<OUT> resultConsumer   = out   -> { LOG.error("LOST RESULT "+out); };

	/** The scrap consumer. */
	private BiConsumer<String,Object> scrapConsumer = (explanation, scrap) -> { LOG.error(explanation + " " + scrap); };
	
	/** The cart consumer. */
	private LabeledValueConsumer<L, ?, Supplier<OUT>> cartConsumer = (l,v,b) -> { 
		scrapConsumer.accept("Cart Consumer is not set. label:",l);
		scrapConsumer.accept("Cart Consumer is not set value:",v);
		throw new IllegalStateException("Cart Consumer is not set");
	};
	
	/** The ready. */
	private BiPredicate<State<K>, Supplier<OUT>> readiness = (l,b) -> {
		scrapConsumer.accept("Readiness Evaluator is not set",l);
		throw new IllegalStateException("Readiness Evaluator is not set");
	};
	
	/** The builder supplier. */
	private Supplier<Supplier<OUT>> builderSupplier = () -> {
		throw new IllegalStateException("Builder Supplier is not set");
	};

	/** The running. */
	private volatile boolean running = true;

	/** The inner thread. */
	private final Thread innerThread;

	/**
	 * The Class Lock.
	 */
	private static final class Lock {
		
		/**
		 * Tell.
		 */
		public synchronized void tell() {
			this.notify();
		}
		
		/**
		 * Wait data.
		 *
		 * @param q the q
		 * @throws InterruptedException the interrupted exception
		 */
		public synchronized void waitData(Queue q) throws InterruptedException {
			if( q.isEmpty() ) {
				this.wait();
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
	private BuildingSite<K, L, IN, OUT> getBuildingSite(IN cart) {
		K key = cart.getKey();
		if(key == null) {
			return null;
		}
		if( Status.TIMED_OUT.equals( cart.getValue() )) {
			return null;
		}
		BuildingSite<K, L, IN, OUT> buildingSite = null;
		buildingSite = collector.get(key);
		if (buildingSite == null) {
			buildingSite = new BuildingSite<K, L, IN, OUT>(cart, builderSupplier, cartConsumer, readiness,
					builderTimeout, TimeUnit.MILLISECONDS);
			collector.put(key, buildingSite);
			if (buildingSite.getBuilderExpiration() > 0) {
				delayQueue.add(buildingSite);
			}
		}
		return buildingSite;
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
					IN cart = inQueue.poll();
					if(cart == null) 
						continue;
					processSite(cart);
					removeExpired();
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
		Cart<K,?,Command> cmd = null;
		while((cmd = mQueue.poll()) != null) {
			LOG.debug("processing command "+cmd);
			Command l = cmd.getLabel();
			l.getSetter().accept(this, cmd);
		}
	}

	/**
	 * Drain queues.
	 */
	protected void drainQueues() {
		IN cart = null;
		while((cart = inQueue.poll()) != null) {
			scrapConsumer.accept("Draining inQueue",cart);
		}
		delayQueue.clear();
		collector.forEach((k,v)->{
			scrapConsumer.accept("Draining site",v);
		});
		collector.clear();
	}

	/**
	 * Adds the first.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	protected boolean addFirst(IN cart) {
		if (!running) {
			scrapConsumer.accept("Not Running",cart);
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
	public boolean addCommand(Cart<K,?,Command> cart) {
		if (!running) {
			scrapConsumer.accept("Not Running",cart);
			lock.tell();
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept("Expired command",cart);
			lock.tell();
			throw new IllegalStateException("Data expired " + cart);
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept("Command too old",cart);
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
	public boolean add(IN cart) {
		if (!running) {
			scrapConsumer.accept("Not Running",cart);
			lock.tell();
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept("Cart expired",cart);
			lock.tell();
			throw new IllegalStateException("Data expired " + cart);
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept("Cart too old",cart);
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
	public boolean offer(IN cart) {
		if ( ! running ) {
			scrapConsumer.accept("Not Running",cart);
			lock.tell();
			return false;
		}
		if ( cart.expired() ) {
			scrapConsumer.accept("Cart expired",cart);
			lock.tell();
			return false;
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept("Cart is too old", cart);
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
	public void setScrapConsumer(BiConsumer<String,Object> scrapConsumer) {
		this.scrapConsumer = scrapConsumer;
	}

	/**
	 * Stop.
	 */
	public void stop() {
		running = false;
		timer.cancel();
		timer.purge();
		lock.tell();
	}

	/**
	 * Gets the expiration collection interval.
	 *
	 * @return the expiration collection interval
	 */
	public long getExpirationCollectionInterval() {
		return expirationCollectionInterval;
	}

	/**
	 * Process site.
	 *
	 * @param cart the cart
	 */
	private void processSite(IN cart) {
		K key = cart.getKey();
		if( key == null ) {
			return;
		}
		BuildingSite<K, L, IN, OUT> buildingSite = null; 
		try {
			LOG.debug("Read " + cart);
			buildingSite = getBuildingSite(cart);
			if(buildingSite == null) {
				return;
			}
			buildingSite.accept((IN) cart);
			if (buildingSite.ready()) {
				collector.remove(key);
				resultConsumer.accept(buildingSite.build());
			}
		} catch (Exception e) {
			scrapConsumer.accept("Cart processor failed "+e.getMessage(),cart);
			if (buildingSite != null) {
				buildingSite.setStatus(Status.INVALID);
				buildingSite.setLastError(e);
				scrapConsumer.accept("Site processor failed "+e.getMessage(),buildingSite);
			}
			collector.remove(key);
		}
	}
	
	/**
	 * Removes the expired.
	 */
	private void removeExpired() {
		int cnt = 0;
		BuildingSite<K, L, IN, OUT> buildingSite = null;
		while ( (buildingSite = delayQueue.poll()) != null) {
			buildingSite.setStatus(Status.TIMED_OUT);
			K key = buildingSite.getKey();
			if (collector.containsKey(key)) {
				collector.remove(key);
				cnt++;
				if (onTimeoutAction) {
					try {
						buildingSite.accept((IN) buildingSite.getCart().nextCart( Status.TIMED_OUT, null ));
						if (buildingSite.ready()) {
							LOG.debug("Expired and finished " + key);
							resultConsumer.accept(buildingSite.build());
						} else {
							LOG.debug("Expired and not finished " + key);
							scrapConsumer.accept("Site expired", buildingSite);
						}
					} catch (Exception e) {
						buildingSite.setStatus(Status.INVALID);
						buildingSite.setLastError(e);
						scrapConsumer.accept("Timeout processor failed "+e.getMessage(),buildingSite);
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
		this.expirationCollectionInterval = unit.toMillis(expirationCollectionInterval);
		if (expiredCollector != null) {
			expiredCollector.cancel();
			timer.purge();
		}
		expiredCollector = new TimerTask() {
			@Override
			public void run() {
				BuildingSite<K, L, IN, OUT> exp;
				if ( (exp = delayQueue.poll()) != null) {
					LOG.debug("CHECK TIMEOUT SENT" );
					Cart<K, Object, L> msg = exp.getCart().nextCart( Status.TIMED_OUT, null );
					addFirst((IN) msg);
					delayQueue.add(exp); //return back
				}
			}
		};
		timer.schedule(expiredCollector, expirationCollectionInterval, expirationCollectionInterval);
	}

	/**
	 * Gets the builder timeout.
	 *
	 * @return the builder timeout
	 */
	public long getBuilderTimeout() {
		return builderTimeout;
	}

	/**
	 * Sets the builder timeout.
	 *
	 * @param builderTimeout the builder timeout
	 * @param unit the unit
	 */
	public void setBuilderTimeout(long builderTimeout, TimeUnit unit) {
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
		return onTimeoutAction;
	}

	/**
	 * Sets the on timeout action.
	 *
	 * @param onTimeoutAction the new on timeout action
	 */
	public void setOnTimeoutAction(boolean onTimeoutAction) {
		this.onTimeoutAction = onTimeoutAction;
	}

	/**
	 * Sets the result consumer.
	 *
	 * @param resultConsumer the new result consumer
	 */
	public void setResultConsumer(Consumer<OUT> resultConsumer) {
		this.resultConsumer = resultConsumer;
	}

	/**
	 * Sets the cart consumer.
	 *
	 * @param cartConsumer the cart consumer
	 */
	public void setCartConsumer(LabeledValueConsumer<L, ?, Supplier<OUT>> cartConsumer) {
		this.cartConsumer = cartConsumer;
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the ready
	 */
	public void setReadinessEvaluator(BiPredicate<State<K>, Supplier<OUT>> readiness) {
		this.readiness = readiness;
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the ready
	 */
	public void setReadinessEvaluator(Predicate<Supplier<OUT>> readiness) {
		this.readiness = (status,builder) -> readiness.test( builder ) ;
	}

	/**
	 * Sets the builder supplier.
	 *
	 * @param builderSupplier the new builder supplier
	 */
	public void setBuilderSupplier(Supplier<Supplier<OUT>> builderSupplier) {
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
			conveyor.addFirst( bs.getCart().nextCart(Status.TIMED_OUT,null));
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

}
