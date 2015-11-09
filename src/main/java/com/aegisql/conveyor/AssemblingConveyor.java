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
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuildingSite.Status;

public class AssemblingConveyor<K, L, IN extends Cart<K, ?, L>, OUT> implements Conveyor<K, L, IN, OUT> {

	private final static Logger LOG = LoggerFactory.getLogger(AssemblingConveyor.class);

	private final Timer timer = new Timer("BuilderTimeoutTicker");

	private TimerTask expiredCollector = null;

	private long expirationCollectionInterval;

	private final Queue<IN> inQueue = new ConcurrentLinkedDeque<>(); // this class does not permit the use of null elements.

	private final Queue<Cart<K,?,Command>> mQueue = new ConcurrentLinkedDeque<>(); // this class does not permit the use of null elements.

	private final BlockingQueue<BuildingSite<K, L, IN, OUT>> delayQueue = new DelayQueue<>();

	private final Map<K, BuildingSite<K, L, IN, OUT>> collector = new HashMap<>();

	private long builderTimeout = 1000;
	
	private long startTimeReject = System.currentTimeMillis();

	private boolean onTimeoutAction = false;

	private Consumer<OUT> resultConsumer   = out   -> { LOG.error("LOST RESULT "+out); };

	private BiConsumer<String,Object> scrapConsumer = (explanation, scrap) -> { LOG.error(explanation + " " + scrap); };
	
	private LabeledValueConsumer<L, ?, Builder<OUT>> cartConsumer = (l,v,b) -> { 
		scrapConsumer.accept("Cart Consumer is not set",l);
		scrapConsumer.accept("Cart Consumer is not set",v);
		throw new IllegalStateException("Cart Consumer is not set");
	};
	
	private BiPredicate<Lot<K>, Builder<OUT>> ready = (l,b) -> {
		scrapConsumer.accept("Readiness Evaluator is not set",l);
		throw new IllegalStateException("Readiness Evaluator is not set");
	};
	
	private Supplier<Builder<OUT>> builderSupplier = () -> {
		throw new IllegalStateException("Builder Supplier is not set");
	};

	private boolean running = true;

	private static final class Lock {
		public synchronized void tell() {
			this.notify();
		}
	}

	private final Lock lock = new Lock();

	private boolean await() {
		try {
			synchronized (lock) {
				if (inQueue.isEmpty()) {
					lock.wait();
				}
			}
		} catch (InterruptedException e) {
			LOG.error("Interrupted ", e);
			stop();
		}
		return running;
	}
	
	private BuildingSite<K, L, IN, OUT> getBuildingSite(IN cart) {
		K key = cart.getKey();
		if(key == null) {
			return null;
		}
		BuildingSite<K, L, IN, OUT> buildingSite = null;
		buildingSite = collector.get(key);
		if (buildingSite == null) {
			buildingSite = new BuildingSite<K, L, IN, OUT>(cart, builderSupplier, cartConsumer, ready,
					builderTimeout, TimeUnit.MILLISECONDS);
			collector.put(key, buildingSite);
			if (buildingSite.getBuilderExpiration() > 0) {
				delayQueue.add(buildingSite);
			}
		}
		return buildingSite;
	}
	
	private final Thread innerThread;
	
