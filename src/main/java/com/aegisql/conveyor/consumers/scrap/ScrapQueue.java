package com.aegisql.conveyor.consumers.scrap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

// TODO: Auto-generated Javadoc
/**
 * The Class ResultQueue.
 *
 * @param <K> the key type
 */
public class ScrapQueue<K> implements Queue<Object>, Consumer<ScrapBin<K,?>> {

	/** The inner. */
	private final Queue<Object> inner;	
	
	/**
	 * Instantiates a new result queue.
	 */
	public ScrapQueue() {
		this(ConcurrentLinkedDeque::new);
	}

	/**
	 * Instantiates a new result queue.
	 *
	 * @param supplier the supplier
	 */
	public ScrapQueue( Supplier<Queue<Object>> supplier ) {
		inner = supplier.get();
	}

	/**
	 * Instantiates a new result queue.
	 *
	 * @param queue the queue
	 */
	public ScrapQueue( Queue<Object> queue ) {
		inner = queue;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#size()
	 */
	@Override
	public int size() {
		return inner.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return inner.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#contains(java.lang.Object)
	 */
	@Override
	public boolean contains(Object o) {
		return inner.contains(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#iterator()
	 */
	@Override
	public Iterator<Object> iterator() {
		return inner.iterator();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray()
	 */
	@Override
	public Object[] toArray() {
		return inner.toArray();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray(java.lang.Object[])
	 */
	@Override
	public <T> T[] toArray(T[] a) {
		return inner.toArray(a);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object o) {
		return inner.remove(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#containsAll(java.util.Collection)
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		return inner.containsAll(c);
	}

	/**
	 * Adds the all.
	 *
	 * @param c the c
	 * @return true, if successful
	 */
	/* (non-Javadoc)
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<?> c) {
		throw new RuntimeException("Method addAll is not available from this context");
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		return inner.removeAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		return inner.retainAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#clear()
	 */
	@Override
	public void clear() {
		inner.clear();
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#add(java.lang.Object)
	 */
	@Override
	public boolean add(Object e) {
		throw new RuntimeException("Method add is not available from this context");
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#offer(java.lang.Object)
	 */
	@Override
	public boolean offer(Object e) {
		throw new RuntimeException("Method offer is not available from this context");
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#remove()
	 */
	@Override
	public Object remove() {
		return inner.remove();
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#poll()
	 */
	@Override
	public Object poll() {
		return inner.poll();
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#element()
	 */
	@Override
	public Object element() {
		return inner.element();
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#peek()
	 */
	@Override
	public Object peek() {
		return inner.peek();
	}

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ScrapBin<K, ?> scrapBin) {
		inner.add(scrapBin.scrap);
	}

	/**
	 * getInnerQueue.
	 *
	 * @return the inner queue
	 */
	public Queue<Object> getInnerQueue() {
		return inner;
	}
	
	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @return the scrap queue
	 */
	public static <K> ScrapQueue<K> of(Conveyor<K, ?, ?> conv) {
		return new ScrapQueue<>();
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param q the q
	 * @return the scrap queue
	 */
	public static <K> ScrapQueue<K> of(Conveyor<K, ?, ?> conv, Queue<Object> q) {
		return new ScrapQueue<>(q);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param qs the qs
	 * @return the scrap queue
	 */
	public static <K> ScrapQueue<K> of(Conveyor<K, ?, ?> conv, Supplier<Queue<Object>> qs) {
		return new ScrapQueue<>(qs);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ScrapQueue [" + inner + "]";
	}
	
}
