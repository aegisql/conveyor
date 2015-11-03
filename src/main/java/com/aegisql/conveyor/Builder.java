package com.aegisql.conveyor;

public interface Builder<T> {
	public T build();
	public boolean ready(Lot lot);
}
