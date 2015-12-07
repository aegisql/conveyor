package com.aegisql.conveyor.utils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.utils.collection.CollectionConveyor;

public class ChainResult<K,IN2 extends Cart<K,OUT1,?>, OUT1> implements Consumer<ProductBin<K,OUT1>> {

	private final Conveyor<K, ?, Cart<K,OUT1,?>, ?> next;
	private final IN2 prototype;
	
	private long ttl = 0;
	TimeUnit unit = null;
	
	public ChainResult(Conveyor<K, ?, Cart<K,OUT1,?>, ?> next, IN2 prototype ) {
		this.next      = Objects.requireNonNull(next);
		this.prototype = Objects.requireNonNull(prototype);
	}

	public ChainResult(Conveyor<K, ?, Cart<K,OUT1,?>, ?> next, IN2 prototype, long ttl, TimeUnit unit ) {
		this.next      = Objects.requireNonNull(next);
		this.prototype = Objects.requireNonNull(prototype);
		this.ttl       = ttl;
		this.unit      = unit;
	}

	@Override
	public void accept(ProductBin<K,OUT1> bin) {
		ShoppingCart<K, OUT1, ?> cart = null;
		if(unit == null) {
			cart = ((ShoppingCart) prototype).nextCart(bin.product);
		} else {
			cart = new ShoppingCart(prototype.getKey(), bin.product, prototype.getLabel(), ttl, unit);
		}
		next.add(cart);
	}

}
