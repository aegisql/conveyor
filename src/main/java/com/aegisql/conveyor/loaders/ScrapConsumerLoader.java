package com.aegisql.conveyor.loaders;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.FutureCart;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Class ScrapConsumerLoader.
 *
 * @param <K> the key type
 */
public final class ScrapConsumerLoader<K> {

	
	/** The cart future consumer. */
	protected final ScrapConsumer<K, Cart<K,?,?>> CART_FUTURE_CONSUMER = scrapBin -> {
		if(scrapBin.scrap == null) {
			return;
		}
		CompletableFuture<Boolean> future = scrapBin.scrap.getFuture();
		if(scrapBin.error != null) {
			future.completeExceptionally(scrapBin.error);
		} else {
			future.cancel(true);
		}
	};

	/** The future consumer. */
	protected final ScrapConsumer<K, FutureCart<K,?,?>> FUTURE_CONSUMER = scrapBin -> {
		if(scrapBin.scrap == null) {
			return;
		}
		CompletableFuture<?> productFuture  = scrapBin.scrap.getValue();
		if(scrapBin.error != null) {
			productFuture.completeExceptionally(scrapBin.error);
		} else {
			productFuture.cancel(true);
		}
	};
	
	/** The cart consumer. */
	protected final ScrapConsumer<K,?> CART_CONSUMER = scrapBin -> {
		if(scrapBin.scrap instanceof Cart) {
			Cart<K,?,?> c = (Cart<K,?,?>)scrapBin.scrap;
			CART_FUTURE_CONSUMER.accept((ScrapBin)scrapBin);
			if( c instanceof FutureCart ) {
				FUTURE_CONSUMER.accept((ScrapBin)scrapBin);
			}
		}
	};
	
	//public final K key;
	
	/** The consumer. */
	public final ScrapConsumer<K,?> consumer;
	
	//public final Function<ScrapConsumerLoader<K>,CompletableFuture<Boolean>> placer;
	
	/** The creation time. */
	//public final long creationTime; 
	
	/** The expiration time. */
	//public final long expirationTime;
	
	/** The ttl msec. */
	//public final long ttlMsec;
	
	private final Consumer<ScrapConsumer<K,?>> globalPlacer;
	
	//public final Predicate<K> filter;
	
//	public ScrapConsumerLoader(
//			Function<ScrapConsumerLoader<K>,CompletableFuture<Boolean>> placer,
//			Consumer<Consumer<ScrapBin<K,?>>> globalPlacer,
//			Consumer<ScrapBin<K,?>> consumer
//			) {
//		this(
//				//placer,
//				globalPlacer,
//				//System.currentTimeMillis(),0,0,null,
//				consumer
//				//,null
//				);
//	}

	/**
	 * Instantiates a new scrap consumer loader.
	 *
	 * @param globalPlacer the global placer
	 * @param consumer the consumer
	 */
	public ScrapConsumerLoader(
			//Function<ScrapConsumerLoader<K>,CompletableFuture<Boolean>> placer, 
			Consumer<ScrapConsumer<K,?>> globalPlacer,
			//long creationTime,
			//long expirationTime,
			//long ttlMsec,
			//K key, 
			ScrapConsumer<K,?> consumer
			//Predicate<K> filter 
			) {
		//this.placer         = placer;
		this.globalPlacer   = globalPlacer;
		this.consumer       = consumer != null ? consumer : CART_CONSUMER;
		//this.key            = key;
		//this.creationTime   = creationTime;
		//this.expirationTime = expirationTime;
		//this.ttlMsec        = ttlMsec;
		//this.filter         = filter;
	}

//	private ScrapConsumerLoader(
//			Function<ScrapConsumerLoader<K>,CompletableFuture<Boolean>> placer, 
//			Consumer<Consumer<ScrapBin<K,?>>> globalPlacer,
//			long creationTime,
//			long ttlMsec,
//			K key, 
//			Consumer<ScrapBin<K,?> > consumer,
//			Predicate<K> filter,
//			boolean dumb) {
//		this.placer         = placer;
//		this.globalPlacer   = globalPlacer;
//		this.consumer       = consumer != null ? consumer : CART_CONSUMER;
//		this.key            = key;
//		this.creationTime   = creationTime;
//		this.expirationTime = creationTime + ttlMsec;
//		this.ttlMsec        = ttlMsec;
//		this.filter         = filter;
//	}

//	public ScrapConsumerLoader<K> id(K k) {
//		return new ScrapConsumerLoader<>(
//				this.placer,
//				this.globalPlacer,
//				this.creationTime,
//				this.expirationTime,
//				this.ttlMsec,
//				k,
//				this.consumer,
//				null/*either key or filter*/);
//	}
//
//	public ScrapConsumerLoader<K> foreach(Predicate<K> f) {
//		return new ScrapConsumerLoader<>(
//				this.placer,
//				this.globalPlacer,
//				this.creationTime,
//				this.expirationTime,
//				this.ttlMsec,
//				null/*either key or filter*/,
//				this.consumer,
//				f);
//	}
//
//	public ScrapConsumerLoader<K> foreach() {
//		return foreach(k->true);
//	}

