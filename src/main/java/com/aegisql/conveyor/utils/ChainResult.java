package com.aegisql.conveyor.utils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;

public class ChainResult<K,OUT1> implements Consumer<ProductBin<K,OUT1>> {

	private final Conveyor next;
	
	private Function<ProductBin<K,OUT1>,Cart<K,OUT1,?>> cartBuilder;

	public ChainResult(Conveyor<K, ?, ?> next, Function<ProductBin<K,OUT1>,Cart<K,OUT1,?>> cartBuilder) {
		this.next        = Objects.requireNonNull(next);
		this.cartBuilder = Objects.requireNonNull(cartBuilder);
	}

	public ChainResult(Conveyor<K, ?, ?> next, Cart<K,OUT1,?> prototype ) {
		this(next, bin -> new ShoppingCart(prototype.getKey(), bin.product, prototype.getLabel(), bin.remainingDelayMsec,TimeUnit.MILLISECONDS));
	}

	public ChainResult(Conveyor<K, ?, ?> next, Cart<K,OUT1,?> prototype, long ttl, TimeUnit unit ) {
		this(next,bin -> new ShoppingCart(prototype.getKey(), bin.product, prototype.getLabel(), ttl, unit));
	}

	@Override
	public void accept(ProductBin<K,OUT1> bin) {
		next.add( cartBuilder.apply(bin) );
	}

}
