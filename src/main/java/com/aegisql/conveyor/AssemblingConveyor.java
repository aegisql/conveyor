package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssemblingConveyor<K, L, IN extends Cart<K, ?, L>, OUT> implements Conveyor<K, L, IN, OUT> {

	private final static Logger LOG = LoggerFactory.getLogger(AssemblingConveyor.class);

	private final Timer timer = new Timer("BuilderTimeoutTicker");

	private TimerTask expiredCollector = null;

	private long expirationCollectionInterval;

	private final Queue<IN> inQueue = new ConcurrentLinkedDeque<>(); // this class does not permit the use of null elements.

	private final BlockingQueue<BuildingSite<K, L, IN, OUT>> delayQueue = new DelayQueue<>();

	private final Map<K, BuildingSite<K, L, IN, OUT>> collector = new HashMap<>();

	private long builderTimeout = 1000;

	private boolean onTimeoutAction = false;

	private Consumer<OUT> resultConsumer   = out   -> { LOG.error("LOST RESULT "+out); };

	private Consumer<Object> scrapConsumer = scrap -> { LOG.debug("scrap: " + scrap); };
	
	private LabeledValueConsumer<L, ?, Builder<OUT>> cartConsumer = (l,v,b) -> { 
		LOG.error("Cart Consumer is not set");
		scrapConsumer.accept(l);
		scrapConsumer.accept(v);
		throw new IllegalStateException("Cart Consumer is not set");
	};
	
	private BiFunction<Lot<K>, Builder<OUT>, Boolean> ready = (l,b) -> {
		LOG.error("Readiness Evaluator is not set");
		scrapConsumer.accept(l);
		throw new IllegalStateException("Readiness Evaluator is not set");
	};
	
	private Supplier<Builder<OUT>> builderSupplier = () -> {
		throw new IllegalStateException("Builder Supplier is not set");
	};

	private boolean running = true;

	private static final class Lock {
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

					IN cart = inQueue.poll();
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
							LOG.error("Cart processor failed", e);
							scrapConsumer.accept(cart);
							if (buildingSite != null) {
								buildingSite.setLastError(e);
								scrapConsumer.accept(buildingSite);
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
		innerThread.setName("AssemblingConveyor Uninitialized");
		innerThread.start();
		LOG.debug("Started {}", innerThread.getName());
	}

	protected void drainQueues() {
		IN cart = null;
		while((cart = inQueue.poll()) != null) {
			scrapConsumer.accept(cart);
		}
		delayQueue.clear();
		collector.forEach((k,v)->{
			scrapConsumer.accept(k);
			scrapConsumer.accept(v);
		});
		collector.clear();
	}

	protected boolean addFirst(IN cart) {
		if (!running) {
			scrapConsumer.accept(cart);
			synchronized (lock) {
				lock.notify();
			}
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept(cart);
			synchronized (lock) {
				lock.notify();
			}
			throw new IllegalStateException("Data expired " + cart);
		}
		boolean r = inQueue.add(cart);
		synchronized (lock) {
			lock.notify();
		}
		return r;
	}

	@Override
	public boolean add(IN cart) {
		if (!running) {
			scrapConsumer.accept(cart);
			synchronized (lock) {
				lock.notify();
			}
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept(cart);
			synchronized (lock) {
				lock.notify();
			}
			throw new IllegalStateException("Data expired " + cart);
		}
		boolean r = inQueue.add(cart);
		synchronized (lock) {
			lock.notify();
		}
		return r;
	}

	@Override
	public boolean offer(IN cart) {
		if (!running || cart.expired()) {
			scrapConsumer.accept(cart);
			synchronized (lock) {
				lock.notify();
			}
			return false;
		}
		boolean r = inQueue.offer(cart);
		synchronized (lock) {
			lock.notify();
		}
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

	public void setScrapConsumer(Consumer<Object> scrapConsumer) {
		this.scrapConsumer = scrapConsumer;
	}

	public void stop() {
		running = false;
		timer.cancel();
		timer.purge();
		synchronized (lock) {
			lock.notify();
		}
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
						scrapConsumer.accept(buildingSite);
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

	public void setReadinessEvaluator(BiFunction<Lot<K>, Builder<OUT>, Boolean> ready) {
		this.ready = ready;
	}

	public void setBuilderSupplier(Supplier<Builder<OUT>> builderSupplier) {
		innerThread.setName( this.getClass().getSimpleName()+" building "+builderSupplier.get().getClass().getSimpleName() );
		this.builderSupplier = builderSupplier;
	}


}
