package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.cart.Cart;

import java.util.function.Function;

@Deprecated(forRemoval = false)
public class AdditionalField<T> extends com.aegisql.conveyor.persistence.core.Field<T> {

    public AdditionalField(Class<T> fieldClass, String name, Function<Cart<?, ?, ?>, T> accessor) {
        super(fieldClass, name, accessor);
    }

    public AdditionalField(Class<T> fieldClass, String name) {
        super(fieldClass, name);
    }
}
