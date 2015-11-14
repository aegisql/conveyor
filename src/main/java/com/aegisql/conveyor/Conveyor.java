package com.aegisql.conveyor;

public interface Conveyor<K, L, IN extends Cart<K,?,L>,OUT> {

	public boolean add(IN cart);
	public boolean offer(IN cart);
	public boolean addCommand(Cart<K,?,Command> cart);

}
