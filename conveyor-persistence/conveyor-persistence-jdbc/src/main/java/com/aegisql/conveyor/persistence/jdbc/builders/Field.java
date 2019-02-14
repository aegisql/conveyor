package com.aegisql.conveyor.persistence.jdbc.builders;

import java.util.function.Function;

import com.aegisql.conveyor.cart.Cart;

public class Field<K> {

	private final Class<K> fieldClass;
	private final String name;
	private final Function<Cart<?,?,?>,K> accessor;

	public Field(Class<K> fieldClass, String name, Function<Cart<?,?,?>, K> accessor) {
		this.fieldClass = fieldClass;
		this.name = name;
		this.accessor = accessor;
	}

	public Field(Class<K> fieldClass, String name) {
		this(fieldClass,name,cart->cart.getProperty(name, fieldClass));
	}

	public Class<K> getFieldClass() {
		return fieldClass;
	}

	public String getName() {
		return name;
	}

	public Function<Cart<?, ?, ?>, K> getAccessor() {
		return accessor;
	}

}
