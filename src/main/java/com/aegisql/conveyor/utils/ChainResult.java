package com.aegisql.conveyor.utils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;

public class ChainResult<K2, L2, IN2 extends Cart<K2,OUT1,L2>,OUT1> implements Consumer<OUT1> {

	private final Conveyor<K2, L2, Cart<K2,OUT1,L2>, ?> next;
	private final ShoppingCart<K2,OUT1,L2> prototype;
	
	private long ttl = 0;
	TimeUnit unit = null;
	
	public ChainResult(Conveyor<K2, L2, Cart<K2,OUT1,L2>, ?> next, ShoppingCart<K2,OUT1,L2> prototype ) {
		this.next      = Objects.requireNonNull(next);
		this.prototype = Objects.requireNonNull(prototype);
	}

	public ChainResult(Conveyor<K2, L2, Cart<K2,OUT1,L2>, ?> next, ShoppingCart<K2,OUT1,L2> prototype, long ttl, TimeUnit unit ) {
		this.next      = Objects.requireNonNull(next);
		this.prototype = Objects.requireNonNull(prototype);
		this.ttl       = ttl;
		this.unit      = unit;
	}

	@Override
	public void accept(OUT1 t) {
		Cart<K2, OUT1, L2> cart = null;
		if(unit == null) {
			cart = prototype.nextCart(t);
		} else {
			cart = new ShoppingCart<K2, OUT1, L2>(prototype.getKey(), t, prototype.getLabel(), ttl, unit);
		}
		next.add(cart);
	}

}
