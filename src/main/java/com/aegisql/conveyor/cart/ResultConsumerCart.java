package com.aegisql.conveyor.cart;

import java.util.Objects;
import java.util.function.Consumer;

import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.consumers.result.ResultConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Class CreatingCart.
 *
 * @param <K> the key type
 * @param <B> the generic type
 * @param <L> the generic type
 */
public class ResultConsumerCart<K, B, L> extends AbstractCart<K, ResultConsumer <K,B>, L> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4985202264573416558L;

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param creation the creation time
	 * @param expiration the expiration time
	 */
	public ResultConsumerCart(K k, ResultConsumer <K,B> v, long creation, long expiration) {
		super(k, v, null, creation, expiration);
		Objects.requireNonNull(k);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K,ResultConsumer <K,B>, L> copy() {
		return new ResultConsumerCart<K,B,L>(getKey(),getValue(),getCreationTime(), getExpirationTime());
	}

}
