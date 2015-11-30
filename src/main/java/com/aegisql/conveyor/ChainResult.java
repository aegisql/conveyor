package com.aegisql.conveyor;

import java.util.function.Consumer;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;

public class ChainResult<K, L, IN extends Cart<K,?,L>,OUT> implements Consumer<OUT> {

	private final Conveyor<K, L, Cart<K,?,L>, ?> next;
	private final ShoppingCart<K,?,L> prototype;
	
	public ChainResult(Conveyor<K, L, Cart<K,?,L>, ?> next, ShoppingCart<K,?,L> prototype ) {
		this.next      = next;
		this.prototype = prototype;
	}
	
	@Override
	public void accept(OUT t) {
		Cart<K, OUT, L> cart = prototype.nextCart(t);
		next.add(cart);
	}

}