	public AssemblingConveyor() {
		this.innerThread = new Thread(() -> {
			try {
				while (running) {
					if (! await() ) break;

					processManagementCommands();
					
					IN cart = inQueue.poll();
					if(cart == null) {
						continue;
					}
					LOG.debug("Read " + cart);
					K key = cart.getKey();
					if (key != null) {
						BuildingSite<K, L, IN, OUT> buildingSite = null; 
						try {
							buildingSite = getBuildingSite(cart);
							buildingSite.accept((IN) cart);
							if (buildingSite.ready()) {
								collector.remove(key);
								resultConsumer.accept(buildingSite.build());
							}
						} catch (Exception e) {
							scrapConsumer.accept("Cart processor failed "+e.getMessage(),cart);
							if (buildingSite != null) {
								buildingSite.setLastError(e);
								scrapConsumer.accept("Site processor failed "+e.getMessage(),buildingSite);
							}
							collector.remove(key);
						}
					}
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

	private void processManagementCommands() {
		Cart<K,?,Command> cmd = null;
		while((cmd = mQueue.poll()) != null) {
			LOG.debug("processing command "+cmd);
			Command l = cmd.getLabel();
			l.getSetter().accept(this, cmd.getKey());
		}
	}

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

	public boolean addCommand(Cart<K,?,Command> cart) {
		if (!running) {
			scrapConsumer.accept("Not Running",cart);
			lock.tell();
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept("Expired cart",cart);
			lock.tell();
			throw new IllegalStateException("Data expired " + cart);
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept("Cart too old",cart);
			lock.tell();
			throw new IllegalStateException("Data too old");
		}
		boolean r = mQueue.add(cart);
		lock.tell();
		return r;
	}
	
	@Override
	public boolean add(IN cart) {
		if (!running) {
			scrapConsumer.accept("Not Running",cart);
			lock.tell();
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept("Expired cart",cart);
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

	@Override
	public boolean offer(IN cart) {
		if ( ! running) {
			scrapConsumer.accept("Not Running",cart);
			lock.tell();
			return false;
		}
		if (cart.expired()) {
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

	public int getCollectorSize() {
		return collector.size();
	}

	public int getInputQueueSize() {
		return inQueue.size();
	}

	public int getDelayedQueueSize() {
		return delayQueue.size();
	}

	public void setScrapConsumer(BiConsumer<String,Object> scrapConsumer) {
		this.scrapConsumer = scrapConsumer;
	}

	public void stop() {
		running = false;
		timer.cancel();
		timer.purge();
		lock.tell();
	}

	public long getExpirationCollectionInterval() {
		return expirationCollectionInterval;
	}

	public void removeExpired() {
		int cnt = 0;
		BuildingSite<K, L, IN, OUT> buildingSite = null;
		while ( (buildingSite = delayQueue.poll()) != null) {
			K key = buildingSite.getKey();
			if (collector.containsKey(key)) {
				LOG.debug("Expired " + key);
				collector.remove(key);
				cnt++;
				if (onTimeoutAction) {
					buildingSite.accept(null);
					if (buildingSite.ready()) {
						resultConsumer.accept(buildingSite.build());
					} else {
						scrapConsumer.accept("Site expired", buildingSite);
					}
				}
			}
		}
		if(cnt > 0) {
			LOG.debug("Timeout collected: " + cnt);
		}
	}

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
					Cart<K, Object, L> msg = new Cart<>(null, "CHECK_TIMEOUT", null);
					addFirst((IN) msg);
					delayQueue.add(exp); //return back
				}
			}
		};
		timer.schedule(expiredCollector, expirationCollectionInterval, expirationCollectionInterval);
	}

	public long getBuilderTimeout() {
		return builderTimeout;
	}

	public void setBuilderTimeout(long builderTimeout, TimeUnit unit) {
		this.builderTimeout = unit.toMillis(builderTimeout);
	}

	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
		this.startTimeReject = unit.toMillis(timeout);
	}

	
	public boolean isOnTimeoutAction() {
		return onTimeoutAction;
	}

	public void setOnTimeoutAction(boolean onTimeoutAction) {
		this.onTimeoutAction = onTimeoutAction;
	}

	public void setResultConsumer(Consumer<OUT> resultConsumer) {
		this.resultConsumer = resultConsumer;
	}

	public void setCartConsumer(LabeledValueConsumer<L, ?, Builder<OUT>> cartConsumer) {
		this.cartConsumer = cartConsumer;
	}

	public void setReadinessEvaluator(BiPredicate<Lot<K>, Builder<OUT>> ready) {
		this.ready = ready;
	}

	public void setBuilderSupplier(Supplier<Builder<OUT>> builderSupplier) {
		this.builderSupplier = builderSupplier;
	}

	public void setName(String name) {
		innerThread.setName(name);
	}

	/*
	 * STATIC METHODS TO SUPPORT MANAGEMENT COMMANDS
	 * 
	 * */
	
	static void cancelNow( AssemblingConveyor conveyor, Object key ) {
		Object res = conveyor.collector.remove(key);
		if(res != null) {
			conveyor.scrapConsumer.accept("Cancel Key Command executed ", res);
		}
	}

	static void timeoutNow(AssemblingConveyor conveyor, Object key ) {
		BuildingSite bs = (BuildingSite) conveyor.collector.get(key);
		if( bs == null ) {
			return;
		}
		final Delayed oldDelayKeeper = bs.delayKeeper;
		bs.setStatus(Status.TIMEED_OUT);
		bs.delayKeeper = new Delayed() {
			
			@Override
			public int compareTo(Delayed o) {
				return oldDelayKeeper.compareTo(o);
			}
			
			@Override
			public long getDelay(TimeUnit unit) {
				return -1;
			}
		};
	}

}
