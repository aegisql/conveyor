package com.aegisql.conveyor.cart;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.BuilderSupplier.BuilderFutureSupplier;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;

import java.io.Serial;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class CreatingCart.
 *
 * @param <K> the key type
 * @param <B> the generic type
 * @param <L> the generic type
 */
public class CreatingCart<K, B, L> extends AbstractCart<K, BuilderSupplier<B>, L> implements Supplier<BuilderSupplier<B>> {

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
	public CreatingCart(K k, BuilderSupplier<B> v, long creation, long expiration,long priority) {
		super(k, v, null, creation,expiration,null,LoadType.BUILDER,priority);
		Objects.requireNonNull(k);
	}

	public CreatingCart(K key, BuilderSupplier<B> v, long creation, long expiration,
			Map<String, Object> allProperties, long priority) {
		super(key, v, null, creation, expiration,null,LoadType.BUILDER,priority);
		Objects.requireNonNull(k);
		this.properties.putAll(allProperties);
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public BuilderSupplier<B> get() {
		return getValue();
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K,BuilderSupplier<B>,L> copy() {
		CreatingCart<K,B,L> cart = new CreatingCart<>(getKey(), getValue(), getCreationTime(), getExpirationTime(), getPriority());
		cart.putAllProperties(this.getAllProperties());
		return cart;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.AbstractCart#getScrapConsumer()
	 */
	@Override
	public ScrapConsumer<K, Cart<K, BuilderSupplier<B>, L>> getScrapConsumer() {
		return super.getScrapConsumer().andThen(bin->{
			BuilderSupplier<B> bs = bin.scrap.getValue();
			if(bs instanceof BuilderFutureSupplier<B> bfs) {
				if(bin.error !=null) {
					bfs.getFuture().completeExceptionally(bin.error);
				} else {
					bfs.getFuture().cancel(true);
				}

			}
		});
	}

	
	
}
