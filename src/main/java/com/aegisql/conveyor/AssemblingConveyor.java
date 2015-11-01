package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssemblingConveyor<K, L, IN extends Cart<K, ?, L>, OUT> implements Conveyor<K, L, IN, OUT> {

	private final static Logger LOG = LoggerFactory.getLogger(AssemblingConveyor.class);

	private final Queue<IN> inQueue = new ConcurrentLinkedQueue<>();

	private final Map<K, BuildingSite<K,L,IN,OUT>> collector = new HashMap<>();

	private Consumer<Object> scrapConsumer = scrap -> {
		LOG.debug("scrap: " + scrap);
	};

	private boolean running = true;

	private static final class Lock {
	}

	private final Lock lock = new Lock();

	public AssemblingConveyor(Supplier<Builder<OUT>> builderSupplier,
			BiConsumer<Lot<K, ?, L>, Builder<OUT>> cartConsumer, Consumer<OUT> resultConsumer) {

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
						running = false;
					}

					Cart<K, ?, L> cart = inQueue.poll();
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
							bs = new BuildingSite<K, L, IN, OUT>(builderSupplier, cartConsumer);
							collector.put(key, bs);
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
				running = false;
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
		synchronized (lock) {
			lock.notify();
		}
	}

}
