package com.aegisql.conveyor.validation;

import com.aegisql.conveyor.cart.Cart;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public interface CommonValidators {

    static <K,L> Consumer<Cart<K,?,L>> CART_NOT_NULL() { return cart-> Objects.requireNonNull(cart,"Cart is null");}
    static <K,L> Consumer<Cart<K,?,L>> CART_VALUE_NOT_NULL() { return cart-> Objects.requireNonNull(cart.getValue(),"Cart value is null");}
    static <K,L> Consumer<Cart<K,?,L>> CART_EXPIRED() { return cart -> {
        if (cart.expired()) {
            throw new IllegalStateException("Cart has already expired " + cart);
        }
    };}
    static <K,L> Consumer<Cart<K,?,L>> CART_TOO_OLD(LongSupplier rejectTimeSupplier) { return cart -> {
        if (cart.getCreationTime() < (System.currentTimeMillis() - rejectTimeSupplier.getAsLong())) {
            throw new IllegalStateException("Cart is too old " + cart);
        }
    };}
    static <K,L> Consumer<Cart<K,?,L>> NOT_RUNNING(BooleanSupplier running, Supplier<String> name) { return cart -> {
        if (!running.getAsBoolean()) {
            throw new IllegalStateException("Conveyor "+name.get()+" is not running");
        }
    };}

}
