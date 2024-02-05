package com.aegisql.conveyor.cart;

import com.aegisql.conveyor.consumers.result.ResultConsumer;

import java.io.Serial;
import java.util.Objects;

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
	@Serial
    private static final long serialVersionUID = 4985202264573416558L;

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param creation the creation time
	 * @param expiration the expiration time
	 * @param priority the priority
	 */
	public ResultConsumerCart(K k, ResultConsumer <K,B> v, long creation, long expiration,long priority) {
		super(k, v, null, creation,expiration,null,LoadType.RESULT_CONSUMER,priority);
		Objects.requireNonNull(k);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K,ResultConsumer <K,B>, L> copy() {
		ResultConsumerCart<K,B,L> cart = new ResultConsumerCart<>(getKey(), getValue(), getCreationTime(), getExpirationTime(), priority);
		cart.putAllProperties(this.getAllProperties());
		return cart;
	}

}
