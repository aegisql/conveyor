package com.aegisql.conveyor.persistence.jdbc.builders;

import java.util.function.Function;

import com.aegisql.conveyor.cart.Cart;

// TODO: Auto-generated Javadoc
/**
 * The Class Field.
 *
 * @param <K> the key type
 */
public class Field<K> {

	/** The field class. */
	private final Class<K> fieldClass;
	
	/** The name. */
	private final String name;
	
	/** The accessor. */
	private final Function<Cart<?,?,?>,K> accessor;

	/**
	 * Instantiates a new field.
	 *
	 * @param fieldClass the field class
	 * @param name the name
	 * @param accessor the accessor
	 */
	public Field(Class<K> fieldClass, String name, Function<Cart<?,?,?>, K> accessor) {
		this.fieldClass = fieldClass;
		this.name = name;
		this.accessor = accessor;
	}

	/**
	 * Instantiates a new field.
	 *
	 * @param fieldClass the field class
	 * @param name the name
	 */
	public Field(Class<K> fieldClass, String name) {
		this(fieldClass,name,cart->cart.getProperty(name, fieldClass));
	}

	/**
	 * Gets the field class.
	 *
	 * @return the field class
	 */
	public Class<K> getFieldClass() {
		return fieldClass;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the accessor.
	 *
	 * @return the accessor
	 */
	public Function<Cart<?, ?, ?>, K> getAccessor() {
		return accessor;
	}

}
