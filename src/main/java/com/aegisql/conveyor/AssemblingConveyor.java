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

	private final Map<K, Builder<OUT>> collector = new HashMap<>();

	private final Consumer<Object> stallConsumer;
	
	private boolean running = true;

	private static final class Lock {
	}

	private final Lock lock = new Lock();

	public AssemblingConveyor(
			Supplier<Builder<OUT>> builderSupplier,
			BiConsumer<Cart<K, ?, L>, Builder<OUT>> cartConsumer, 
			Consumer<OUT> resultConsumer,
			Consumer<Object> stallConsumer
			) 
		{
		this.stallConsumer = stallConsumer;
		
		Thread t = new Thread(() -> {
			while (running) {
				try {
					synchronized (lock) {
						if (inQueue.isEmpty()) {
							lock.wait();
						}
					}
					if( ! running ) break;
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
				Builder<OUT> b = null;
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
					LOG.error("Cart processor failed",e);
					stallConsumer.accept(cart);
					if(b != null) {
						stallConsumer.accept(b);
					}
					if(key != null) {
						collector.remove(key);
					}
				}
			}
			LOG.debug("Leaving thread {}" + Thread.currentThread().getName());
		});
		t.setDaemon(false);
		t.setName("AssemblingConveyor collecting " + builderSupplier.get().getClass().getSimpleName());
		t.start();
	}

	@Override
	public boolean add(IN cart) {
		if( ! running || cart.expired() ) {
			stallConsumer.accept(cart);
			return false;
		}
		boolean r = inQueue.add(cart);
		synchronized (lock) {
			lock.notify();
		}
		return r;
	}

	@Override
	public boolean offer(IN cart) {
		if( ! running || cart.expired() ) {
			stallConsumer.accept(cart);
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
	
	public void stop() {
		running = false;
		synchronized (lock) {
			lock.notify();
		}
	}

}
