package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssemblingConveyor<K, L, IN extends Cart<K, ?, L>, OUT> implements Conveyor<K, L, IN, OUT> {

	public static enum TimeoutStrategy {
		NON_EXPIREABLE, TIMEOUT_FROM_QUERY, TIMEOUT_FROM_CONVEYOR
	}

	private final static Logger LOG = LoggerFactory.getLogger(AssemblingConveyor.class);

	private final Timer timer = new Timer("BuilderTimeoutTicker");

	private TimerTask expiredCollector = null;

	private long expirationCollectionInterval;

	private final Queue<IN> inQueue = new ConcurrentLinkedQueue<>();

	private final BlockingQueue<BuildingSite<K, L, IN, OUT>> delayQueue = new DelayQueue<>();

	private final Map<K, BuildingSite<K, L, IN, OUT>> collector = new HashMap<>();

	private TimeoutStrategy timeoutStrategy = TimeoutStrategy.TIMEOUT_FROM_CONVEYOR;

	private long builderTimeout = 1000;

	private boolean onTimeoutAction = false;

	private final Consumer<OUT> resultConsumer;

	private Consumer<Object> scrapConsumer = scrap -> {
		LOG.debug("scrap: " + scrap);
	};

	private boolean running = true;

	private static final class Lock {
	}

	private final Lock lock = new Lock();

	public AssemblingConveyor(Supplier<Builder<OUT>> builderSupplier,
			BiConsumer<Lot<K, ?, L>, Builder<OUT>> cartConsumer, Consumer<OUT> resultConsumer) {
		this.resultConsumer = resultConsumer;
		Thread t = new Thread(() -> {
			try {
				while (running) {
					try {
						synchronized (lock) {
							if (inQueue.isEmpty()) {
								lock.wait();
							}
						}
						if (!running) {
							break;
						}
					} catch (InterruptedException e) {
						LOG.error("Interrupted ", e);
						stop();
						break;
					}

					IN cart = inQueue.poll();
					if (cart == null) {
						LOG.warn("Empty cart");
						continue;
					}
					LOG.debug("Read " + cart);
					K key = cart.getKey();
					if (key != null) {
						BuildingSite<K, L, IN, OUT> buildingSite = null;
						try {
							buildingSite = collector.get(key);
							if (buildingSite == null) {

								switch (timeoutStrategy) {
								case NON_EXPIREABLE:
									buildingSite = new BuildingSite<K, L, IN, OUT>(cart, builderSupplier, cartConsumer);
									break;
								case TIMEOUT_FROM_CONVEYOR:
									buildingSite = new BuildingSite<K, L, IN, OUT>(cart, builderSupplier, cartConsumer,
											builderTimeout, TimeUnit.MILLISECONDS);
									break;
								case TIMEOUT_FROM_QUERY:
									buildingSite = new BuildingSite<K, L, IN, OUT>(cart, builderSupplier, cartConsumer,
											cart.getExpirationTime());
									break;
								}

								collector.put(key, buildingSite);
								if (buildingSite.getBuilderExpiration() > 0) {
									delayQueue.add(buildingSite);
								}
							}

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
			} catch (Throwable e) { // Let it crash, but don't pretend its
									// running
				stop();
				throw e;
			}
		});
		t.setDaemon(false);
		t.setName("AssemblingConveyor for " + builderSupplier.get().getClass().getSimpleName());
		t.start();
		LOG.debug("Started {}", t.getName());
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
			K key = buildingSite.getCart().getKey();
			if (collector.containsKey(key)) {
				LOG.debug("Expired " + key);
				collector.remove(key);
				cnt++;
				if (onTimeoutAction) {
					buildingSite.accept(null);
					if (buildingSite.ready()) {
						LOG.debug("Good after timeout " + key);
						resultConsumer.accept(buildingSite.build());
					} else {
						LOG.debug("To Scrap Yard " + key);
						scrapConsumer.accept(buildingSite);
					}
				}
			}
		}
		LOG.debug("Timeout collected: " + cnt);
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
				if (delayQueue.poll() != null) {
					LOG.debug("CHECK TIMEOUT SENT " + inQueue.size() + " " + inQueue.peek());
					Cart<K, Object, L> msg = new Cart<>(null, "CHECK_TIMEOUT", null);
					add((IN) msg);
				}
			}
		};
		timer.schedule(expiredCollector, expirationCollectionInterval, expirationCollectionInterval);
	}

	public TimeoutStrategy getTimeoutStrategy() {
		return timeoutStrategy;
	}

	public void setTimeoutStrategy(TimeoutStrategy timeoutStrategy) {
		this.timeoutStrategy = timeoutStrategy;
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

}
