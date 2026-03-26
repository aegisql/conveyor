package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.cart.Cart;

import java.util.Objects;
import java.util.function.Function;

public class AdditionalField<T> {

    private final Class<T> fieldClass;
    private final String name;
    private final Function<Cart<?, ?, ?>, T> accessor;

    public AdditionalField(Class<T> fieldClass, String name, Function<Cart<?, ?, ?>, T> accessor) {
        this.fieldClass = Objects.requireNonNull(fieldClass, "fieldClass must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.accessor = Objects.requireNonNull(accessor, "accessor must not be null");
    }

    public AdditionalField(Class<T> fieldClass, String name) {
        this(fieldClass, name, cart -> cart.getProperty(name, fieldClass));
    }

    public Class<T> getFieldClass() {
        return fieldClass;
    }

    public String getName() {
        return name;
    }

    public Function<Cart<?, ?, ?>, T> getAccessor() {
        return accessor;
    }
}
