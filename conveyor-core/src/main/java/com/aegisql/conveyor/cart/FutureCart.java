package com.aegisql.conveyor.cart;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Class FutureCart.
 *
 * @param <K> the key type
 * @param <B> the generic type
 * @param <L> the generic type
 */
public class FutureCart<K, B, L> extends AbstractCart<K, CompletableFuture<B>, L> implements Supplier<CompletableFuture<B>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4985202262573406558L;

	/**
	 * Instantiates a new future cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param expiration the expiration
	 */
	public FutureCart(K k, CompletableFuture<B> v, long expiration) {
		super(k, v, null, System.currentTimeMillis(),expiration,null,LoadType.FUTURE);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new future cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param creation the creation
	 * @param expiration the expiration
	 * @param priority the priority
	 */
	public FutureCart(K k, CompletableFuture<B> v, long creation, long expiration, long priority) {
		super(k, v, null, creation,expiration,null,LoadType.FUTURE,priority);
		Objects.requireNonNull(k);
	}

	public FutureCart(K k, CompletableFuture<B> v, long creation, long expiration,
			Map<String, Object> allProperties, long priority) {
		super(k, v, null, creation,expiration,null,LoadType.FUTURE,priority);
		Objects.requireNonNull(k);
		this.properties.putAll(allProperties);
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public CompletableFuture<B> get() {
		return getValue();
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K, CompletableFuture<B>, L> copy() {
		FutureCart<K, B, L> cart = new FutureCart<K, B, L>(getKey(), getValue(), getExpirationTime());
		cart.putAllProperties(this.getAllProperties());
		return cart;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.AbstractCart#getScrapConsumer()
	 */
	@Override
	public ScrapConsumer<K, Cart<K, CompletableFuture<B>, L>> getScrapConsumer() {
		return super.getScrapConsumer().andThen(bin->{
			CompletableFuture<B> f = bin.scrap.getValue();
			if(bin.error !=null) {
				f.completeExceptionally(bin.error);
			} else {
				f.cancel(true);
			}
		});
	}
	
	
}
