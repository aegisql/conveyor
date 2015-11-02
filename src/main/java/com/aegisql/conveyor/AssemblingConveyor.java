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
		NON_EXPIREABLE,
		TIMEOUT_FROM_QUERY,
		TIMEOUT_FROM_CONVEYOR
	}
	
	private final static Logger LOG = LoggerFactory.getLogger(AssemblingConveyor.class);

	private final Timer timer = new Timer();
	
	private TimerTask expiredCollector = null;
	
	private long expirationCollectionInterval;
	
	private final Queue<IN> inQueue = new ConcurrentLinkedQueue<>();

	private final BlockingQueue<BuildingSite<K,L,IN,OUT>> delayQueue = new DelayQueue<>();

	private final Map<K, BuildingSite<K,L,IN,OUT>> collector = new HashMap<>();

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
						if (!running)
							break;
					} catch (InterruptedException e) {
						LOG.error("Interrupted ", e);
						stop();
					}

					IN cart = inQueue.poll();
					if (cart == null) {
						LOG.warn("Empty cart");
						continue;
					}
					LOG.debug("Read " + cart);
					K key = cart.getKey();
					BuildingSite<K,L,IN,OUT> bs = null;
					try {
						bs = collector.get(key);
						if (bs == null) {
							
							switch(timeoutStrategy) {
							case NON_EXPIREABLE:
								bs = new BuildingSite<K, L, IN, OUT>( cart, builderSupplier, cartConsumer);
								break;
							case TIMEOUT_FROM_CONVEYOR:
								bs = new BuildingSite<K, L, IN, OUT>( cart, builderSupplier, cartConsumer, builderTimeout, TimeUnit.MILLISECONDS);
								break;
							case TIMEOUT_FROM_QUERY:
								bs = new BuildingSite<K, L, IN, OUT>( cart, builderSupplier, cartConsumer, cart.getExpirationTime() );
								break;
							}
							
							collector.put(key, bs);
							if(bs.getBuilderExpiration() > 0) {
								delayQueue.add(bs);
							}
						}

						bs.accept((IN) cart);

						if (bs.ready()) {
							collector.remove(key);
							resultConsumer.accept(bs.build());
						}

					} catch (Exception e) {
						LOG.error("Cart processor failed", e);
						scrapConsumer.accept(cart);
						if (bs != null) {
							scrapConsumer.accept(bs);
						}
						if (key != null) {
							collector.remove(key);
						}
					}
				}
				LOG.debug("Leaving {}", Thread.currentThread().getName());
			} catch (Throwable e) { //Let it crash, but don't pretend its running
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
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept(cart);
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

	public void setExpirationCollectionInterval(long expirationCollectionInterval,TimeUnit unit) {
		this.expirationCollectionInterval = unit.toMillis(expirationCollectionInterval);
		if(expiredCollector == null) {
			expiredCollector = new TimerTask() {

				@Override
				public void run() {
					int cnt = 0;
					while( ! delayQueue.isEmpty() ) {
						BuildingSite<K, L, Cart<K,?,L>, OUT> bs = (BuildingSite<K, L, Cart<K, ?, L>, OUT>) delayQueue.poll();
						if(bs == null) {
							continue;
						}
						K key = bs.getCart().getKey();
						if(collector.containsKey(key)) {
							LOG.debug("expired "+key);
							collector.remove(key);
							cnt++;
							if(onTimeoutAction) {
								bs.accept(null);
								if( bs.ready()) {
									LOG.debug("Good after timeout "+key);
									resultConsumer.accept(bs.build());
								} else {
									LOG.debug("To Scrap Yard "+key);
									scrapConsumer.accept(bs);
								}
							}
						}
					}
					LOG.debug("Expired collected: " + cnt);
				}
				
			};
		} else {
			expiredCollector.cancel();
			timer.purge();
		}
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
		this.builderTimeout = unit.toMillis( builderTimeout );
	}

	public boolean isOnTimeoutAction() {
		return onTimeoutAction;
	}

	public void setOnTimeoutAction(boolean onTimeoutAction) {
		this.onTimeoutAction = onTimeoutAction;
	}

}
