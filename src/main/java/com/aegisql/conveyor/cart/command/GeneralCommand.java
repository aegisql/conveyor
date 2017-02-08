package com.aegisql.conveyor.cart.command;

import java.util.Objects;
import java.util.function.Predicate;

import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.AbstractCart;
import com.aegisql.conveyor.cart.Cart;

// TODO: Auto-generated Javadoc
/**
 * The Class GeneralCommand.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class GeneralCommand<K, V> extends AbstractCart<K, V, CommandLabel> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -5709296056171099659L;

	protected final Predicate<K> filter;
	
	/**
	 * Instantiates a new general command.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param expiration the expiration
	 */
	public GeneralCommand(K k, V v, CommandLabel label, long expiration) {
		super(k, v, label, expiration);
		Objects.requireNonNull(k);
		filter = key -> k.equals(k);
	}

	public GeneralCommand(Predicate<K> f, V v, CommandLabel label, long expiration) {
		super(null, v, label, expiration);
		Objects.requireNonNull(f);
		filter = f;
	}

	public GeneralCommand(K k, V v, CommandLabel label, long creation, long expiration) {
		super(k, v, label, creation, expiration);
		Objects.requireNonNull(k);
		filter = key -> k.equals(k);
	}

	public GeneralCommand(Predicate<K> f, V v, CommandLabel label, long creation, long expiration) {
		super(null, v, label, creation, expiration);
		Objects.requireNonNull(f);
		filter = f;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+" [key=" + k + 
				", value=" + v + 
				", label=" + label + 
				", expirationTime=" + expirationTime +
				 "]";
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K, V, CommandLabel> copy() {
		return new GeneralCommand<K, V>(getKey(), getValue(), getLabel(),getExpirationTime());
	}

	public Predicate<K> getFilter() {
		return filter;
	}

}
