package com.aegisql.conveyor.consumers.result;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

// TODO: Auto-generated Javadoc
/**
 * The Class ResultQueue.
 *
 * @param <K> the key type
 * @param <E> the element type
 */
public class ResultQueue<K,E> implements Queue<E>, Consumer<ProductBin<K,E>> {

	/** The inner. */
	private final Queue<E> inner;	
	
	/**
	 * Instantiates a new result queue.
	 */
	public ResultQueue() {
		this(ConcurrentLinkedDeque::new);
	}

	/**
	 * Instantiates a new result queue.
	 *
	 * @param supplier the supplier
	 */
	public ResultQueue( Supplier<Queue<E>> supplier ) {
		inner = supplier.get();
	}

	/**
	 * Instantiates a new result queue.
	 *
	 * @param queue the queue
	 */
	public ResultQueue( Queue<E> queue ) {
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
	public Iterator<E> iterator() {
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

	/* (non-Javadoc)
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends E> c) {
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
	public boolean add(E e) {
		throw new RuntimeException("Method add is not available from this context");
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#offer(java.lang.Object)
	 */
	@Override
	public boolean offer(E e) {
		throw new RuntimeException("Method offer is not available from this context");
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#remove()
	 */
	@Override
	public E remove() {
		return inner.remove();
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#poll()
	 */
	@Override
	public E poll() {
		return inner.poll();
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#element()
	 */
	@Override
	public E element() {
		return inner.element();
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#peek()
	 */
	@Override
	public E peek() {
		return inner.peek();
	}

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K, E> productBin) {
		inner.add(productBin.product);
	}

	/**
	 * Unwrap.
	 *
	 * @param <T> the generic type
	 * @return the t
	 */
	@SuppressWarnings("unchecked")
	public <T extends Queue<E>> T unwrap() {
		return (T) inner;
	}
	
	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conv the conv
	 * @return the result queue
	 */
	public static <K,E> ResultQueue<K,E> of(Conveyor<K, ?, E> conv) {
		return new ResultQueue<>();
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conv the conv
	 * @param q the q
	 * @return the result queue
	 */
	public static <K,E> ResultQueue<K,E> of(Conveyor<K, ?, E> conv, Queue<E> q) {
		return new ResultQueue<>(q);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conv the conv
	 * @param qs the qs
	 * @return the result queue
	 */
	public static <K,E> ResultQueue<K,E> of(Conveyor<K, ?, E> conv, Supplier<Queue<E>> qs) {
		return new ResultQueue<>(qs);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ResultQueue [" + inner + "]";
	}

}
