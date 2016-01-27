package com.aegisql.conveyor.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.aegisql.conveyor.ProductBin;

public class ResultQueue<E> implements Queue<E>, Consumer<ProductBin<?,E>> {

	private final Queue<E> inner;	
	
	public ResultQueue() {
		inner = new ConcurrentLinkedDeque<>();
	}

	public ResultQueue( Supplier<Queue<E>> supplier ) {
		inner = supplier.get();
	}
	
	@Override
	public int size() {
		return inner.size();
	}

	@Override
	public boolean isEmpty() {
		return inner.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return inner.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return inner.iterator();
	}

	@Override
	public Object[] toArray() {
		return inner.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return inner.toArray(a);
	}

	@Override
	public boolean remove(Object o) {
		return inner.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return inner.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return inner.addAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return inner.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return inner.retainAll(c);
	}

	@Override
	public void clear() {
		inner.clear();
	}

	@Override
	public boolean add(E e) {
		return inner.add(e);
	}

	@Override
	public boolean offer(E e) {
		return inner.offer(e);
	}

	@Override
	public E remove() {
		return inner.remove();
	}

	@Override
	public E poll() {
		return inner.poll();
	}

	@Override
	public E element() {
		return inner.element();
	}

	@Override
	public E peek() {
		return inner.peek();
	}

	@Override
	public void accept(ProductBin<?, E> productBin) {
		this.add(productBin.product);
	}

}
