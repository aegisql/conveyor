package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class AssemblingConveyor<K, L, IN extends Cart<K, ?, L>, OUT> implements Conveyor<K, L, IN, OUT> {

	private final Queue<IN> inQueue = new ConcurrentLinkedQueue<>();
	private final Queue<OUT> outQueue = new ConcurrentLinkedQueue<>();

	private Map<K, Builder<OUT>> collector = new HashMap<>();

	private boolean running = true;

	private static final class Lock {
	}

	private final Object lock = new Lock();

	public AssemblingConveyor(Supplier<Builder<OUT>> builderSupplier,
			BiConsumer<Cart<K, ?, L>, Builder<OUT>> cartConsumer) {
		Thread t = new Thread(() -> {
			while (running) {
				if (inQueue.isEmpty()) {
					try {
						synchronized (lock) {
							lock.wait();
						}
					} catch (InterruptedException e) {
						// e.printStackTrace();
						running = false;
					}
				}

				Cart<K, ?, L> cart = inQueue.poll();
				if (cart == null) {
					continue;
				}
				System.out.println("Cart read " + cart);
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
						outQueue.add(b.build());
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			System.out.println("Leaving thread ");
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

	@Override
	public OUT element() {
		return outQueue.element();
	}

	@Override
	public OUT peek() {
		return outQueue.peek();
	}

	@Override
	public OUT poll() {
		return outQueue.poll();
	}

	@Override
	public OUT remove() {
		return outQueue.remove();
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
