package com.aegisql.conveyor.loaders;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.FutureCart;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Class ScrapConsumerLoader.
 *
 * @param <K>
 *            the key type
 */
public final class ScrapConsumerLoader<K> {

	/** The cart future consumer. */
	protected final ScrapConsumer<K, Cart<K, ?, ?>> CART_FUTURE_CONSUMER = scrapBin -> {
		if (scrapBin.scrap == null) {
			return;
		}
		CompletableFuture<Boolean> future = scrapBin.scrap.getFuture();
		if (scrapBin.error != null) {
			future.completeExceptionally(scrapBin.error);
		} else {
			future.cancel(true);
		}
	};

	/** The future consumer. */
	protected final ScrapConsumer<K, FutureCart<K, ?, ?>> FUTURE_CONSUMER = scrapBin -> {
		if (scrapBin.scrap == null) {
			return;
		}
		CompletableFuture<?> productFuture = scrapBin.scrap.getValue();
		if (scrapBin.error != null) {
			productFuture.completeExceptionally(scrapBin.error);
		} else {
			productFuture.cancel(true);
		}
	};

	/** The cart consumer. */
	protected final ScrapConsumer<K, ?> CART_CONSUMER = scrapBin -> {
		if (scrapBin.scrap instanceof Cart) {
			Cart<K, ?, ?> c = (Cart<K, ?, ?>) scrapBin.scrap;
			CART_FUTURE_CONSUMER.accept((ScrapBin) scrapBin);
			if (c instanceof FutureCart) {
				FUTURE_CONSUMER.accept((ScrapBin) scrapBin);
			}
		}
	};

	/** The consumer. */
	public final ScrapConsumer<K, ?> consumer;

	/** The global placer. */
	private final Consumer<ScrapConsumer<K, ?>> globalPlacer;
	
	/**
	 * Instantiates a new scrap consumer loader.
	 *
	 * @param globalPlacer            the global placer
	 * @param consumer            the consumer
	 */
	public ScrapConsumerLoader(Consumer<ScrapConsumer<K, ?>> globalPlacer, ScrapConsumer<K, ?> consumer) {
		this.globalPlacer = globalPlacer;
		this.consumer = consumer != null ? consumer : CART_CONSUMER;
	}

	/**
	 * First.
	 *
	 * @param consumer
	 *            the consumer
	 * @return the scrap consumer loader
	 */
	public ScrapConsumerLoader<K> first(ScrapConsumer<K, ?> consumer) {
		return new ScrapConsumerLoader<>(
				this.globalPlacer,
				CART_CONSUMER.andThen((ScrapConsumer) consumer) // first take care about futures
		);
	}

	/**
	 * And then.
	 *
	 * @param consumer
	 *            the consumer
	 * @return the scrap consumer loader
	 */
	public ScrapConsumerLoader<K> andThen(ScrapConsumer<K, ?> consumer) {
		return new ScrapConsumerLoader<>(
				this.globalPlacer,
				this.consumer != null ? this.consumer.andThen((ScrapConsumer) consumer)
						: CART_CONSUMER.andThen((ScrapConsumer) consumer)
		);
	}

	/**
	 * Sets the.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> set() {
		CompletableFuture<Boolean> ready = new CompletableFuture<>();
		globalPlacer.accept(consumer);
		ready.complete(true);
		return ready;
	}

	public static <K> ScrapConsumerLoader<K> byConveyorName(String name) {
		return Conveyor.byName(name).scrapConsumer();
	}

	public static <K> ScrapConsumerLoader<K> byConveyorName(String name,ScrapConsumer<K,?> scrapConsumer) {
		return Conveyor.byName(name).scrapConsumer(scrapConsumer);
	}

}
