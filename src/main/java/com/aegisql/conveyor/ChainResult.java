package com.aegisql.conveyor;

import java.util.function.Consumer;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;

public class ChainResult<K2, L2, IN2 extends Cart<K2,OUT1,L2>,OUT1> implements Consumer<OUT1> {

	private final Conveyor<K2, L2, Cart<K2,OUT1,L2>, ?> next;
	private final ShoppingCart<K2,OUT1,L2> prototype;
	
	public ChainResult(Conveyor<K2, L2, Cart<K2,OUT1,L2>, ?> next, ShoppingCart<K2,OUT1,L2> prototype ) {
		this.next      = next;
		this.prototype = prototype;
	}
	
	@Override
	public void accept(OUT1 t) {
		Cart<K2, OUT1, L2> cart = prototype.nextCart(t);
		next.add(cart);
	}

}