	/**
 * First.
 *
 * @param consumer the consumer
 * @return the scrap consumer loader
 */
public ScrapConsumerLoader<K> first(ScrapConsumer<K,?> consumer) {
		return new ScrapConsumerLoader<>(
				//this.placer,
				this.globalPlacer,
				//this.creationTime,
				//this.expirationTime,
				//this.ttlMsec,
				//this.key,
				CART_CONSUMER.andThen( (ScrapConsumer) consumer ) //first take care about futures
				//filter
				);
	}

//	/**
//	 * Expiration time.
//	 *
//	 * @param et the et
//	 * @return the part loader
//	 */
//	public ScrapConsumerLoader<K>  expirationTime(long et) {
//		return new ScrapConsumerLoader<>(placer,globalPlacer,creationTime,et,0,key,consumer,filter);
//	}
//	
//	/**
//	 * Expiration time.
//	 *
//	 * @param instant the instant
//	 * @return the part loader
//	 */
//	public ScrapConsumerLoader<K>  expirationTime(Instant instant) {
//		return new ScrapConsumerLoader<>(placer,globalPlacer,creationTime,instant.toEpochMilli(),0,key,consumer,filter);
//	}
//	
//	/**
//	 * Ttl.
//	 *
//	 * @param time the time
//	 * @param unit the unit
//	 * @return the part loader
//	 */
//	public ScrapConsumerLoader<K>  ttl(long time, TimeUnit unit) {
//		return new ScrapConsumerLoader<>(placer,globalPlacer, creationTime,TimeUnit.MILLISECONDS.convert(time, unit),key,consumer,filter,true);
//	}
//	
//	/**
//	 * Ttl.
//	 *
//	 * @param duration the duration
//	 * @return the part loader
//	 */
//	public ScrapConsumerLoader<K>  ttl(Duration duration) {
//		return new ScrapConsumerLoader<>(placer,globalPlacer,creationTime,duration.toMillis(),key,consumer,filter,true);
//	}

	
	/**
 * And then.
 *
 * @param consumer the consumer
 * @return the scrap consumer loader
 */
public ScrapConsumerLoader<K> andThen(ScrapConsumer<K,?> consumer) {
		return new ScrapConsumerLoader<>(
				//this.placer,
				this.globalPlacer,
				//this.creationTime,
				//this.expirationTime,
				//this.ttlMsec,
				//this.key,
				this.consumer != null ? this.consumer.andThen((ScrapConsumer) consumer) : CART_CONSUMER.andThen( (ScrapConsumer) consumer )
				//,this.filter
				);
	}

	/**
	 * Sets the.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> set() {
//		if(key == null && filter == null) {
//			CompletableFuture<Boolean> ready = new CompletableFuture<>();
//			ready.complete(true);
//			globalPlacer.accept(consumer);
//			return ready;
//		} else {
//			return placer.apply(this);
//		}
		CompletableFuture<Boolean> ready = new CompletableFuture<>();
		globalPlacer.accept(consumer);
		ready.complete(true);
		return ready;
	}

	
	
	
}
