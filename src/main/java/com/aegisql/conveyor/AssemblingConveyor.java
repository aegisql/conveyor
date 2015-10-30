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
	
	private final Queue<IN> inQueue   = new ConcurrentLinkedQueue<>();

	private Map<K, Builder<OUT>> collector = new HashMap<>();

	private boolean running = true;

	private static final class Lock {
	}

	private final Object lock = new Lock();

	public AssemblingConveyor(
			Supplier<Builder<OUT>> builderSupplier,
			BiConsumer<Cart<K, ?, L>, Builder<OUT>> cartConsumer,
			Consumer<OUT> resultConsumer
			) {
		Thread t = new Thread(() -> {
			while (running) {
				if (inQueue.isEmpty()) {
					try {
						synchronized (lock) {
							lock.wait();
						}
					} catch (InterruptedException e) {
						LOG.error("Interrupted ",e);
						running = false;
					}
				}

				Cart<K, ?, L> cart = inQueue.poll();
				if (cart == null) {
					continue;
				}
				LOG.debug("Cart read " + cart);
				K key = cart.getKey();
				Builder<OUT> b;
				try {
					b = collector.get(key);
					if (b == null) {
						b = builderSupplier.get();
						collector.put(key, b);
					}

					cartConsumer.accept(cart, b);

					if (b.ready()) {
						collector.remove(key);
						resultConsumer.accept(b.build());
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			LOG.debug("Leaving thread {}"+Thread.currentThread().getName());
		});
		t.setDaemon(false);
		t.setName("AssemblingConveyor collecting " + builderSupplier.get().getClass().getSimpleName());
		t.start();
	}

	@Override
	public boolean add(IN cart) {
		boolean r = inQueue.add(cart);
		synchronized (lock) {
			lock.notify();
		}
		return r;
	}

	@Override
	public boolean offer(IN cart) {
		boolean r = inQueue.offer(cart);
		synchronized (lock) {
			lock.notify();
		}
		return r;
	}

	public void stop() {
		running = false;
	}

	public void finalize() throws Throwable {
		running = false;
		synchronized (lock) {
			lock.notify();
		}
	}

}
