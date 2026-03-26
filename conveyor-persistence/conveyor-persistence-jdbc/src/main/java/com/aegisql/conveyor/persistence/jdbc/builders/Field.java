package com.aegisql.conveyor.persistence.jdbc.builders;

import com.aegisql.conveyor.cart.Cart;

import java.util.function.Function;

@Deprecated(forRemoval = false)
public class Field<K> extends com.aegisql.conveyor.persistence.core.Field<K> {

	public Field(Class<K> fieldClass, String name, Function<Cart<?,?,?>, K> accessor) {
		super(fieldClass, name, accessor);
	}

	public Field(Class<K> fieldClass, String name) {
		super(fieldClass, name);
	}
}
