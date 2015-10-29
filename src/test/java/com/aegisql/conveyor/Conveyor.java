package com.aegisql.conveyor;

public interface Conveyor<K, IN extends Cart<K,?>,OUT> {

	public boolean add(IN cart);
	public boolean offer(IN cart);

	public OUT element();
	public OUT peek();
	public OUT poll();
	public OUT remove();

}
